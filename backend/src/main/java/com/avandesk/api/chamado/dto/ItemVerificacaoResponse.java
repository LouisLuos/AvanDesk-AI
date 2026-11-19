package com.avandesk.api.chamado.dto;

public record ItemVerificacaoResponse(
    String codigo,
    String descricao,
    boolean ok,
    String pendencia
) {
}
