package com.saas.automotriz.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Registro de eventos de seguridad (trazabilidad: quien hizo que).
 * Alimentado por AuditService en acciones sensibles (cambios de rol, estado de
 * negocios, eliminaciones, pagos).
 */
@Data
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String actorEmail;   // quien ejecuto la accion (null = sistema)
    private String actorRole;
    private String action;       // ej. CHANGE_ROLE, APPROVE_BUSINESS, DELETE_USER
    private String targetType;   // ej. USER, BUSINESS, REVIEW
    private Long targetId;

    @Column(length = 1000)
    private String details;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
