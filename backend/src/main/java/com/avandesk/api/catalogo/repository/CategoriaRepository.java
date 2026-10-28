package com.avandesk.api.catalogo.repository;

import com.avandesk.api.catalogo.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {

    List<Categoria> findByAtivoTrueOrderByNomeAsc();
}
