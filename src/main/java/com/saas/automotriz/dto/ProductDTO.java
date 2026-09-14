package com.saas.automotriz.dto;

import lombok.Data;

@Data
public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer stock;
    private String category;
    private String brand;
    private String photoUrl;
    private String businessName;
    private String supplier;
    private Boolean igv;
    private Boolean deliveryAvailable;
    private Long localId;

}
