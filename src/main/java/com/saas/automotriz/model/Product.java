package com.saas.automotriz.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;  // nuevo
    private Double price;
    private Integer stock;

    private String category;     // nuevo - filtros, aceites, frenos, baterías, neumáticos
    private String brand;        // nuevo - marca del repuesto
    private String photoUrl;     // nuevo - para Fase 6 S3

    private Boolean active = true; // soft delete
    private String supplier; // nombre del proveedor
    private Boolean igv = false;
    private Boolean deliveryAvailable = false;
    @ManyToOne
    @JoinColumn(name = "business_id")
    private Business business;   // cambio: de Long a relación real

    @ManyToOne
    @JoinColumn(name = "local_id")
    private Branch branch;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
