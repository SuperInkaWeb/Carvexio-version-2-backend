package com.saas.automotriz.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "El mensaje es obligatorio")
    private String message;

    // Opcional: si se continúa una sesión existente
    private String sessionPublicId;
}
