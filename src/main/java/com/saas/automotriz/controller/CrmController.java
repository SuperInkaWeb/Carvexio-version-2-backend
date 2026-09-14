package com.saas.automotriz.controller;

import com.saas.automotriz.dto.*;
import com.saas.automotriz.model.*;
import com.saas.automotriz.repository.BookingRepository;
import com.saas.automotriz.repository.BusinessRepository;
import com.saas.automotriz.repository.OrderRepository;
import com.saas.automotriz.repository.VehicleRepository;
import com.saas.automotriz.service.PlanLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/crm")
@RequiredArgsConstructor
public class CrmController {

    private final BookingRepository bookingRepository;
    private final BusinessRepository businessRepository;
    private final OrderRepository orderRepository;
    private final PlanLimitService planLimitService;
    private final VehicleRepository vehicleRepository;

    // lista de todos los clientes del negocio (reservas + pedidos)
    @GetMapping("/clients")
    public ResponseEntity<List<ClientSummaryDTO>> getClients(@AuthenticationPrincipal User user,
                                                             @RequestParam(required = false) Long localId) {
        Business business = businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));

        Set<User> clientsSet = new HashSet<>();
        List<User> bookingClients = localId != null
                ? bookingRepository.findByBusinessAndBranchId(business, localId).stream().map(Booking::getClient).toList()
                : bookingRepository.findClientsByBusiness(business);
        if (bookingClients != null) clientsSet.addAll(bookingClients);

        List<User> orderClients = localId != null
                ? orderRepository.findClientsByBusinessAndBranchId(business, localId)
                : orderRepository.findClientsByBusiness(business);
        if (orderClients != null) clientsSet.addAll(orderClients);

        List<ClientSummaryDTO> clients = clientsSet.stream().map(client -> {
            List<Booking> bookings = bookingRepository.findByBusinessAndClient(business, client);
            List<Order> orders = orderRepository.findByBusinessAndClient(business, client);

            ClientSummaryDTO dto = new ClientSummaryDTO();
            dto.setId(client.getId());
            dto.setName(client.getName());
            dto.setEmail(client.getEmail());
            dto.setPhone(client.getPhone());
            dto.setVehicles(vehicleRepository.findByClientOrderByIdDesc(client).stream().map(this::toVehicleDTO).toList());

            long totalReservas = bookings != null ? bookings.size() : 0;
            long totalPedidos = orders != null ? orders.size() : 0;

            double montoServicios = bookings != null ? bookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                    .mapToDouble(b -> b.getService() != null && b.getService().getPrice() != null ? b.getService().getPrice() : 0.0)
                    .sum() : 0.0;

            double montoProductos = orders != null ? orders.stream()
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED && o.getStatus() != OrderStatus.PENDING)
                    .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0)
                    .sum() : 0.0;

            dto.setTotalReservas(totalReservas);
            dto.setTotalPedidos(totalPedidos);
            dto.setMontoTotalServicios(montoServicios);
            dto.setMontoTotalProductos(montoProductos);
            dto.setMontoTotalTotal(montoServicios + montoProductos);

            LocalDateTime latestBookingDate = bookings != null ? bookings.stream()
                    .map(Booking::getCreatedAt)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null) : null;

            LocalDateTime latestOrderDate = orders != null ? orders.stream()
                    .map(Order::getCreatedAt)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null) : null;

            if (latestBookingDate == null) {
                dto.setUltimaVisita(latestOrderDate);
            } else if (latestOrderDate == null) {
                dto.setUltimaVisita(latestBookingDate);
            } else {
                dto.setUltimaVisita(latestBookingDate.isAfter(latestOrderDate) ? latestBookingDate : latestOrderDate);
            }

            return dto;
        }).toList();

        return ResponseEntity.ok(clients);
    }

    // historial completo de un cliente específico (reservas + pedidos)
    @GetMapping("/clients/{clientId}")
    public ResponseEntity<ClientHistoryDTO> getClientHistory(@AuthenticationPrincipal User user,
                                                             @PathVariable Long clientId) {
        Business business = businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));
        planLimitService.checkCrmDetailAccess(business);

        Set<User> clientsSet = new HashSet<>();
        List<User> bookingClients = bookingRepository.findClientsByBusiness(business);
        if (bookingClients != null) clientsSet.addAll(bookingClients);
        List<User> orderClients = orderRepository.findClientsByBusiness(business);
        if (orderClients != null) clientsSet.addAll(orderClients);

        User client = clientsSet.stream()
                .filter(c -> c.getId().equals(clientId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        List<Booking> clientBookings = bookingRepository.findByBusinessAndClient(business, client);
        List<Order> clientOrders = orderRepository.findByBusinessAndClient(business, client);

        ClientHistoryDTO dto = new ClientHistoryDTO();
        dto.setClientId(client.getId());
        dto.setClientName(client.getName());
        dto.setClientPhone(client.getPhone());
        dto.setVehicles(vehicleRepository.findByClientOrderByIdDesc(client).stream().map(this::toVehicleDTO).toList());

        List<BookingDTO> bookingDTOs = clientBookings != null ? clientBookings.stream().map(b -> {
            BookingDTO bdto = new BookingDTO();
            bdto.setId(b.getId());
            bdto.setServiceName(b.getService() != null ? b.getService().getName() : "-");
            bdto.setDate(b.getDate());
            bdto.setTime(b.getTime());
            bdto.setStatus(b.getStatus() != null ? b.getStatus().name() : "-");
            bdto.setNotes(b.getNotes());
            bdto.setCreatedAt(b.getCreatedAt());
            return bdto;
        }).toList() : Collections.emptyList();

        List<OrderDTO> orderDTOs = clientOrders != null ? clientOrders.stream().map(o -> {
            OrderDTO odto = new OrderDTO();
            odto.setId(o.getId());
            odto.setClientId(client.getId());
            odto.setClientName(client.getName());
            odto.setBusinessId(business.getId());
            odto.setBusinessName(business.getName());
            odto.setTotalAmount(o.getTotalAmount());
            odto.setStatus(o.getStatus() != null ? o.getStatus().name() : "-");
            odto.setDeliveryMethod(o.getDeliveryMethod() != null ? o.getDeliveryMethod().name() : "PICKUP");
            odto.setAddress(o.getAddress());
            odto.setPhone(o.getPhone());
            odto.setNotes(o.getNotes());
            odto.setCreatedAt(o.getCreatedAt());
            if (o.getItems() != null) {
                odto.setItems(o.getItems().stream().map(item -> {
                    OrderItemDTO itemDTO = new OrderItemDTO();
                    itemDTO.setId(item.getId());
                    itemDTO.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
                    itemDTO.setProductName(item.getProduct() != null ? item.getProduct().getName() : "Producto");
                    itemDTO.setQuantity(item.getQuantity());
                    itemDTO.setPriceAtPurchase(item.getPriceAtPurchase());
                    itemDTO.setSubtotal(item.getSubtotal());
                    return itemDTO;
                }).toList());
            }
            return odto;
        }).toList() : Collections.emptyList();

        dto.setHistorial(bookingDTOs);
        dto.setHistorialReservas(bookingDTOs);
        dto.setHistorialPedidos(orderDTOs);

        return ResponseEntity.ok(dto);
    }

    private VehicleDTO toVehicleDTO(Vehicle vehicle) {
        VehicleDTO dto = new VehicleDTO();
        dto.setId(vehicle.getId());
        dto.setVehicleType(vehicle.getVehicleType());
        dto.setPlate(vehicle.getPlate());
        dto.setVin(vehicle.getVin());
        dto.setMileage(vehicle.getMileage());
        dto.setYearsOfUse(vehicle.getYearsOfUse());
        return dto;
    }
}

