package com.avandesk.api.usuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerificarEmailRequest(

    @Schema(description = "Token recebido no link do e-mail de verificação")
    @NotBlank(message = "é obrigatório")
    @Size(max = 100, message = "deve ter no máximo 100 caracteres")
    String token
) {

    @Override
    public String toString() {
        return "VerificarEmailRequest[token=***]";
    }
}
