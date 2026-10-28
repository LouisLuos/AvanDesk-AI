package com.avandesk.api.catalogo.service;

import com.avandesk.api.catalogo.dto.SistemaEquipamentoResponse;
import com.avandesk.api.catalogo.entity.SistemaEquipamento;
import com.avandesk.api.catalogo.repository.SistemaEquipamentoRepository;
import com.avandesk.api.core.exception.RecursoNaoEncontradoException;
import com.avandesk.api.core.exception.RegraNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SistemaEquipamentoService {

    private final SistemaEquipamentoRepository sistemaEquipamentoRepository;

    @Transactional(readOnly = true)
    public List<SistemaEquipamentoResponse> listarAtivos() {
        return sistemaEquipamentoRepository.findByAtivoTrueOrderByNomeAsc().stream()
            .map(SistemaEquipamentoResponse::de)
            .toList();
    }

    @Transactional(readOnly = true)
    public SistemaEquipamento obterAtivoParaNovoChamado(UUID id) {
        SistemaEquipamento sistema = sistemaEquipamentoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Sistema/equipamento não encontrado."));
        if (!sistema.isAtivo()) {
            throw new RegraNegocioException("Sistema/equipamento inativo não pode ser usado em novos chamados.");
        }
        return sistema;
    }
}
