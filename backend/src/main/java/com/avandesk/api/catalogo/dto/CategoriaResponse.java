package com.avandesk.api.catalogo.dto;

import com.avandesk.api.catalogo.entity.Categoria;

import java.util.UUID;

public record CategoriaResponse(UUID id, String nome) {

    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome());
    }
}
