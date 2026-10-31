package com.avandesk.api.chamado.dto;

import com.avandesk.api.usuario.entity.Usuario;

import java.util.UUID;

/**
 * Responsável pelo chamado: só id e nome, nunca e-mail ou perfil.
 * Mesmo formato no detalhe (HU02) e na listagem (HU14).
 */
public record ResponsavelResponse(UUID id, String nome) {

    /** Devolve null quando o chamado não tem responsável ("Não atribuído"). */
    public static ResponsavelResponse de(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new ResponsavelResponse(usuario.getId(), usuario.getNome());
    }
}
