package com.avandesk.api.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "E-mail com formato inválido.")
    String email,

    @NotBlank(message = "A senha é obrigatória.")
    String senha
) {
}
