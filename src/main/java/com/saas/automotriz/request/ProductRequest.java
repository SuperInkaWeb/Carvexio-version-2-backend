package com.saas.automotriz.request;

import lombok.Data;

@Data
public class ProductRequest {
    private String name;
    private String description;
    private Double price;
    private Integer stock;
    private String category;
    private String brand;
    private String supplier;
    private Boolean igv;
    private Boolean deliveryAvailable;
    private Long localId;
}
