package com.saas.automotriz.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ClientSummaryDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Long totalReservas;
    private Long totalPedidos;
    private Double montoTotalServicios;
    private Double montoTotalProductos;
    private Double montoTotalTotal;
    private LocalDateTime ultimaVisita;
    private List<VehicleDTO> vehicles;
}
