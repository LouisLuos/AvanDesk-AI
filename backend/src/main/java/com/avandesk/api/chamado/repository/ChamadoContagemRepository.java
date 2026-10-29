package com.avandesk.api.chamado.repository;

import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.StatusChamado;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

/** Contagens agregadas da fila, que o {@code JpaSpecificationExecutor} não faz (GROUP BY). */
public interface ChamadoContagemRepository {

    /** Quantos chamados de cada status atendem à consulta. Status sem chamados não aparecem. */
    Map<StatusChamado, Long> contarPorStatus(Specification<Chamado> consulta);
}
