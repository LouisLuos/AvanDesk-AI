package com.avandesk.api.usuario.dto;

import com.avandesk.api.usuario.entity.StatusConta;

import java.time.Instant;

public record CadastroSolicitanteResponse(String email, StatusConta statusConta, Instant verificacaoExpiraEm) {
}
