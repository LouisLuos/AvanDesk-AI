package com.avandesk.api.chamado.dto;

import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.DirecaoContato;
import com.avandesk.api.chamado.entity.RegistroContato;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma ligação, mensagem, e-mail ou visita registrada no chamado. {@code chamadoOriginal} aponta o chamado
 * em que o contato foi registrado quando ele chegou por uma mesclagem (HU03); nos outros, é null.
 */
public record RegistroContatoResponse(

    UUID id,
    CanalOrigem canal,
    DirecaoContato direcao,
    String conteudo,
    Instant ocorridoEm,
    SolicitanteResponse solicitante,
    String registradoPor,
    ChamadoReferenciaResponse chamadoOriginal
) {

    public static RegistroContatoResponse de(RegistroContato contato) {
        return new RegistroContatoResponse(
            contato.getId(),
            contato.getCanal(),
            contato.getDirecao(),
            contato.getConteudo(),
            contato.getOcorridoEm(),
            SolicitanteResponse.de(contato.getSolicitante()),
            contato.getRegistradoPor() == null ? null : contato.getRegistradoPor().getNome(),
            ChamadoReferenciaResponse.de(contato.getChamadoOriginal())
        );
    }
}
