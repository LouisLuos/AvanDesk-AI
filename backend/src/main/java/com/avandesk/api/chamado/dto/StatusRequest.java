package com.avandesk.api.chamado.dto;

import com.avandesk.api.chamado.entity.MotivoEspera;
import com.avandesk.api.chamado.entity.StatusChamado;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatusRequest(

    @NotNull(message = "é obrigatório")
    StatusChamado status,

    MotivoEspera motivoEspera,

    @Size(max = 500, message = "deve ter no máximo 500 caracteres")
    String motivoCancelamento
) {
}
