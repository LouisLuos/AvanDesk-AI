package com.avandesk.api.repository;

import com.avandesk.api.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {

    List<Categoria> findByAtivoTrueOrderByNomeAsc();
}
