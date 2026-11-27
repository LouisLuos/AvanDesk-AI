package com.avandesk.api.chamado.repository;

import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.StatusChamado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Contagens agregadas e consultas paginadas customizadas da fila. */
public interface ChamadoContagemRepository {

    /** Quantos chamados de cada status atendem à consulta. Status sem chamados não aparecem. */
    Map<StatusChamado, Long> contarPorStatus(Specification<Chamado> consulta);

    /** Lista chamados paginados ordenando pela última atualização ({@code COALESCE(MAX(ocorrido_em), aberto_em)}). */
    Page<Chamado> listarOrdenadoPorAtualizacao(Specification<Chamado> consulta, Pageable paginacao, boolean crescente);

    /** Quantos chamados ativos da consulta ultrapassaram o prazo de SLA, descontando pausas do solicitante. */
    long contarEstourados(Specification<Chamado> consulta);

    /** Quantos chamados em andamento cada responsável tem (HU10). Responsáveis sem chamados não aparecem. */
    Map<UUID, Long> contarEmAndamentoPorResponsavel();

    /** Quantos chamados foram abertos em cada unidade a partir de {@code desde} (HU11). Unidades sem chamados não aparecem. */
    Map<UUID, Long> contarPorUnidadeDesde(Instant desde);
}
