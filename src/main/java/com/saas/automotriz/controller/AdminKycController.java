package com.saas.automotriz.controller;

import com.saas.automotriz.dto.BusinessDocumentResponse;
import com.saas.automotriz.model.DocumentStatus;
import com.saas.automotriz.service.BusinessDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lado del admin: revisar los documentos de verificación (KYC) de un negocio.
 * La ruta /api/admin/** está restringida a rol ADMIN en SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminKycController {

    private final BusinessDocumentService documentService;

    @GetMapping("/businesses/{businessId}/documents")
    public ResponseEntity<List<BusinessDocumentResponse>> getBusinessDocuments(@PathVariable Long businessId) {
        return ResponseEntity.ok(documentService.getBusinessDocuments(businessId));
    }

    @PutMapping("/documents/{documentId}/review")
    public ResponseEntity<BusinessDocumentResponse> reviewDocument(
            @PathVariable Long documentId,
            @RequestParam("status") DocumentStatus status) {
        return ResponseEntity.ok(documentService.reviewDocument(documentId, status));
    }
}
