package com.saas.automotriz.service;

import com.saas.automotriz.dto.BusinessDocumentResponse;
import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.BusinessDocument;
import com.saas.automotriz.model.DocumentStatus;
import com.saas.automotriz.model.User;
import com.saas.automotriz.repository.BusinessDocumentRepository;
import com.saas.automotriz.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * VerificaciÃ³n de identidad (KYC) del negocio: el dueÃ±o sube documentos
 * (RUC, licencia) como archivos; el admin los revisa antes de aprobar el negocio.
 */
@Service
@RequiredArgsConstructor
public class BusinessDocumentService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private final BusinessDocumentRepository documentRepository;
    private final BusinessRepository businessRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public BusinessDocumentResponse uploadDocument(User owner, MultipartFile file,
                                                   String documentType, String documentNumber) {
        Business business = businessRepository.findByOwnerId(owner.getId())
                .orElseThrow(() -> new RuntimeException("No tienes un negocio registrado"));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo es obligatorio");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("El archivo supera el tamaño maximo permitido de 5 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Tipo de archivo no permitido. Solo PDF, JPEG y PNG");
        }

        String originalFilename = file.getOriginalFilename();
        String lowerFilename = originalFilename == null ? "" : originalFilename.toLowerCase();
        if (!(lowerFilename.endsWith(".pdf") || lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg") || lowerFilename.endsWith(".png"))) {
            throw new IllegalArgumentException("Extension de archivo no permitida");
        }

        if (documentType == null || documentType.isBlank()) {
            throw new IllegalArgumentException("El tipo de documento es obligatorio");
        }
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new IllegalArgumentException("El numero de documento es obligatorio");
        }

        documentType = documentType.trim();
        documentNumber = documentNumber.trim();
        if (documentType.length() > 50) {
            throw new IllegalArgumentException("El tipo de documento supera la longitud permitida");
        }
        if (documentNumber.length() > 100) {
            throw new IllegalArgumentException("El numero de documento supera la longitud permitida");
        }

        String fileUrl = cloudinaryService.uploadDocument(file);

        BusinessDocument document = new BusinessDocument();
        document.setBusiness(business);
        document.setDocumentType(documentType);
        document.setDocumentNumber(documentNumber);
        document.setFileUrl(fileUrl);

        return toResponse(documentRepository.save(document));
    }

    @Transactional(readOnly = true)
    public List<BusinessDocumentResponse> getMyDocuments(User owner) {
        Business business = businessRepository.findByOwnerId(owner.getId())
                .orElseThrow(() -> new RuntimeException("No tienes un negocio registrado"));


        return getBusinessDocuments(business.getId());
    }

    @Transactional(readOnly = true)
    public List<BusinessDocumentResponse> getBusinessDocuments(Long businessId) {
        return documentRepository.findByBusinessIdOrderByCreatedAtDesc(businessId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public BusinessDocumentResponse reviewDocument(Long documentId, DocumentStatus newStatus) {
        if (newStatus != DocumentStatus.VERIFIED && newStatus != DocumentStatus.REJECTED) {
            throw new RuntimeException("Estado invÃ¡lido: use VERIFIED o REJECTED");
        }
        BusinessDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        document.setStatus(newStatus);
        document.setReviewedAt(LocalDateTime.now());
        return toResponse(documentRepository.save(document));
    }

    private BusinessDocumentResponse toResponse(BusinessDocument d) {
        return new BusinessDocumentResponse(
                d.getId(),
                d.getDocumentType(),
                d.getDocumentNumber(),
                d.getFileUrl(),
                d.getStatus(),
                d.getCreatedAt(),
                d.getReviewedAt()
        );
    }
}








