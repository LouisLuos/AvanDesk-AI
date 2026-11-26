package com.avandesk.api.usuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record ReenvioVerificacaoRequest(

    @Schema(example = "mariana.alves@gmail.com")
    @NotBlank(message = "é obrigatório")
    @Email(message = "deve ser um e-mail válido")
    @Size(max = 254, message = "deve ter no máximo 254 caracteres")
    String email
) {

    public ReenvioVerificacaoRequest {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
