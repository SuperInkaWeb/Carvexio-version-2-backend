package com.saas.automotriz.dto;

import lombok.Data;

@Data
public class ClientStatsDTO {
    private Long clientId;
    private String nombre;
    private Long totalReservas;
}