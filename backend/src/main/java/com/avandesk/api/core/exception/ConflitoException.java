package com.avandesk.api.core.exception;

/** O recurso já existe com o mesmo identificador único (ex.: e-mail de usuário). Responde 409. */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
