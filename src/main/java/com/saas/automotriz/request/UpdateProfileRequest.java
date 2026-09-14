package com.saas.automotriz.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String name;
    private String phone;
}