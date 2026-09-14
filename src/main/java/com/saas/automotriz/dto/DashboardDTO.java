package com.saas.automotriz.dto;

import lombok.Data;

@Data
public class DashboardDTO {
    private Long reservasHoy;
    private Long reservasPendientes;
    private Long reservasCompletadas;

    private Long pedidosTotales;
    private Long pedidosPendientes;
    private Long pedidosCompletados;

    private Double ingresosServicios;
    private Double ingresosProductos;
    private Double ingresosTotal;

    private Long clientesTotal;
    private String servicioMasSolicitado;
    private String productoMasVendido;
}