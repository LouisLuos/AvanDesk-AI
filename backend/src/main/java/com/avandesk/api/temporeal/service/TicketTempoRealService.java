package com.avandesk.api.temporeal.service;

import com.avandesk.api.temporeal.dto.TicketTempoRealResponse;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.service.JwtService;
import com.avandesk.api.usuario.service.UsuarioLogadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TicketTempoRealService {

    public static final Duration VALIDADE_TICKET = Duration.ofSeconds(60);

    private final JwtService jwtService;
    private final UsuarioLogadoService usuarioLogadoService;

    public TicketTempoRealResponse emitir() {
        Usuario usuario = usuarioLogadoService.obterUsuarioLogado();
        Instant expiraEm = Instant.now().plus(VALIDADE_TICKET);
        return new TicketTempoRealResponse(jwtService.gerarTicketTempoReal(usuario, VALIDADE_TICKET), expiraEm);
    }
}
