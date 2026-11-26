package com.avandesk.api.temporeal.controller;

import com.avandesk.api.core.config.SecurityConfig;
import com.avandesk.api.temporeal.dto.TicketTempoRealResponse;
import com.avandesk.api.temporeal.service.TicketTempoRealService;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import com.avandesk.api.usuario.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TempoRealController.class)
@Import(SecurityConfig.class)
class TempoRealControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketTempoRealService ticketTempoRealService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser
    void deveEmitirOTicketParaQuemEstaLogado() throws Exception {
        when(ticketTempoRealService.emitir())
            .thenReturn(new TicketTempoRealResponse("ticket-jwt", Instant.parse("2026-11-26T13:01:00Z")));

        mockMvc.perform(get("/api/v1/tempo-real/ticket"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.ticket").value("ticket-jwt"))
            .andExpect(jsonPath("$.data.expira_em").value("2026-11-26T13:01:00Z"));
    }

    @Test
    void deveExigirLogin() throws Exception {
        mockMvc.perform(get("/api/v1/tempo-real/ticket"))
            .andExpect(status().isUnauthorized());
    }
}
