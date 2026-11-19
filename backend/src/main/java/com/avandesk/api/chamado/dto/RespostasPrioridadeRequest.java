package com.avandesk.api.chamado.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RespostasPrioridadeRequest(

    @NotNull(message = "é obrigatório")
    @Min(value = 1, message = "deve ser no mínimo 1")
    @Max(value = 3, message = "deve ser no máximo 3")
    Short severidade,

    @NotNull(message = "é obrigatório")
    @Min(value = 1, message = "deve ser no mínimo 1")
    @Max(value = 3, message = "deve ser no máximo 3")
    Short urgencia,

    @NotNull(message = "é obrigatório")
    @Min(value = 1, message = "deve ser no mínimo 1")
    @Max(value = 3, message = "deve ser no máximo 3")
    Short impacto
) {
}
