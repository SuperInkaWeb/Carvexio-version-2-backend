package com.saas.automotriz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SessionMessageDto {
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
