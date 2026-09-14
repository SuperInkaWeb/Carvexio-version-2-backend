package com.saas.automotriz.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "services")
public class AutoService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String category;
    private Double price;
    private Integer duration; // en minutos

    private Boolean active = true; // nuevo - para "eliminar" sin borrar de BD

    @ManyToOne
    @JoinColumn(name = "business_id")
    private Business business; // cambio: de Long a relación real

    @ManyToOne
    @JoinColumn(name = "local_id")
    private Branch branch;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
