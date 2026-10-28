package com.avandesk.api.usuario.dto;

public record LoginResponse(
    String accessToken,
    UsuarioAutenticadoResponse usuario
) {
}
