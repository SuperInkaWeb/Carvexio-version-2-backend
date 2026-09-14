package com.saas.automotriz.controller;

import com.saas.automotriz.dto.DashboardDTO;
import com.saas.automotriz.model.BookingStatus;
import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.OrderStatus;
import com.saas.automotriz.model.User;
import com.saas.automotriz.repository.BookingRepository;
import com.saas.automotriz.repository.BusinessRepository;
import com.saas.automotriz.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final BookingRepository bookingRepository;
    private final BusinessRepository businessRepository;
    private final OrderRepository orderRepository;

    @GetMapping
    public ResponseEntity<DashboardDTO> getDashboard(@AuthenticationPrincipal User user,
                                                     @RequestParam(required = false) Long localId) {
        Business business = businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));

        DashboardDTO dto = new DashboardDTO();

        // 1. Reservas de Servicios
        if (localId != null) {
            dto.setReservasHoy(bookingRepository.countByBusinessAndBranchIdAndDate(business, localId, LocalDate.now()));
            dto.setReservasPendientes(bookingRepository.countByBusinessAndBranchIdAndStatus(business, localId, BookingStatus.PENDING));
            dto.setReservasCompletadas(bookingRepository.countByBusinessAndBranchIdAndStatus(business, localId, BookingStatus.COMPLETED));
            dto.setIngresosServicios(bookingRepository.sumIngresosByBusinessAndBranchId(business, localId));

            List<Object[]> topServices = bookingRepository.findTopServiceByBusinessAndBranchId(business, localId);
            if (!topServices.isEmpty()) {
                dto.setServicioMasSolicitado((String) topServices.get(0)[0]);
            } else {
                dto.setServicioMasSolicitado("Sin datos aún");
            }
        } else {
            dto.setReservasHoy(bookingRepository.countByBusinessAndDate(business, LocalDate.now()));
            dto.setReservasPendientes(bookingRepository.countByBusinessAndStatus(business, BookingStatus.PENDING));
            dto.setReservasCompletadas(bookingRepository.countByBusinessAndStatus(business, BookingStatus.COMPLETED));
            dto.setIngresosServicios(bookingRepository.sumIngresosByBusiness(business));

            List<Object[]> topServices = bookingRepository.findTopServiceByBusiness(business);
            if (!topServices.isEmpty()) {
                dto.setServicioMasSolicitado((String) topServices.get(0)[0]);
            } else {
                dto.setServicioMasSolicitado("Sin datos aún");
            }
        }

        if (dto.getIngresosServicios() == null) {
            dto.setIngresosServicios(0.0);
        }

        // 2. Pedidos de Productos
        Long totalPedidos;
        Long pedidosPendientes;
        Double ingresosProductos;

        if (localId != null) {
            totalPedidos = orderRepository.countByBusinessAndBranchId(business, localId);
            pedidosPendientes = orderRepository.countByBusinessAndStatusAndBranchId(business, OrderStatus.PENDING, localId);
            ingresosProductos = orderRepository.sumIngresosByBusinessAndBranchId(business, localId);
        } else {
            totalPedidos = orderRepository.countByBusiness(business);
            pedidosPendientes = orderRepository.countByBusinessAndStatus(business, OrderStatus.PENDING);
            ingresosProductos = orderRepository.sumIngresosByBusiness(business);
        }

        if (totalPedidos == null) totalPedidos = 0L;
        if (pedidosPendientes == null) pedidosPendientes = 0L;
        if (ingresosProductos == null) ingresosProductos = 0.0;

        dto.setPedidosTotales(totalPedidos);
        dto.setPedidosPendientes(pedidosPendientes);
        dto.setPedidosCompletados(Math.max(0L, totalPedidos - pedidosPendientes));
        dto.setIngresosProductos(ingresosProductos);

        // 3. Ingreso total combinado (Servicios + Productos)
        dto.setIngresosTotal(dto.getIngresosServicios() + dto.getIngresosProductos());

        // 4. Top Producto más vendido
        List<Object[]> topProducts = localId != null 
                ? orderRepository.findTopProductByBusinessAndBranchId(business, localId)
                : orderRepository.findTopProductByBusiness(business);

        if (topProducts != null && !topProducts.isEmpty()) {
            dto.setProductoMasVendido((String) topProducts.get(0)[0]);
        } else {
            dto.setProductoMasVendido("Sin ventas aún");
        }

        // 5. Total Clientes Únicos (Servicios + Productos)
        Set<Long> uniqueClientIds = new HashSet<>();
        List<User> bookingClients = localId != null
                ? bookingRepository.findByBusinessAndBranchId(business, localId).stream().map(b -> b.getClient()).toList()
                : bookingRepository.findClientsByBusiness(business);

        if (bookingClients != null) {
            for (User u : bookingClients) {
                if (u != null && u.getId() != null) {
                    uniqueClientIds.add(u.getId());
                }
            }
        }

        List<User> orderClients = localId != null
                ? orderRepository.findClientsByBusinessAndBranchId(business, localId)
                : orderRepository.findClientsByBusiness(business);

        if (orderClients != null) {
            for (User u : orderClients) {
                if (u != null && u.getId() != null) {
                    uniqueClientIds.add(u.getId());
                }
            }
        }
        dto.setClientesTotal((long) uniqueClientIds.size());

        return ResponseEntity.ok(dto);
    }
}
