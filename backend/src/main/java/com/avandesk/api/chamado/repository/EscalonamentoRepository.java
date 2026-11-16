package com.avandesk.api.chamado.repository;

import com.avandesk.api.chamado.entity.Escalonamento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EscalonamentoRepository extends JpaRepository<Escalonamento, UUID> {

    @EntityGraph(attributePaths = {"responsavel", "enviadoPor", "retornoRegistradoPor"})
    List<Escalonamento> findByChamadoIdOrderByEnviadoEmAsc(UUID chamadoId);
}
