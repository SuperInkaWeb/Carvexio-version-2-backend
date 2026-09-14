package com.saas.automotriz.dto;

import lombok.Data;

import java.util.List;

@Data
public class ClientHistoryDTO {
    private Long clientId;
    private String clientName;
    private String clientPhone;
    private List<VehicleDTO> vehicles;
    private List<BookingDTO> historial; // Mantener para compatibilidad
    private List<BookingDTO> historialReservas;
    private List<OrderDTO> historialPedidos;
}
