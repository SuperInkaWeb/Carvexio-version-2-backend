package com.saas.automotriz.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TransactionDTO {
    private Long id;
    private String type;
    private String status;
    private Double amount;
    private String currency;
    private String paymentMethod;
    private String description;
    private LocalDateTime createdAt;
}
