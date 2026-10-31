package com.avandesk.api.chamado.dto;

import com.avandesk.api.chamado.entity.MotivoEspera;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatusRequest(

    @NotNull(message = "é obrigatório")
    StatusChamado status,

    @JsonProperty("motivo_espera")
    @JsonAlias({"motivoEspera"})
    MotivoEspera motivoEspera,

    @JsonProperty("motivo_cancelamento")
    @JsonAlias({"motivoCancelamento"})
    @Size(max = 500, message = "deve ter no máximo 500 caracteres")
    String motivoCancelamento
) {
}
