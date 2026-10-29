package com.avandesk.api.usuario.controller;

import com.avandesk.api.core.config.SecurityConfig;
import com.avandesk.api.usuario.dto.UsuarioAutenticadoResponse;
import com.avandesk.api.usuario.entity.PerfilUsuario;
import com.avandesk.api.usuario.service.JwtService;
import com.avandesk.api.usuario.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @WithMockUser
    void deveListarAnalistasAtivosComSucesso() throws Exception {
        UUID id = UUID.randomUUID();
        when(usuarioService.listarAnalistasAtivos()).thenReturn(List.of(
            new UsuarioAutenticadoResponse(id, "Analista Um", "analista1@lumen.local", PerfilUsuario.ANALISTA_SUPORTE)
        ));

        mockMvc.perform(get("/api/v1/usuarios/analistas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status_code").value(200))
            .andExpect(jsonPath("$.message").value("Analistas recuperados com sucesso"))
            .andExpect(jsonPath("$.data[0].id").value(id.toString()))
            .andExpect(jsonPath("$.data[0].nome").value("Analista Um"))
            .andExpect(jsonPath("$.data[0].email").value("analista1@lumen.local"));

        verify(usuarioService).listarAnalistasAtivos();
    }

    @Test
    void deveExigirAutenticacaoParaListarAnalistas() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/analistas"))
            .andExpect(status().isUnauthorized());
    }
}
