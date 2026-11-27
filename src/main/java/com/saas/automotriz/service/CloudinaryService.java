package com.saas.automotriz.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;       // 5 MB
    private static final long MAX_DOCUMENT_BYTES = 10L * 1024 * 1024;   // 10 MB
    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> DOCUMENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif", "application/pdf");

    // Validacion server-side del archivo (el 'accept' del frontend es evadible).
    // Nota: valida el content-type declarado; para endurecer, verificar magic bytes
    // (Apache Tika) en una iteracion posterior.
    private void validate(MultipartFile file, Set<String> allowedTypes, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo esta vacio");
        }
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "El archivo supera el tamano maximo permitido");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Tipo de archivo no permitido");
        }
    }

    public String uploadImage(MultipartFile file) {
        validate(file, IMAGE_TYPES, MAX_IMAGE_BYTES);
        try {
            Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret
            ));

            Map result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", "automotriz"));

            return (String) result.get("secure_url");
        } catch (Exception e) {
            throw new RuntimeException("Error al subir imagen");
        }
    }

    public String uploadDocument(MultipartFile file) {
        validate(file, DOCUMENT_TYPES, MAX_DOCUMENT_BYTES);
        try {
            Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret
            ));

            Map result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "automotriz_docs",
                            "resource_type", "auto"
                    ));

            return (String) result.get("secure_url");
        } catch (Exception e) {
            throw new RuntimeException("Error al subir documento");
        }
    }
}
