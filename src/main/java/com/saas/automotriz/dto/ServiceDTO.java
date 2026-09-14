package com.saas.automotriz.dto;

import lombok.Data;

@Data
public class ServiceDTO {
    private Long id;
    private String name;
    private String description;
    private String category;
    private Double price;
    private Integer duration;
    private Boolean active;
    private Long localId;
}
