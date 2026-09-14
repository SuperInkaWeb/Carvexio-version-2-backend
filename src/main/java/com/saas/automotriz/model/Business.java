package com.saas.automotriz.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "businesses")
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;  // nuevo
    private String category;
    private String department;
    private String address;
    private String phone;
    private String photoUrl;     // nuevo - para la foto de portada
    private String logoUrl;      // nuevo - para el logo de la empresa
    private String type;
    private Double latitude;     // nuevo - para búsqueda por ubicación
    private Double longitude;    // nuevo

    @OneToOne
    @JoinColumn(name = "owner_id", unique = true)
    private User owner;          // cambio: de Long a relación real

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private BusinessStatus status = BusinessStatus.PENDING; // por defecto pendiente

    @ManyToOne
    @JoinColumn(name = "plan_id")
    private Plan plan; // plan activo del negocio
}
