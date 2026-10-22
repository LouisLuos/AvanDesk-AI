package com.avandesk.api.repository;

import com.avandesk.api.entity.RegistroContato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RegistroContatoRepository extends JpaRepository<RegistroContato, UUID> {
}
