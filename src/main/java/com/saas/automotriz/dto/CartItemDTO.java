package com.saas.automotriz.dto;

import lombok.Data;

@Data
public class CartItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private Double price;
    private Integer quantity;
    private Integer stock;
    private Double subtotal;
    private Boolean deliveryAvailable;
    private String businessName;
}
