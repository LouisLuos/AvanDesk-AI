package com.avandesk.api.usuario.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record LoginResponse(
    @JsonIgnore String accessToken,
    UsuarioAutenticadoResponse usuario
) {
}
