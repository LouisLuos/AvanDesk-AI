package com.avandesk.api.temporeal.dto;

import com.avandesk.api.chamado.dto.ChamadoResumoResponse;
import com.avandesk.api.chamado.dto.ResponsavelResponse;

import java.time.Instant;

public record EventoTempoRealMessage(
    TipoEventoTempoReal tipo,
    ChamadoResumoResponse chamado,
    ResponsavelResponse autor,
    Instant ocorridoEm
) {
}
