package com.saas.automotriz.controller;

import com.saas.automotriz.dto.ClientStatsDTO;
import com.saas.automotriz.dto.ReportDTO;
import com.saas.automotriz.dto.ServiceStatsDTO;
import com.saas.automotriz.model.Booking;
import com.saas.automotriz.model.BookingStatus;
import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.User;
import com.saas.automotriz.repository.BookingRepository;
import com.saas.automotriz.repository.BusinessRepository;
import com.saas.automotriz.service.PlanLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final BookingRepository bookingRepository;
    private final BusinessRepository businessRepository;
    private final PlanLimitService planLimitService;

    @GetMapping
    public ResponseEntity<ReportDTO> getReport(@AuthenticationPrincipal User user,
                                               @RequestParam LocalDate start,
                                               @RequestParam LocalDate end,
                                               @RequestParam(required = false) Long localId) {
        Business business = businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));
        planLimitService.checkReportsAccess(business);

        List<Booking> bookings = bookingRepository.findByBusinessAndDateBetween(business, start, end);
        if (localId != null) {
            bookings = bookings.stream().filter(b -> b.getBranch() != null && b.getBranch().getId().equals(localId)).toList();
        }

        // totales
        long completadas = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        long canceladas = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();
        double ingresos = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .mapToDouble(b -> b.getService().getPrice()).sum();

        // servicios más solicitados
        List<ServiceStatsDTO> topServicios = bookingRepository.findTopServices(business)
                .stream().map(row -> {
                    ServiceStatsDTO dto = new ServiceStatsDTO();
                    dto.setNombre((String) row[0]);
                    dto.setTotal((Long) row[1]);
                    return dto;
                }).toList();

        // clientes frecuentes
        List<ClientStatsDTO> topClientes = bookingRepository.findTopClients(business)
                .stream().map(row -> {
                    User client = (User) row[0];
                    ClientStatsDTO dto = new ClientStatsDTO();
                    dto.setClientId(client.getId());
                    dto.setNombre(client.getName());
                    dto.setTotalReservas((Long) row[1]);
                    return dto;
                }).toList();

        ReportDTO report = new ReportDTO();
        report.setTotalReservas((long) bookings.size());
        report.setReservasCompletadas(completadas);
        report.setReservasCanceladas(canceladas);
        report.setIngresosPeriodo(ingresos);
        report.setServiciosMasSolicitados(topServicios);
        report.setClientesFrecuentes(topClientes);

        return ResponseEntity.ok(report);
    }
}


