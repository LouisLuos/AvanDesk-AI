package com.avandesk.api.chamado.dto;

/**
 * Ordenação opcional da fila de chamados (HU14).
 * {@code PADRAO} ordena por abertura mais recente; as demais ordenam pela última atualização
 * ({@code COALESCE(MAX(evento.ocorridoEm), chamado.abertoEm)}).
 */
public enum OrdenacaoFila {
    PADRAO,
    ATUALIZADO_DESC,
    ATUALIZADO_ASC
}
