package com.avandesk.api.chamado.repository;

import com.avandesk.api.chamado.entity.Chamado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ChamadoRepository extends JpaRepository<Chamado, UUID>, JpaSpecificationExecutor<Chamado>,
    ChamadoContagemRepository {

    @Query(value = "SELECT nextval('chamados_codigo_seq')", nativeQuery = true)
    Long proximoNumeroCodigo();

    @EntityGraph(attributePaths = {"unidade", "solicitante", "categoria", "sistemaEquipamento", "responsavel"})
    Optional<Chamado> findDetalheById(UUID id);

    /**
     * Fila de chamados (HU14): já traz unidade, categoria e responsável de cada linha, para não fazer
     * uma consulta por chamado. A contagem da paginação não usa o grafo.
     */
    @Override
    @EntityGraph(attributePaths = {"unidade", "categoria", "responsavel"})
    Page<Chamado> findAll(Specification<Chamado> consulta, Pageable paginacao);
}
