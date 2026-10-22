package com.avandesk.api.dto;

import com.avandesk.api.entity.SistemaEquipamento;

import java.util.UUID;

public record SistemaEquipamentoResponse(UUID id, String nome) {

    public static SistemaEquipamentoResponse de(SistemaEquipamento sistema) {
        return new SistemaEquipamentoResponse(sistema.getId(), sistema.getNome());
    }
}
