package com.saas.automotriz.dto;

import lombok.Data;

import java.time.LocalTime;

@Data
public class BranchAvailabilityDTO {
    private Long id;
    private Long branchId;
    private Long businessId;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer capacity;
    private Boolean enabled;
}
