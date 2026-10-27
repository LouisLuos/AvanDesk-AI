package com.avandesk.api.dto;

import com.avandesk.api.entity.Categoria;

import java.util.UUID;

public record CategoriaResponse(UUID id, String nome) {

    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome());
    }
}
