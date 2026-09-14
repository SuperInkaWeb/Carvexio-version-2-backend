package com.saas.automotriz.request;

import lombok.Data;

@Data
public class DirectMessageRequest {
    private Long businessId;
    private Long clientId;
    private String content;
}
