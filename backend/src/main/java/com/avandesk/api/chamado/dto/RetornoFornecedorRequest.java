package com.avandesk.api.chamado.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record RetornoFornecedorRequest(

    @NotBlank(message = "é obrigatório")
    @Size(max = 2000, message = "deve ter no máximo 2000 caracteres")
    String retorno,

    @JsonProperty("retorno_em")
    @JsonAlias({"retornoEm"})
    Instant retornoEm
) {
}
