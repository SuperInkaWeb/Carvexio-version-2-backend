package com.saas.automotriz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
public class AdminDashboardDTO {
    private Long totalUsuarios;
    private Long totalClientes;
    private Long totalEmpresas;
    private Long totalNegocios;
    private Long totalReservas;
    private Double ingresosTotales;
    private Long totalProductos;

    // Ingresos por rango de tiempo
    private Double ingresosHoy;
    private Double ingresosSieteDias;
    private Double ingresosMes;
    private Double ingresosAnio;

    // Distribución del modelo de comisión
    private Double gananciaAdmin;
    private Double pagoNegocios;

    // Desgloses
    private List<BusinessRevenueDTO> topNegocios;
    private List<CategoryRevenueDTO> ingresosPorCategoria;
    private List<MonthlyRevenueDTO> tendenciaMensual;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BusinessRevenueDTO {
        private String businessName;
        private Double totalRevenue;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryRevenueDTO {
        private String categoryName;
        private Double totalRevenue;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthlyRevenueDTO {
        private String monthName;
        private Double totalRevenue;
    }
}