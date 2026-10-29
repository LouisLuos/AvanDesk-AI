package com.avandesk.api.chamado.repository;

import com.avandesk.api.chamado.dto.FiltroChamados;
import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.Prioridade;
import com.avandesk.api.chamado.entity.StatusChamado;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Consulta dinâmica da fila de chamados (HU14): cada filtro preenchido vira uma condição, e todas
 * precisam ser verdadeiras ao mesmo tempo. Filtro vazio não restringe nada.
 */
public final class ChamadoSpecification {

    private ChamadoSpecification() {
    }

    public static Specification<Chamado> de(FiltroChamados filtro) {
        List<Specification<Chamado>> condicoes = new ArrayList<>();
        if (filtro.status() != null && !filtro.status().isEmpty()) {
            condicoes.add(comStatus(filtro.status()));
        }
        if (filtro.unidadeId() != null) {
            condicoes.add(daUnidade(filtro.unidadeId()));
        }
        if (filtro.prioridade() != null) {
            condicoes.add(comPrioridade(filtro.prioridade()));
        }
        if (filtro.canalOrigem() != null) {
            condicoes.add(doCanal(filtro.canalOrigem()));
        }
        if (filtro.responsavelId() != null) {
            condicoes.add(doResponsavel(filtro.responsavelId()));
        }
        if (filtro.semResponsavel()) {
            condicoes.add(semResponsavel());
        }
        if (filtro.semPrioridade()) {
            condicoes.add(semPrioridade());
        }
        if (filtro.codigo() != null && !filtro.codigo().isBlank()) {
            condicoes.add(comCodigo(filtro.codigo()));
        }
        return condicoes.isEmpty() ? Specification.unrestricted() : Specification.allOf(condicoes);
    }

    static Specification<Chamado> comStatus(Collection<StatusChamado> status) {
        return (root, query, cb) -> root.get("status").in(status);
    }

    static Specification<Chamado> daUnidade(UUID unidadeId) {
        return (root, query, cb) -> cb.equal(root.get("unidade").get("id"), unidadeId);
    }

    /**
     * Filtra pela prioridade final avaliada ou sugerida.
     */
    static Specification<Chamado> comPrioridade(Prioridade prioridade) {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("prioridadeFinal"), prioridade),
            cb.and(cb.isNull(root.get("prioridadeFinal")), cb.equal(root.get("prioridadeSugerida"), prioridade))
        );
    }

    static Specification<Chamado> doCanal(CanalOrigem canal) {
        return (root, query, cb) -> cb.equal(root.get("canalOrigem"), canal);
    }

    static Specification<Chamado> doResponsavel(UUID responsavelId) {
        return (root, query, cb) -> cb.equal(root.get("responsavel").get("id"), responsavelId);
    }

    static Specification<Chamado> semResponsavel() {
        return (root, query, cb) -> cb.isNull(root.get("responsavel"));
    }

    /** Chamados ainda sem prioridade definida (nem final nem sugerida). */
    static Specification<Chamado> semPrioridade() {
        return (root, query, cb) -> cb.and(
            cb.isNull(root.get("prioridadeFinal")),
            cb.isNull(root.get("prioridadeSugerida"))
        );
    }

    /**
     * Busca flexível por código (exato ou parcial) ou por palavras no nome/descrição do chamado (case-insensitive).
     */
    static Specification<Chamado> comCodigo(String termo) {
        String normalizado = termo.trim().toLowerCase(Locale.ROOT);
        String likePattern = "%" + normalizado + "%";
        return (root, query, cb) -> cb.or(
            cb.like(cb.lower(root.get("codigo")), likePattern),
            cb.like(cb.lower(root.get("descricao")), likePattern)
        );
    }
}
