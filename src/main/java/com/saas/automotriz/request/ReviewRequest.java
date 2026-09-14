package com.saas.automotriz.request;

import lombok.Data;

@Data
public class ReviewRequest {
    private Integer rating; // 1-5
    private String comment;
    private Long businessId;
}