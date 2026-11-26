package com.avandesk.api.chamado.repository;

import com.avandesk.api.chamado.dto.ChamadoReferenciaResponse;
import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.OrigemDado;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChamadoRepository extends JpaRepository<Chamado, UUID>, JpaSpecificationExecutor<Chamado>,
    ChamadoContagemRepository {

    @Query(value = "SELECT nextval('chamados_codigo_seq')", nativeQuery = true)
    Long proximoNumeroCodigo();

    long countByOrigemDado(OrigemDado origemDado);

    long countBySolicitanteId(UUID solicitanteId);

    @EntityGraph(attributePaths = {
        "unidade", "solicitante", "categoria", "sistemaEquipamento", "responsavel", "chamadoPrincipal"
    })
    Optional<Chamado> findDetalheById(UUID id);

    @Query("""
        SELECT new com.avandesk.api.chamado.dto.ChamadoReferenciaResponse(c.id, c.codigo)
        FROM Chamado c
        WHERE c.chamadoPrincipal.id = :principalId
        ORDER BY c.abertoEm ASC
        """)
    List<ChamadoReferenciaResponse> listarMesclados(@Param("principalId") UUID principalId);

    /**
     * Mesclagem (HU03): trava as linhas dos chamados até o fim da transação, sempre na ordem do id, para duas
     * mesclagens cruzadas ao mesmo tempo (A em B e B em A) não passarem juntas pelas checagens nem travarem uma à outra.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Chamado c WHERE c.id IN :ids ORDER BY c.id")
    List<Chamado> travarParaMesclagem(@Param("ids") Collection<UUID> ids);

    /**
     * Fila de chamados (HU14): já traz unidade, categoria e responsável de cada linha, para não fazer
     * uma consulta por chamado. A contagem da paginação não usa o grafo.
     */
    @Override
    @EntityGraph(attributePaths = {"unidade", "categoria", "responsavel"})
    Page<Chamado> findAll(Specification<Chamado> consulta, Pageable paginacao);
}
