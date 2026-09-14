package com.saas.automotriz.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReportDTO {
    private Long totalReservas;
    private Long reservasCompletadas;
    private Long reservasCanceladas;
    private Double ingresosPeriodo;
    private List<ServiceStatsDTO> serviciosMasSolicitados;
    private List<ClientStatsDTO> clientesFrecuentes;
}
