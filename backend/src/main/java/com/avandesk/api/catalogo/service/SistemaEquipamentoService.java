package com.avandesk.api.catalogo.service;

import com.avandesk.api.catalogo.dto.SistemaEquipamentoResponse;
import com.avandesk.api.catalogo.repository.SistemaEquipamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
