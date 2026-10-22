package com.avandesk.api.service;

import com.avandesk.api.dto.SistemaEquipamentoResponse;
import com.avandesk.api.entity.SistemaEquipamento;
import com.avandesk.api.exception.RecursoNaoEncontradoException;
import com.avandesk.api.exception.RegraNegocioException;
import com.avandesk.api.repository.SistemaEquipamentoRepository;
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

    public SistemaEquipamento obterAtivoParaNovoChamado(UUID id) {
        SistemaEquipamento sistema = sistemaEquipamentoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Sistema/equipamento não encontrado."));
        if (!sistema.isAtivo()) {
            throw new RegraNegocioException("Sistema/equipamento inativo não pode ser usado em novos chamados.");
        }
        return sistema;
    }
}
