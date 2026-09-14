package com.saas.automotriz.controller;

import com.saas.automotriz.dto.AdminDashboardDTO;
import com.saas.automotriz.dto.BusinessDTO;
import com.saas.automotriz.dto.UserDTO;
import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.BusinessStatus;
import com.saas.automotriz.model.Role;
import com.saas.automotriz.model.User;
import com.saas.automotriz.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.time.LocalDate;
import com.saas.automotriz.model.Booking;
import com.saas.automotriz.model.BookingStatus;

import com.saas.automotriz.service.EmailService;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final BookingRepository bookingRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final com.saas.automotriz.service.Auth0ManagementService auth0ManagementService;
    private final EmailService emailService;

    // KPIs globales
    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardDTO> getDashboard() {
        AdminDashboardDTO dto = new AdminDashboardDTO();
        dto.setTotalUsuarios(userRepository.count());
        dto.setTotalClientes(userRepository.countByRole(Role.CLIENTE));
        dto.setTotalEmpresas(userRepository.countByRole(Role.EMPRESA));
        dto.setTotalNegocios(businessRepository.countBy());
        dto.setTotalReservas(bookingRepository.countBy());
        dto.setTotalProductos(productRepository.count());

        // Obtener todas las reservas completadas
        List<Booking> completedBookings = bookingRepository.findByStatus(BookingStatus.COMPLETED);

        // Suma total histórica (monto acumulado)
        double totalRevenue = completedBookings.stream()
                .mapToDouble(b -> b.getService() != null && b.getService().getPrice() != null ? b.getService().getPrice() : 0.0)
                .sum();
        dto.setIngresosTotales(totalRevenue);

        // Modelo de Comisión (10% admin, 90% negocios)
        dto.setGananciaAdmin(totalRevenue * 0.10);
        dto.setPagoNegocios(totalRevenue * 0.90);

        // Rangos de Tiempo
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(7);
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate startOfYear = today.withDayOfYear(1);

        double revToday = 0.0;
        double revSevenDays = 0.0;
        double revMonth = 0.0;
        double revYear = 0.0;

        for (Booking b : completedBookings) {
            LocalDate bDate = b.getDate();
            if (bDate == null) continue;

            double price = b.getService() != null && b.getService().getPrice() != null ? b.getService().getPrice() : 0.0;

            if (bDate.equals(today)) {
                revToday += price;
            }
            if (!bDate.isBefore(sevenDaysAgo) && !bDate.isAfter(today)) {
                revSevenDays += price;
            }
            if (!bDate.isBefore(startOfMonth) && !bDate.isAfter(today)) {
                revMonth += price;
            }
            if (!bDate.isBefore(startOfYear) && !bDate.isAfter(today)) {
                revYear += price;
            }
        }

        dto.setIngresosHoy(revToday);
        dto.setIngresosSieteDias(revSevenDays);
        dto.setIngresosMes(revMonth);
        dto.setIngresosAnio(revYear);

        // Top 5 Negocios por Ingresos
        Map<String, Double> businessRevenueMap = new HashMap<>();
        for (Booking b : completedBookings) {
            if (b.getBusiness() != null && b.getBusiness().getName() != null) {
                String bizName = b.getBusiness().getName();
                double price = b.getService() != null && b.getService().getPrice() != null ? b.getService().getPrice() : 0.0;
                businessRevenueMap.put(bizName, businessRevenueMap.getOrDefault(bizName, 0.0) + price);
            }
        }
        List<AdminDashboardDTO.BusinessRevenueDTO> topNegocios = businessRevenueMap.entrySet().stream()
                .map(e -> new AdminDashboardDTO.BusinessRevenueDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(AdminDashboardDTO.BusinessRevenueDTO::getTotalRevenue).reversed())
                .limit(5)
                .collect(Collectors.toList());
        dto.setTopNegocios(topNegocios);

        // Ingresos por Categoría de Servicio (agrupado por nombre del servicio)
        Map<String, Double> serviceRevenueMap = new HashMap<>();
        for (Booking b : completedBookings) {
            if (b.getService() != null && b.getService().getName() != null) {
                String srvName = b.getService().getName();
                double price = b.getService().getPrice() != null ? b.getService().getPrice() : 0.0;
                serviceRevenueMap.put(srvName, serviceRevenueMap.getOrDefault(srvName, 0.0) + price);
            }
        }
        List<AdminDashboardDTO.CategoryRevenueDTO> ingresosPorCategoria = serviceRevenueMap.entrySet().stream()
                .map(e -> new AdminDashboardDTO.CategoryRevenueDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(AdminDashboardDTO.CategoryRevenueDTO::getTotalRevenue).reversed())
                .collect(Collectors.toList());
        dto.setIngresosPorCategoria(ingresosPorCategoria);

        // Historial Mensual del año en curso
        String[] monthNames = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
        double[] monthlySums = new double[12];
        for (Booking b : completedBookings) {
            LocalDate bDate = b.getDate();
            if (bDate != null && bDate.getYear() == today.getYear()) {
                int monthIdx = bDate.getMonthValue() - 1;
                double price = b.getService() != null && b.getService().getPrice() != null ? b.getService().getPrice() : 0.0;
                monthlySums[monthIdx] += price;
            }
        }
        List<AdminDashboardDTO.MonthlyRevenueDTO> tendenciaMensual = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            tendenciaMensual.add(new AdminDashboardDTO.MonthlyRevenueDTO(monthNames[i], monthlySums[i]));
        }
        dto.setTendenciaMensual(tendenciaMensual);

        return ResponseEntity.ok(dto);
    }

    // listar todos los usuarios
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userRepository.findAll().stream().map(u -> {
            UserDTO dto = new UserDTO();
            dto.setId(u.getId());
            dto.setName(u.getName());
            dto.setEmail(u.getEmail());
            dto.setPhone(u.getPhone());
            dto.setRole(u.getRole().name());
            return dto;
        }).toList();
        return ResponseEntity.ok(users);
    }

    // cambiar rol de usuario
    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserDTO> changeRole(@PathVariable Long id,
                                              @RequestParam String role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        user.setRole(Role.valueOf(role.toUpperCase()));
        user = userRepository.save(user);
        
        // Sincronizar el cambio de rol con Auth0 en segundo plano o de forma síncrona
        auth0ManagementService.updateUserRoleInAuth0(user.getEmail(), role.toUpperCase());
        
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole().name());
        return ResponseEntity.ok(dto);
    }

    // eliminar usuario
    // En AdminController.java
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            // Esto devolverá un error claro al frontend en lugar de un 403 genérico
            return ResponseEntity.status(409).body("No se puede eliminar: El usuario tiene negocios o registros asociados.");
        }
    }


    // listar todos los negocios
    @GetMapping("/businesses")
    public ResponseEntity<List<BusinessDTO>> getAllBusinesses() {
        List<BusinessDTO> businesses = businessRepository.findAll().stream().map(b -> {
            BusinessDTO dto = new BusinessDTO();
            dto.setId(b.getId());
            dto.setName(b.getName());
            dto.setDescription(b.getDescription());
            dto.setAddress(b.getAddress());
            dto.setPhone(b.getPhone());
            dto.setCategory(b.getCategory());
            dto.setLatitude(b.getLatitude());
            dto.setLongitude(b.getLongitude());
            dto.setPhotoUrl(b.getPhotoUrl());
            if (b.getStatus() != null) {
                dto.setStatus(b.getStatus().name());
            }
            return dto;
        }).toList();
        return ResponseEntity.ok(businesses);
    }

    // eliminar negocio
    @DeleteMapping("/businesses/{id}")
    public ResponseEntity<Void> deleteBusiness(@PathVariable Long id) {
        businessRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }


    // listar negocios por estado
    @GetMapping("/businesses/status/{status}")
    public ResponseEntity<List<BusinessDTO>> getBusinessesByStatus(@PathVariable String status) {
        BusinessStatus businessStatus = BusinessStatus.valueOf(status.toUpperCase());
        List<BusinessDTO> businesses = businessRepository.findByStatus(businessStatus)
                .stream().map(b -> {
                    BusinessDTO dto = new BusinessDTO();
                    dto.setId(b.getId());
                    dto.setName(b.getName());
                    dto.setDescription(b.getDescription());
                    dto.setAddress(b.getAddress());
                    dto.setPhone(b.getPhone());
                    dto.setCategory(b.getCategory());
                    dto.setLatitude(b.getLatitude());
                    dto.setLongitude(b.getLongitude());
                    dto.setPhotoUrl(b.getPhotoUrl());
                    if (b.getStatus() != null) {
                        dto.setStatus(b.getStatus().name());
                    }
                    return dto;
                }).toList();
        return ResponseEntity.ok(businesses);
    }

    @PutMapping("/businesses/{id}/approve")
    public ResponseEntity<Void> approveBusiness(@PathVariable Long id) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));
        business.setStatus(BusinessStatus.APPROVED);
        Business saved = businessRepository.save(business);
        
        try {
            if (saved.getOwner() != null) {
                emailService.sendBusinessApprovedEmail(saved.getOwner().getEmail(), saved.getName(), saved.getOwner().getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/businesses/{id}/reject")
    public ResponseEntity<Void> rejectBusiness(@PathVariable Long id) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));
        business.setStatus(BusinessStatus.REJECTED);
        Business saved = businessRepository.save(business);
        
        try {
            if (saved.getOwner() != null) {
                emailService.sendBusinessRejectedEmail(saved.getOwner().getEmail(), saved.getName(), saved.getOwner().getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/businesses/{id}/block")
    public ResponseEntity<Void> blockBusiness(@PathVariable Long id) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));
        business.setStatus(BusinessStatus.BLOCKED);
        businessRepository.save(business);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}