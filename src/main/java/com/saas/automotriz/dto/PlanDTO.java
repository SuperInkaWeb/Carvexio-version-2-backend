package com.saas.automotriz.dto;

import lombok.Data;

@Data
public class PlanDTO {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Double commission;
    private Boolean hasMarketplace;
    private Boolean hasCrm;
    private Boolean hasInventory;
    private Boolean hasReports;
    private Boolean hasWhatsapp;
}