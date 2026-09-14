package com.saas.automotriz.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "vehicles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_vehicle_plate", columnNames = "plate"),
        @UniqueConstraint(name = "uk_vehicle_vin", columnNames = "vin")
})
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @Column(nullable = false, length = 100)
    private String vehicleType;

    @Column(nullable = false, length = 16)
    private String plate;

    @Column(nullable = false, length = 17)
    private String vin;

    @Column(nullable = false)
    private Integer mileage;

    @Column(name = "year", nullable = false)
    private Integer yearsOfUse;
}
