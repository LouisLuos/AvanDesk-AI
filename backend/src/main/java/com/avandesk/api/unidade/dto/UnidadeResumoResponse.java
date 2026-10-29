package com.avandesk.api.unidade.dto;

import com.avandesk.api.unidade.entity.Unidade;

import java.util.UUID;

/** Unidade resumida (id e nome), usada dentro das respostas do chamado. */
public record UnidadeResumoResponse(UUID id, String nome) {

    public static UnidadeResumoResponse de(Unidade unidade) {
        return new UnidadeResumoResponse(unidade.getId(), unidade.getNome());
    }
}
