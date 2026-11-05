package com.avandesk.api.chamado.repository;

import com.avandesk.api.chamado.entity.Escalonamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EscalonamentoRepository extends JpaRepository<Escalonamento, UUID> {
}
