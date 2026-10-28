package com.avandesk.api.chamado.repository;

import com.avandesk.api.chamado.entity.RegistroContato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RegistroContatoRepository extends JpaRepository<RegistroContato, UUID> {
}
