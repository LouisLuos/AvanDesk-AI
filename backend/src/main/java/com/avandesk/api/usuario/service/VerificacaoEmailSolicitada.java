package com.avandesk.api.usuario.service;

import java.time.Instant;
import java.util.UUID;

public record VerificacaoEmailSolicitada(UUID usuarioId, String nome, String email, String token, Instant expiraEm) {

    @Override
    public String toString() {
        return "VerificacaoEmailSolicitada[usuarioId=%s, expiraEm=%s]".formatted(usuarioId, expiraEm);
    }
}
