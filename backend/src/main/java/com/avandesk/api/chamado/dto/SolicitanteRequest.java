package com.avandesk.api.chamado.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record SolicitanteRequest(

    @Size(max = 120, message = "deve ter no máximo 120 caracteres")
    String nome,

    @NotBlank(message = "é obrigatório")
    @Email(regexp = SolicitanteRequest.FORMATO_EMAIL, message = "deve ser um e-mail válido")
    @Size(max = 254, message = "deve ter no máximo 254 caracteres")
    String email,

    @Size(max = 20, message = "deve ter no máximo 20 caracteres")
    String telefone
) {

    public static final String FORMATO_EMAIL = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$";

    public SolicitanteRequest {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        telefone = telefone == null || telefone.isBlank() ? null : telefone.trim();
    }
}
