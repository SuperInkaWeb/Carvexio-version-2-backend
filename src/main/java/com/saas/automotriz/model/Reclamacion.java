package com.saas.automotriz.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "reclamaciones")
public class Reclamacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_reclamo", unique = true)
    private String codigoReclamo;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    private String docType;
    private String docNumber;
    private String firstName;
    private String middleName;
    private String lastName1;
    private String lastName2;
    private String address;
    private String department;
    private String province;
    private String district;
    private String email;
    private String phone;

    private boolean minor;
    private String guardianDocType;
    private String guardianDocNumber;
    private String guardianFirstName;
    private String guardianMiddleName;
    private String guardianLastName1;
    private String guardianLastName2;
    private String guardianEmail;
    private String guardianPhone;

    private String claimType;
    private String productOrService;
    private String orderNumber;
    private String claimedAmount;
    private String productName;
    private String productDescription;
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(columnDefinition = "TEXT")
    private String pedido;

    @Column(name = "archivo_sustento_url")
    private String archivoSustentoUrl;

    @Column(name = "pdf_filename")
    private String pdfFilename;

    @Column(nullable = false)
    private String estado = "REGISTRADO";

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getFullName() {
        return (safe(firstName) + " " + safe(middleName) + " " + safe(lastName1) + " " + safe(lastName2))
                .replaceAll("\\s+", " ")
                .trim();
    }

    public String getGuardianFullName() {
        return (safe(guardianFirstName) + " " + safe(guardianMiddleName) + " " + safe(guardianLastName1) + " " + safe(guardianLastName2))
                .replaceAll("\\s+", " ")
                .trim();
    }

    public String getFullAddress() {
        return (safe(address) + ", " + safe(district) + ", " + safe(province) + ", " + safe(department))
                .replaceAll("(,\\s*)+", ", ")
                .replaceAll("^,\\s*|,\\s*$", "")
                .trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
