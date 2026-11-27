package com.avandesk.api.temporeal.dto;

import com.avandesk.api.chamado.entity.TipoEvento;

public enum TipoEventoTempoReal {
    CHAMADO_ATUALIZADO,
    STATUS_ALTERADO,
    CHAMADO_ATRIBUIDO,
    CHAMADO_CRIADO;

    public static TipoEventoTempoReal de(TipoEvento tipo) {
        if (tipo == TipoEvento.CHAMADO_ABERTO) {
            return CHAMADO_CRIADO;
        }
        if (tipo == TipoEvento.CHAMADO_ATRIBUIDO) {
            return CHAMADO_ATRIBUIDO;
        }
        if (TipoEvento.MUDANCAS_DE_STATUS.contains(tipo)) {
            return STATUS_ALTERADO;
        }
        return CHAMADO_ATUALIZADO;
    }

    public TipoEventoTempoReal maisRelevante(TipoEventoTempoReal outro) {
        return outro.ordinal() > ordinal() ? outro : this;
    }
}
