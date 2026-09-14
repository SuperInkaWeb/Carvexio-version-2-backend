package com.saas.automotriz.controller;

import com.saas.automotriz.dto.BusinessDocumentResponse;
import com.saas.automotriz.model.User;
import com.saas.automotriz.service.BusinessDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Lado del negocio: subir documentos de verificación (KYC) y ver su estado.
 */
@RestController
@RequestMapping("/api/business/documents")
@RequiredArgsConstructor
public class BusinessDocumentController {

    private final BusinessDocumentService documentService;

    @PostMapping
    public ResponseEntity<BusinessDocumentResponse> upload(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam("documentNumber") String documentNumber) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.uploadDocument(user, file, documentType, documentNumber));
    }

    @GetMapping
    public ResponseEntity<List<BusinessDocumentResponse>> getMyDocuments(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(documentService.getMyDocuments(user));
    }
}
