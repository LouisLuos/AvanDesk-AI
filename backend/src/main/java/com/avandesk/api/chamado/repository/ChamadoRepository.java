package com.avandesk.api.chamado.repository;

import com.avandesk.api.chamado.entity.Chamado;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ChamadoRepository extends JpaRepository<Chamado, UUID> {

    @Query(value = "SELECT nextval('chamados_codigo_seq')", nativeQuery = true)
    Long proximoNumeroCodigo();

    @EntityGraph(attributePaths = {"unidade", "solicitante", "categoria", "sistemaEquipamento", "responsavel"})
    Optional<Chamado> findDetalheById(UUID id);
}
