package com.avandesk.api.chamado.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ResponsavelRequest(

    @NotNull(message = "é obrigatório")
    UUID responsavelId
) {
}
