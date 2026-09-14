package com.saas.automotriz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SessionSummaryDto {
    private String sessionPublicId;
    private String title;
    private int messageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
