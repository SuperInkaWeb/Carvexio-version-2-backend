package com.saas.automotriz.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private User client;

    @ManyToOne
    @JoinColumn(name = "business_id")
    private Business business;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private DeliveryMethod deliveryMethod = DeliveryMethod.PICKUP;

    private Double totalAmount;
    private Double discountAmount = 0.0;
    private Double paidAmount;
    private Boolean rewardPointsGranted = false;
    private String address;     // Dirección de envío o recojo
    private String phone;       // Teléfono de contacto para el pedido
    private String notes;       // Notas extra del cliente

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
