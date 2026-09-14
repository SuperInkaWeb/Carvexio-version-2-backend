package com.saas.automotriz.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDTO {
    private Long id;
    private Long clientId;
    private String clientName;
    private Long businessId;
    private String businessName;
    private Double totalAmount;
    private Double discountAmount;
    private Double paidAmount;
    private String status;
    private String deliveryMethod;
    private String address;
    private String phone;
    private String notes;
    private LocalDateTime createdAt;
    private List<OrderItemDTO> items;
}
