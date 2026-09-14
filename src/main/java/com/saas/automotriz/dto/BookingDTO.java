package com.saas.automotriz.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class BookingDTO {
    private Long id;
    private String serviceName;
    private String businessName;
    private String clientName;
    private LocalDate date;
    private LocalTime time;
    private String status;
    private String notes;
    private Long localId;
    private Double servicePrice;
    private LocalDateTime createdAt;
    private VehicleDTO vehicle;
}
