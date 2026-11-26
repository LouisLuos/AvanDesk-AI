package com.avandesk.api.chamado.dto;

import jakarta.validation.constraints.Size;

public record FechamentoRequest(

    @Size(max = 500, message = "deve ter no máximo 500 caracteres")
    String justificativaSemCausaRaiz
) {
}
