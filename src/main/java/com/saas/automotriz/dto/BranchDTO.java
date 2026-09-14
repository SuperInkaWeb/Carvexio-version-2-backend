package com.saas.automotriz.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BranchDTO {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private String district;
    private Double latitude;
    private Double longitude;
    private Boolean active;
    private Long businessId;
    private LocalDateTime createdAt;
}
