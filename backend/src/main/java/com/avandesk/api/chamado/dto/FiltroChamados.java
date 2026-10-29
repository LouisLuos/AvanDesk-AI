package com.avandesk.api.chamado.dto;

import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.Prioridade;
import com.avandesk.api.chamado.entity.StatusChamado;

import java.util.List;
import java.util.UUID;

/**
 * Filtros da fila de chamados (HU14). Todos são opcionais e se somam: cada um informado restringe mais
 * o resultado. O filtro "meus chamados" já chega aqui convertido em {@code responsavelId}.
 *
 * @param status         um ou mais status aceitos; vazio ou nulo não filtra
 * @param semResponsavel quando true, só chamados com {@code responsavel_id IS NULL}
 * @param semPrioridade  quando true, só chamados ainda sem prioridade final (aguardando triagem)
 * @param codigo         código exato do chamado (LUM-AAAA-NNNNNN), sem diferenciar maiúsculas
 */
public record FiltroChamados(
    List<StatusChamado> status,
    UUID unidadeId,
    Prioridade prioridade,
    CanalOrigem canalOrigem,
    UUID responsavelId,
    boolean semResponsavel,
    boolean semPrioridade,
    String codigo
) {

    /** Mesmo filtro, sem o status: usado para contar os chamados de cada status (cartões do topo). */
    public FiltroChamados semStatus() {
        return new FiltroChamados(null, unidadeId, prioridade, canalOrigem, responsavelId, semResponsavel,
            semPrioridade, codigo);
    }

    /** Mesmo filtro, restrito a um responsável (usado pelo "meus chamados"). */
    public FiltroChamados comResponsavel(UUID responsavel) {
        return new FiltroChamados(status, unidadeId, prioridade, canalOrigem, responsavel, semResponsavel,
            semPrioridade, codigo);
    }
}
