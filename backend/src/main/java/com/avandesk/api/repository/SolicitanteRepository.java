package com.avandesk.api.repository;

import com.avandesk.api.entity.Solicitante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SolicitanteRepository extends JpaRepository<Solicitante, UUID> {

    Optional<Solicitante> findByEmail(String email);

    Optional<Solicitante> findFirstByTelefoneAndNomeIgnoreCase(String telefone, String nome);

    Optional<Solicitante> findFirstByTelefoneAndNomeIsNull(String telefone);
}
