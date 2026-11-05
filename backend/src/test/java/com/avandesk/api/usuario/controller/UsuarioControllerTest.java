package com.avandesk.api.usuario.controller;

import com.avandesk.api.core.config.SecurityConfig;
import com.avandesk.api.core.exception.ConflitoException;
import com.avandesk.api.usuario.dto.AnalistaCadastradoResponse;
import com.avandesk.api.usuario.dto.AnalistaRequest;
import com.avandesk.api.usuario.dto.AnalistaResponse;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import com.avandesk.api.usuario.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Gestão de analistas (HU10): contrato HTTP e controle por perfil. */
@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)
class UsuarioControllerTest {

    private static final UUID ID = UUID.randomUUID();
    private static final String CORPO_VALIDO = """
        {"nome": "Ana Souza", "email": "ana.souza@lumen.local"}
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser(roles = "ANALISTA_SUPORTE")
    void deveListarAnalistasParaQualquerUsuarioAutenticado() throws Exception {
        when(usuarioService.listarAnalistas(true)).thenReturn(List.of(analista(true)));

        mockMvc.perform(get("/api/v1/analistas").param("ativo", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].email").value("ana.souza@lumen.local"))
            .andExpect(jsonPath("$.data[0].criado_em").exists());
    }

    @Test
    void deveRecusarListagemSemToken() throws Exception {
        mockMvc.perform(get("/api/v1/analistas")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ANALISTA_SUPORTE")
    void deveResponder403QuandoAnalistaTentaCadastrar() throws Exception {
        mockMvc.perform(post("/api/v1/analistas").contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Acesso negado para o perfil do usuário."));

        verifyNoInteractions(usuarioService);
    }

    @Test
    @WithMockUser(roles = "LIDERANCA_TI")
    void deveCadastrarComSenhaTemporariaParaALideranca() throws Exception {
        when(usuarioService.cadastrarAnalista(any(AnalistaRequest.class)))
            .thenReturn(new AnalistaCadastradoResponse(analista(true), "Kp7m-Qx4t-Rz9w"));

        mockMvc.perform(post("/api/v1/analistas").contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.analista.nome").value("Ana Souza"))
            .andExpect(jsonPath("$.data.senha_temporaria").value("Kp7m-Qx4t-Rz9w"));
    }

    @Test
    @WithMockUser(roles = "LIDERANCA_TI")
    void deveRecusarCadastroSemNome() throws Exception {
        mockMvc.perform(post("/api/v1/analistas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"ana.souza@lumen.local\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0]").value("Campo 'nome' é obrigatório."));

        verifyNoInteractions(usuarioService);
    }

    @Test
    @WithMockUser(roles = "LIDERANCA_TI")
    void deveResponder409ParaEmailRepetido() throws Exception {
        when(usuarioService.cadastrarAnalista(any(AnalistaRequest.class)))
            .thenThrow(new ConflitoException("Já existe um usuário com o e-mail 'ana.souza@lumen.local'."));

        mockMvc.perform(post("/api/v1/analistas").contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errors[0]").value("Já existe um usuário com o e-mail 'ana.souza@lumen.local'."));
    }

    @Test
    @WithMockUser(roles = "LIDERANCA_TI")
    void deveEditarDesativarEReativarComALideranca() throws Exception {
        when(usuarioService.editarAnalista(eq(ID), any(AnalistaRequest.class))).thenReturn(analista(true));
        when(usuarioService.desativarAnalista(ID)).thenReturn(analista(false));
        when(usuarioService.reativarAnalista(ID)).thenReturn(analista(true));

        mockMvc.perform(put("/api/v1/analistas/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
            .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/analistas/{id}/desativar", ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.ativo").value(false));
        mockMvc.perform(patch("/api/v1/analistas/{id}/reativar", ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.ativo").value(true));

        verify(usuarioService).desativarAnalista(ID);
    }

    @Test
    @WithMockUser(roles = "ANALISTA_SUPORTE")
    void deveResponder403QuandoAnalistaTentaDesativar() throws Exception {
        mockMvc.perform(patch("/api/v1/analistas/{id}/desativar", ID)).andExpect(status().isForbidden());

        verifyNoInteractions(usuarioService);
    }

    private static AnalistaResponse analista(boolean ativo) {
        return new AnalistaResponse(ID, "Ana Souza", "ana.souza@lumen.local", ativo,
            Instant.parse("2026-11-03T12:00:00Z"));
    }
}
