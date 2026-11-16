package com.avandesk.api.chamado.dto;

import com.avandesk.api.chamado.entity.Chamado;

import java.util.UUID;

/** Referência curta a outro chamado (HU03): o principal de uma mesclagem, os mesclados nele e a origem de um contato. */
public record ChamadoReferenciaResponse(

    UUID id,
    String codigo
) {

    public static ChamadoReferenciaResponse de(Chamado chamado) {
        return chamado == null ? null : new ChamadoReferenciaResponse(chamado.getId(), chamado.getCodigo());
    }
}
