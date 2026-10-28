package com.avandesk.api.catalogo.service;

import com.avandesk.api.catalogo.dto.CategoriaResponse;
import com.avandesk.api.catalogo.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarAtivas() {
        return categoriaRepository.findByAtivoTrueOrderByNomeAsc().stream()
            .map(CategoriaResponse::de)
            .toList();
    }
}
