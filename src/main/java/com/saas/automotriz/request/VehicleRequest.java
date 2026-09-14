package com.saas.automotriz.request;

import lombok.Data;

@Data
public class VehicleRequest {
    private String vehicleType;
    private String plate;
    private String vin;
    private Integer mileage;
    private Integer yearsOfUse;
}
