package com.saas.automotriz.request;



import lombok.Data;

@Data
public class UpdateBusinessRequest {
    private String name;
    private String description;
    private String department;
    private String address;
    private String phone;
    private String category;
    private Double latitude;
    private Double longitude;
}
