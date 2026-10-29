package com.avandesk.api.chamado.entity;

import java.util.EnumSet;
import java.util.Set;

public enum StatusChamado {
    ABERTO,
    EM_TRIAGEM,
    EM_ATENDIMENTO,
    AGUARDANDO,
    RESOLVIDO,
    REABERTO,
    FECHADO,
    CANCELADO,
    MESCLADO;

    /** Ciclo de vida do chamado: diagrama v3 da seção 6 de docs/01-analise-de-dominio-Nexo.md. */
    public Set<StatusChamado> proximosStatus() {
        return switch (this) {
            case ABERTO -> EnumSet.of(EM_TRIAGEM, CANCELADO, MESCLADO);
            case EM_TRIAGEM -> EnumSet.of(EM_ATENDIMENTO, CANCELADO, MESCLADO);
            case EM_ATENDIMENTO -> EnumSet.of(AGUARDANDO, RESOLVIDO, MESCLADO);
            case AGUARDANDO -> EnumSet.of(EM_ATENDIMENTO, RESOLVIDO, MESCLADO);
            case RESOLVIDO -> EnumSet.of(REABERTO, FECHADO);
            case REABERTO -> EnumSet.of(EM_ATENDIMENTO, MESCLADO);
            case FECHADO, CANCELADO, MESCLADO -> EnumSet.noneOf(StatusChamado.class);
        };
    }

    public boolean podeIrPara(StatusChamado destino) {
        return proximosStatus().contains(destino);
    }

    public boolean encerrado() {
        return proximosStatus().isEmpty();
    }
}
