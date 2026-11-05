package com.avandesk.api.chamado.dto;

import com.avandesk.api.chamado.entity.Prioridade;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PrioridadeRequest(

    @NotNull(message = "é obrigatório")
    @Min(value = 1, message = "deve ser no mínimo 1")
    @Max(value = 3, message = "deve ser no máximo 3")
    Integer severidade,

    @NotNull(message = "é obrigatório")
    @Min(value = 1, message = "deve ser no mínimo 1")
    @Max(value = 3, message = "deve ser no máximo 3")
    Integer urgencia,

    @NotNull(message = "é obrigatório")
    @Min(value = 1, message = "deve ser no mínimo 1")
    @Max(value = 3, message = "deve ser no máximo 3")
    @JsonProperty("impacto_operacional")
    @JsonAlias({"impactoOperacional"})
    Integer impactoOperacional,

    @JsonProperty("prioridade_final")
    @JsonAlias({"prioridadeFinal"})
    Prioridade prioridadeFinal,

    @JsonProperty("justificativa_prioridade")
    @JsonAlias({"justificativaPrioridade", "justificativa"})
    @Size(max = 500, message = "deve ter no máximo 500 caracteres")
    String justificativaPrioridade
) {
}

