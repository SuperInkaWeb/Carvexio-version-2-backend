package com.saas.automotriz.dto;

import com.saas.automotriz.model.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BusinessDocumentResponse {
    private Long id;
    private String documentType;
    private String documentNumber;
    private String fileUrl;
    private DocumentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
