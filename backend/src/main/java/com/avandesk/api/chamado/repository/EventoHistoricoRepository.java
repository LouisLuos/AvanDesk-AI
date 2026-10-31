package com.avandesk.api.chamado.repository;

import com.avandesk.api.chamado.entity.EventoHistorico;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EventoHistoricoRepository extends JpaRepository<EventoHistorico, UUID> {

    @EntityGraph(attributePaths = "autor")
    List<EventoHistorico> findByChamadoIdOrderByOcorridoEmAsc(UUID chamadoId);

    /** Último evento de cada chamado da página da fila (coluna "Atualizado"), numa consulta só. */
    @Query("""
        select e.chamado.id as chamadoId, max(e.ocorridoEm) as ultimaAtualizacao
        from EventoHistorico e
        where e.chamado.id in :chamadoIds
        group by e.chamado.id
        """)
    List<UltimaAtualizacao> buscarUltimaAtualizacao(@Param("chamadoIds") Collection<UUID> chamadoIds);

    interface UltimaAtualizacao {
        UUID getChamadoId();

        Instant getUltimaAtualizacao();
    }
}
