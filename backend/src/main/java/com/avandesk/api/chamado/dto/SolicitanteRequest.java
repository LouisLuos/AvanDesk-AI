package com.avandesk.api.chamado.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitanteRequest(

    @Size(max = 120, message = "deve ter no máximo 120 caracteres")
    String nome,

    @NotBlank(message = "é obrigatório")
    @Size(max = 254, message = "deve ter no máximo 254 caracteres")
    String contato
) {
}
