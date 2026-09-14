package com.saas.automotriz.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTicketRequest {

    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 255, message = "El asunto no puede superar los 255 caracteres")
    private String subject;

    @NotBlank(message = "La descripción es obligatoria")
    private String description;

    private String category;
}
