package com.avandesk.api.chamado.repository;

import com.avandesk.api.chamado.entity.RegistroContato;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegistroContatoRepository extends JpaRepository<RegistroContato, UUID> {

    @EntityGraph(attributePaths = {"solicitante", "registradoPor"})
    List<RegistroContato> findByChamadoIdOrderByOcorridoEmAsc(UUID chamadoId);
}
