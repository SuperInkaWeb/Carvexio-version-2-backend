package com.saas.automotriz.request;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class BookingRequest {
    private Long serviceId;
    private LocalDate date;
    private LocalTime time;
    private String notes;
    private Long localId;
    private Long vehicleId;
}
