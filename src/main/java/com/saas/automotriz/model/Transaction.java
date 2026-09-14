package com.saas.automotriz.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User client;

    @Enumerated(EnumType.STRING)
    private TransactionType type; // BOOKING o PRODUCT

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;// PENDING, COMPLETED, FAILED, REFUNDED

    private Double amount;
    private String currency = "PEN"; // soles por defecto

    private String paymentMethod; // MERCADOPAGO, STRIPE, CASH
    private String externalId;    // ID que devuelve MercadoPago cuando lo integres

    private Long referenceId;     // ID de la reserva o carrito
    private String description;

    @CreationTimestamp
    private LocalDateTime createdAt;
}