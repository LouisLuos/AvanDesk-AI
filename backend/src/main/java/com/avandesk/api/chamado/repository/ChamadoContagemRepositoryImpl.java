package com.avandesk.api.chamado.repository;

import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.StatusChamado;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.EnumMap;
import java.util.Map;

/** Implementação do fragmento {@link ChamadoContagemRepository}, montada pelo Spring Data. */
class ChamadoContagemRepositoryImpl implements ChamadoContagemRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Map<StatusChamado, Long> contarPorStatus(Specification<Chamado> consulta) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Chamado> root = query.from(Chamado.class);
        Path<StatusChamado> status = root.get("status");

        Predicate condicao = consulta.toPredicate(root, query, cb);
        if (condicao != null) {
            query.where(condicao);
        }
        query.multiselect(status, cb.count(root)).groupBy(status);

        Map<StatusChamado, Long> totais = new EnumMap<>(StatusChamado.class);
        for (Tuple linha : entityManager.createQuery(query).getResultList()) {
            totais.put(linha.get(0, StatusChamado.class), linha.get(1, Long.class));
        }
        return totais;
    }
}
