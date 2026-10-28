package com.avandesk.api.catalogo.repository;

import com.avandesk.api.catalogo.entity.SistemaEquipamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SistemaEquipamentoRepository extends JpaRepository<SistemaEquipamento, UUID> {

    List<SistemaEquipamento> findByAtivoTrueOrderByNomeAsc();
}
