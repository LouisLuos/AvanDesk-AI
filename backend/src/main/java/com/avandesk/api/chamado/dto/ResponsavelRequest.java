package com.avandesk.api.chamado.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ResponsavelRequest(

    @NotNull(message = "é obrigatório")
    @JsonProperty("responsavel_id")
    @JsonAlias({"responsavelId"})
    UUID responsavelId
) {
}
