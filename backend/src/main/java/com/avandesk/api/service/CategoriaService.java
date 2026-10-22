package com.avandesk.api.service;

import com.avandesk.api.dto.CategoriaResponse;
import com.avandesk.api.entity.Categoria;
import com.avandesk.api.exception.RecursoNaoEncontradoException;
import com.avandesk.api.exception.RegraNegocioException;
import com.avandesk.api.repository.CategoriaRepository;
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

    public Categoria obterAtivaParaNovoChamado(UUID id) {
        Categoria categoria = categoriaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
        if (!categoria.isAtivo()) {
            throw new RegraNegocioException("Categoria inativa não pode ser usada em novos chamados.");
        }
        return categoria;
    }
}
