package com.avandesk.api.notificacao.entity;

public enum SituacaoNotificacao {
    PENDENTE,
    ENVIADA,
    FALHOU,
    ESGOTADA;

    public boolean aguardandoEnvio() {
        return this == PENDENTE || this == FALHOU;
    }
}
