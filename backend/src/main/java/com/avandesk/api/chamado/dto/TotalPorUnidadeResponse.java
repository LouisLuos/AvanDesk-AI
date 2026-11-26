package com.avandesk.api.chamado.dto;

import java.util.UUID;

/** Chamados abertos por unidade numa janela recente de dias (HU11). */
public record TotalPorUnidadeResponse(UUID unidadeId, long total) {
}
