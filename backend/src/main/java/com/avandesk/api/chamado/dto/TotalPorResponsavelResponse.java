package com.avandesk.api.chamado.dto;

import java.util.UUID;

/** Chamados em andamento de um responsável (HU10): tudo o que não está resolvido nem encerrado. */
public record TotalPorResponsavelResponse(UUID responsavelId, long total) {
}
