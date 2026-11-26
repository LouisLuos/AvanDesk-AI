package com.avandesk.api.chamado.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MesclagemRequest(

    @JsonProperty("chamado_principal_id")
    @JsonAlias({"chamadoPrincipalId"})
    @NotNull(message = "é obrigatório")
    UUID chamadoPrincipalId
) {
}
