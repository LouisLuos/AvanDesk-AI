package com.avandesk.api.chamado.dto;

import com.avandesk.api.chamado.entity.Chamado;

public record RespostasPrioridadeResponse(
    Short severidade,
    Short urgencia,
    Short impacto
) {

    public static RespostasPrioridadeResponse de(Chamado chamado) {
        if (chamado.getSeveridadeInformada() == null) {
            return null;
        }
        return new RespostasPrioridadeResponse(
            chamado.getSeveridadeInformada(), chamado.getUrgenciaInformada(), chamado.getImpactoInformado());
    }
}
