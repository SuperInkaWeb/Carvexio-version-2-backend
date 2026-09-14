package com.saas.automotriz.dto;

import lombok.Data;

@Data
public class VehicleDTO {
    private Long id;
    private String vehicleType;
    private String plate;
    private String vin;
    private Integer mileage;
    private Integer yearsOfUse;
}
