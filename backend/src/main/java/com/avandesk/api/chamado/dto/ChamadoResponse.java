package com.avandesk.api.chamado.dto;

import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.OrigemDado;
import com.avandesk.api.chamado.entity.StatusChamado;

import java.time.Instant;
import java.util.UUID;

public record ChamadoResponse(

    UUID id,
    String codigo,
    StatusChamado status,
    Instant dataCriacao,
    OrigemDado origemDado
) {

    public static ChamadoResponse de(Chamado chamado) {
        return new ChamadoResponse(
            chamado.getId(),
            chamado.getCodigo(),
            chamado.getStatus(),
            chamado.getAbertoEm(),
            chamado.getOrigemDado()
        );
    }
}
