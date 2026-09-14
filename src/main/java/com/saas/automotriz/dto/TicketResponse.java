package com.saas.automotriz.dto;

import com.saas.automotriz.model.TicketPriority;
import com.saas.automotriz.model.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class TicketResponse {
    private UUID publicId;
    private String subject;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private String category;
    private LocalDateTime createdAt;
}
