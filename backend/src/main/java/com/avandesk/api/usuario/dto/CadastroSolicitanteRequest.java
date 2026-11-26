package com.avandesk.api.usuario.dto;

import com.avandesk.api.chamado.dto.SolicitanteRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Locale;
import java.util.UUID;

public record CadastroSolicitanteRequest(

    @Schema(example = "Mariana Alves")
    @NotBlank(message = "é obrigatório")
    @Size(max = 120, message = "deve ter no máximo 120 caracteres")
    String nome,

    @Schema(example = "mariana.alves@gmail.com")
    @NotBlank(message = "é obrigatório")
    @Email(regexp = SolicitanteRequest.FORMATO_EMAIL, message = "deve ser um e-mail válido")
    @Size(max = 254, message = "deve ter no máximo 254 caracteres")
    String email,

    @Schema(description = "Unidade padrão do solicitante (ativa)")
    @NotNull(message = "é obrigatório")
    UUID unidadeId,

    @Schema(description = "10 a 72 caracteres, com letra maiúscula, minúscula, número e símbolo")
    @NotBlank(message = "é obrigatório")
    @Size(max = 72, message = "deve ter no máximo 72 caracteres")
    String senha
) {

    public CadastroSolicitanteRequest {
        nome = nome == null ? null : nome.trim();
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "CadastroSolicitanteRequest[nome=%s, email=%s, unidadeId=%s]".formatted(nome, email, unidadeId);
    }
}
