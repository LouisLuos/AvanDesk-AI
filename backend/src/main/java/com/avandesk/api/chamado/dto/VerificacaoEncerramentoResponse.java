package com.avandesk.api.chamado.dto;

import java.util.List;

public record VerificacaoEncerramentoResponse(
    boolean aprovado,
    int itensConformes,
    int totalItens,
    List<ItemVerificacaoResponse> itens
) {

    public static VerificacaoEncerramentoResponse de(List<ItemVerificacaoResponse> itens) {
        int conformes = (int) itens.stream().filter(ItemVerificacaoResponse::ok).count();
        return new VerificacaoEncerramentoResponse(conformes == itens.size(), conformes, itens.size(), itens);
    }

    public List<String> pendencias() {
        return itens.stream().filter(item -> !item.ok()).map(ItemVerificacaoResponse::pendencia).toList();
    }
}
