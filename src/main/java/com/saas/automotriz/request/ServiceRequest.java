package com.saas.automotriz.request;

import lombok.Data;

@Data
public class ServiceRequest {
    private String name;
    private String description;
    private String category;
    private Double price;
    private Integer duration;
    private Long localId;
}
