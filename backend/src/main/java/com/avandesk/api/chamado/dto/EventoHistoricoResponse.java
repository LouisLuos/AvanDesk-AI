package com.avandesk.api.chamado.dto;

import com.avandesk.api.chamado.entity.EventoHistorico;
import com.avandesk.api.chamado.entity.TipoEvento;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Um acontecimento da linha do tempo, com os nomes do contrato (docs/06, seção 2). */
public record EventoHistoricoResponse(

    UUID idEvento,
    TipoEvento tipo,
    Instant data,
    String autor,
    Map<String, Object> detalhes
) {

    /** O autor vai só pelo nome; fica null quando o evento é gerado pelo sistema. */
    public static EventoHistoricoResponse de(EventoHistorico evento) {
        return new EventoHistoricoResponse(
            evento.getId(),
            evento.getTipo(),
            evento.getOcorridoEm(),
            evento.getAutor() == null ? null : evento.getAutor().getNome(),
            evento.getDados()
        );
    }
}
