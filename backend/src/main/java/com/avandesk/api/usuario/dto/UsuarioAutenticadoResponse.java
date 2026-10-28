package com.avandesk.api.usuario.dto;

import com.avandesk.api.usuario.entity.PerfilUsuario;
import com.avandesk.api.usuario.entity.Usuario;

import java.util.UUID;

public record UsuarioAutenticadoResponse(
    UUID id,
    String nome,
    String email,
    PerfilUsuario perfil
) {

    public static UsuarioAutenticadoResponse de(Usuario usuario) {
        return new UsuarioAutenticadoResponse(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getPerfil()
        );
    }
}
