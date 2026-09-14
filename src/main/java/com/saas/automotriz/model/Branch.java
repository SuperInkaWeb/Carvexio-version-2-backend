package com.saas.automotriz.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "locales")
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String address;
    private String phone;
    private String district;
    private Double latitude;
    private Double longitude;

    private Boolean active = true;

    @ManyToOne
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
