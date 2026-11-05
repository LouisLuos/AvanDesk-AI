package com.avandesk.api.usuario.dto;

import com.avandesk.api.usuario.entity.Usuario;

import java.time.Instant;
import java.util.UUID;

public record AnalistaResponse(
    UUID id,
    String nome,
    String email,
    boolean ativo,
    Instant criadoEm
) {

    public static AnalistaResponse de(Usuario usuario) {
        return new AnalistaResponse(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.isAtivo(),
            usuario.getCriadoEm()
        );
    }
}
