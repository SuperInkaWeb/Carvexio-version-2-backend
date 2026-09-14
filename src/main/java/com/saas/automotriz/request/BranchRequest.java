package com.saas.automotriz.request;

import lombok.Data;

@Data
public class BranchRequest {
    private String name;
    private String address;
    private String phone;
    private String district;
    private Double latitude;
    private Double longitude;
}
