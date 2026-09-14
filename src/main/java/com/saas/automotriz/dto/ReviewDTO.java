package com.saas.automotriz.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewDTO {
    private Long id;
    private Integer rating;
    private String comment;
    private String clientName;
    private LocalDateTime createdAt;
}
