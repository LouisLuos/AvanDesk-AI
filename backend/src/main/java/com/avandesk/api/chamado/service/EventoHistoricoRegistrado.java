package com.avandesk.api.chamado.service;

import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.TipoEvento;
import com.avandesk.api.usuario.entity.Usuario;

import java.time.Instant;

public record EventoHistoricoRegistrado(
    Chamado chamado,
    TipoEvento tipo,
    Usuario autor,
    Instant ocorridoEm
) {
}
