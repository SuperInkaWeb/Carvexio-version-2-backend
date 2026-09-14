package com.saas.automotriz.request;

import lombok.Data;

@Data
public class ApplyDiscountRequest {
    private Double discount; // porcentaje 0-100
}