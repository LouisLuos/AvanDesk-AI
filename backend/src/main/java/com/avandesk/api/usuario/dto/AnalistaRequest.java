package com.avandesk.api.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cadastro e edição de analista (HU10). O perfil é sempre ANALISTA_SUPORTE e a senha vem do servidor. */
public record AnalistaRequest(

    @NotBlank(message = "é obrigatório")
    @Size(max = 120, message = "deve ter no máximo 120 caracteres")
    String nome,

    @NotBlank(message = "é obrigatório")
    @Email(message = "deve ser um e-mail válido")
    @Size(max = 254, message = "deve ter no máximo 254 caracteres")
    String email
) {
}
