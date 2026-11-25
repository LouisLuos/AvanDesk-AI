package com.avandesk.api.core.exception;

import java.util.List;

public class OperacaoNaoPermitidaException extends RuntimeException {

    private final List<String> erros;

    public OperacaoNaoPermitidaException(String mensagem) {
        this(mensagem, List.of(mensagem));
    }

    public OperacaoNaoPermitidaException(String mensagem, List<String> erros) {
        super(mensagem);
        this.erros = List.copyOf(erros);
    }

    public List<String> getErros() {
        return erros;
    }
}
