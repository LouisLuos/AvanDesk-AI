package com.avandesk.api.chamado.dto;

import com.avandesk.api.chamado.entity.Escalonamento;

import java.time.Instant;
import java.util.UUID;

/** Envio do chamado a um fornecedor externo e o retorno dele (HU07). Quem enviou e quem registrou vão só pelo nome. */
public record EscalonamentoResponse(

    UUID id,
    String fornecedor,
    String protocoloExterno,
    String observacao,
    Instant enviadoEm,
    String enviadoPor,
    ResponsavelResponse responsavel,
    String retorno,
    Instant retornoEm,
    String retornoRegistradoPor,
    boolean aberto
) {

    public static EscalonamentoResponse de(Escalonamento escalonamento) {
        return new EscalonamentoResponse(
            escalonamento.getId(),
            escalonamento.getFornecedor(),
            escalonamento.getProtocoloExterno(),
            escalonamento.getObservacao(),
            escalonamento.getEnviadoEm(),
            escalonamento.getEnviadoPor().getNome(),
            ResponsavelResponse.de(escalonamento.getResponsavel()),
            escalonamento.getRetorno(),
            escalonamento.getRetornoEm(),
            escalonamento.getRetornoRegistradoPor() == null ? null : escalonamento.getRetornoRegistradoPor().getNome(),
            escalonamento.aberto()
        );
    }
}
