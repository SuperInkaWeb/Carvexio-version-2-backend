package com.saas.automotriz.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;        // Básico, Profesional, Premium
    private String description;
    private Double price;       // precio mensual
    private Double commission;  // % de comisión por transacción

    // features incluidas
    private Boolean hasMarketplace = false;
    private Boolean hasCrm = false;
    private Boolean hasInventory = false;
    private Boolean hasReports = false;
    private Boolean hasWhatsapp = false;

    private Boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;
}