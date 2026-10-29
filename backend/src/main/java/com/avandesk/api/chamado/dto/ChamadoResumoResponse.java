package com.avandesk.api.chamado.dto;

import com.avandesk.api.catalogo.dto.CategoriaResponse;
import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.Prioridade;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.unidade.dto.UnidadeResumoResponse;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma linha da fila de chamados (HU14). Sem SLA por enquanto: o cálculo do prazo é de outra HU.
 *
 * @param titulo       primeira linha da descrição, com até {@value #TAMANHO_TITULO} caracteres
 * @param prioridade   prioridade final definida pelo analista ou sugerida; nula se ainda não avaliada
 * @param responsavel  nulo quando o chamado não tem responsável ("Não atribuído")
 * @param atualizadoEm último evento do histórico; sem eventos, a data de abertura
 */
public record ChamadoResumoResponse(
    UUID id,
    String codigo,
    String titulo,
    UnidadeResumoResponse unidade,
    CategoriaResponse categoria,
    CanalOrigem canalOrigem,
    StatusChamado status,
    Prioridade prioridade,
    ResponsavelResponse responsavel,
    Instant abertoEm,
    Instant atualizadoEm
) {

    static final int TAMANHO_TITULO = 80;

    public static ChamadoResumoResponse de(Chamado chamado, Instant ultimaAtualizacao) {
        Prioridade prioridade = chamado.getPrioridadeFinal() != null
            ? chamado.getPrioridadeFinal()
            : chamado.getPrioridadeSugerida();

        return new ChamadoResumoResponse(
            chamado.getId(),
            chamado.getCodigo(),
            titulo(chamado.getDescricao()),
            UnidadeResumoResponse.de(chamado.getUnidade()),
            chamado.getCategoria() == null ? null : CategoriaResponse.de(chamado.getCategoria()),
            chamado.getCanalOrigem(),
            chamado.getStatus(),
            prioridade,
            ResponsavelResponse.de(chamado.getResponsavel()),
            chamado.getAbertoEm(),
            ultimaAtualizacao != null ? ultimaAtualizacao : chamado.getAbertoEm()
        );
    }

    static String titulo(String descricao) {
        String primeiraLinha = descricao.strip().lines().findFirst().orElse("").strip();
        if (primeiraLinha.length() <= TAMANHO_TITULO) {
            return primeiraLinha;
        }
        return primeiraLinha.substring(0, TAMANHO_TITULO - 1).stripTrailing() + "…";
    }
}
