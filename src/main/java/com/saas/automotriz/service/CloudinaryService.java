package com.saas.automotriz.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    // Instancia unica reutilizable (antes se reconstruia en cada llamada).
    private Cloudinary cloudinary;

    @PostConstruct
    void init() {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    /** Imagenes publicas (fotos de negocio, logos, productos). */
    public String uploadImage(MultipartFile file) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", "automotriz"));
            return (String) result.get("secure_url");
        } catch (Exception e) {
            throw new RuntimeException("Error al subir imagen");
        }
    }

    /** Documentos publicos de proposito general (ej. adjuntos del libro de reclamaciones). */
    public String uploadDocument(MultipartFile file) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "automotriz_docs",
                            "resource_type", "auto"
                    ));
            return (String) result.get("secure_url");
        } catch (Exception e) {
            throw new RuntimeException("Error al subir documento");
        }
    }

    /**
     * Documentos KYC (ID-004b): se suben como 'authenticated', de modo que la URL
     * publica responde 401. La entrega requiere una URL firmada (generateSignedUrl),
     * que solo se emite en los flujos autorizados (dueno del negocio o admin).
     */
    public UploadResult uploadKycDocument(MultipartFile file) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "automotriz_docs",
                            "resource_type", "auto",
                            "type", "authenticated"
                    ));
            return new UploadResult(
                    (String) result.get("public_id"),
                    (String) result.get("resource_type"),
                    (String) result.get("secure_url")
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al subir documento");
        }
    }

    /**
     * URL firmada de entrega para un documento 'authenticated'. Sin la firma
     * (derivada del api_secret) Cloudinary responde 401, por lo que el documento
     * nunca es accesible publicamente.
     */
    public String generateSignedUrl(String publicId, String resourceType) {
        return cloudinary.url()
                .resourceType(resourceType != null ? resourceType : "image")
                .type("authenticated")
                .secure(true)
                .signed(true)
                .generate(publicId);
    }

    /** Datos que el servicio de documentos necesita persistir para poder re-firmar. */
    public record UploadResult(String publicId, String resourceType, String secureUrl) {
    }
}
