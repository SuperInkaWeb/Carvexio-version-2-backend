package com.saas.automotriz.request;

import com.saas.automotriz.model.TransactionType;
import lombok.Data;

@Data
public class TransactionRequest {
    private TransactionType type;
    private Double amount;
    private String paymentMethod;
    private Long referenceId;
    private String description;
}