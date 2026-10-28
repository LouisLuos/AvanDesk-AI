package com.avandesk.api.catalogo.dto;

import com.avandesk.api.catalogo.entity.SistemaEquipamento;

import java.util.UUID;

public record SistemaEquipamentoResponse(UUID id, String nome) {

    public static SistemaEquipamentoResponse de(SistemaEquipamento sistema) {
        return new SistemaEquipamentoResponse(sistema.getId(), sistema.getNome());
    }
}
