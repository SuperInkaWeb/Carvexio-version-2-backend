package com.saas.automotriz.dto;

import lombok.Data;

import java.util.List;

@Data
public class CartDTO {
    private Long id;
    private List<CartItemDTO> items;
    private Double discount;
    private Double subtotal;
    private Double discountAmount;
    private Double total;
}
