package com.saas.automotriz.dto;

import lombok.Data;

@Data
public class MessageContactDTO {
    private Long businessId;
    private Long clientId;
    private String name;
    private String imageUrl;
    private long unreadCount;
}
