package com.avandesk.api.core.exception;

import lombok.Getter;

@Getter
public class TokenVerificacaoInvalidoException extends RuntimeException {

    public enum Motivo {
        TOKEN_INVALIDO,
        TOKEN_UTILIZADO,
        TOKEN_SUBSTITUIDO,
        TOKEN_EXPIRADO
    }

    private final Motivo motivo;

    public TokenVerificacaoInvalidoException(Motivo motivo, String mensagem) {
        super(mensagem);
        this.motivo = motivo;
    }
}
