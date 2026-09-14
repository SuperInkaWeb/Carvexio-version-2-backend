package com.saas.automotriz.dto;

import lombok.Data;

@Data
public class OrderItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private Double priceAtPurchase;
    private Integer quantity;
    private Double subtotal;
}
