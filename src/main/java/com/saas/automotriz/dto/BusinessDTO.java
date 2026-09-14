package com.saas.automotriz.dto;


import lombok.Data;

import java.util.List;

@Data
public class BusinessDTO {
    private Long id;
    private String name;
    private String description;
    private String department;
    private String address;
    private String phone;
    private String category;
    private Double latitude;
    private Double longitude;
    private String photoUrl;
    private String logoUrl;
    private String status;
    private Long planId;
    private String planName;
    private Boolean hasCrm;
    private List<BranchDTO> branches;
}
