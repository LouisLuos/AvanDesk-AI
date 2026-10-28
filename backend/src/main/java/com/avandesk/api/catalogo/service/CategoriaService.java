package com.avandesk.api.catalogo.service;

import com.avandesk.api.catalogo.dto.CategoriaResponse;
import com.avandesk.api.catalogo.entity.Categoria;
import com.avandesk.api.catalogo.repository.CategoriaRepository;
import com.avandesk.api.core.exception.RecursoNaoEncontradoException;
import com.avandesk.api.core.exception.RegraNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    @Transactional(readOnly = true)
    public Categoria obterAtivaParaNovoChamado(UUID id) {
        Categoria categoria = categoriaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
        if (!categoria.isAtivo()) {
            throw new RegraNegocioException("Categoria inativa não pode ser usada em novos chamados.");
        }
        return categoria;
    }
}
