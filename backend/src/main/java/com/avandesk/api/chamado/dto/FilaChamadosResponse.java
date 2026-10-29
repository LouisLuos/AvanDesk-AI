package com.avandesk.api.chamado.dto;

import com.avandesk.api.chamado.entity.StatusChamado;

import java.util.List;
import java.util.Map;

/**
 * Página da fila de chamados (HU14). Formato próprio, sem expor o Page do Spring, para o contrato
 * da API não mudar quando o framework mudar.
 *
 * @param pagina          número da página, começando em 0
 * @param totaisPorStatus quantos chamados de cada status atendem aos filtros, ignorando o filtro de
 *                        status (cartões do topo da tela). Todos os status aparecem, mesmo com zero.
 */
public record FilaChamadosResponse(
    List<ChamadoResumoResponse> itens,
    int pagina,
    int tamanho,
    long totalItens,
    int totalPaginas,
    Map<StatusChamado, Long> totaisPorStatus
) {
}
