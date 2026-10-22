package com.avandesk.api.repository;

import com.avandesk.api.entity.SistemaEquipamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SistemaEquipamentoRepository extends JpaRepository<SistemaEquipamento, UUID> {
}
