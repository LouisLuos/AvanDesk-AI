package com.avandesk.api.chamado.dto;

import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.DirecaoContato;
import com.avandesk.api.chamado.entity.RegistroContato;

import java.time.Instant;
import java.util.UUID;

/** Uma ligação, mensagem, e-mail ou visita registrada no chamado. */
public record RegistroContatoResponse(

    UUID id,
    CanalOrigem canal,
    DirecaoContato direcao,
    String conteudo,
    Instant ocorridoEm,
    SolicitanteResponse solicitante,
    String registradoPor
) {

    public static RegistroContatoResponse de(RegistroContato contato) {
        return new RegistroContatoResponse(
            contato.getId(),
            contato.getCanal(),
            contato.getDirecao(),
            contato.getConteudo(),
            contato.getOcorridoEm(),
            SolicitanteResponse.de(contato.getSolicitante()),
            contato.getRegistradoPor() == null ? null : contato.getRegistradoPor().getNome()
        );
    }
}
