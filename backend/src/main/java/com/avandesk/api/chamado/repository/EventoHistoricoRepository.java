package com.avandesk.api.chamado.repository;

import com.avandesk.api.chamado.entity.EventoHistorico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventoHistoricoRepository extends JpaRepository<EventoHistorico, UUID> {
}
