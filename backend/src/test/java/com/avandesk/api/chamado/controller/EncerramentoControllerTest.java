package com.avandesk.api.chamado.controller;

import com.avandesk.api.chamado.dto.ChamadoDetalheResponse;
import com.avandesk.api.chamado.dto.FechamentoRequest;
import com.avandesk.api.chamado.dto.ItemVerificacaoResponse;
import com.avandesk.api.chamado.dto.VerificacaoEncerramentoResponse;
import com.avandesk.api.chamado.service.EncerramentoService;
import com.avandesk.api.core.config.SecurityConfig;
import com.avandesk.api.core.exception.OperacaoNaoPermitidaException;
import com.avandesk.api.core.exception.RecursoNaoEncontradoException;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import com.avandesk.api.usuario.service.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EncerramentoController.class)
@Import(SecurityConfig.class)
class EncerramentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EncerramentoService encerramentoService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser
    void deveDevolverOChecklistEmSnakeCase() throws Exception {
        UUID id = UUID.randomUUID();
        when(encerramentoService.verificar(id)).thenReturn(VerificacaoEncerramentoResponse.de(List.of(
            new ItemVerificacaoResponse("PROBLEMA_E_CATEGORIA", "Problema e categoria descritos", true, null),
            new ItemVerificacaoResponse("RESPONSAVEL", "Responsável identificado", false,
                "Atribua um responsável ao chamado.")
        )));

        mockMvc.perform(get("/api/v1/chamados/{id}/verificacao-encerramento", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.aprovado").value(false))
            .andExpect(jsonPath("$.data.itens_conformes").value(1))
            .andExpect(jsonPath("$.data.total_itens").value(2))
            .andExpect(jsonPath("$.data.itens[0].ok").value(true))
            .andExpect(jsonPath("$.data.itens[0].pendencia").value(nullValue()))
            .andExpect(jsonPath("$.data.itens[1].codigo").value("RESPONSAVEL"))
            .andExpect(jsonPath("$.data.itens[1].pendencia").value("Atribua um responsável ao chamado."))
            .andExpect(jsonPath("$.data.pendencias").doesNotExist());
    }

    @Test
    @WithMockUser
    void deveResponder404ParaChamadoInexistente() throws Exception {
        UUID id = UUID.randomUUID();
        when(encerramentoService.verificar(id)).thenThrow(new RecursoNaoEncontradoException("Chamado não encontrado."));

        mockMvc.perform(get("/api/v1/chamados/{id}/verificacao-encerramento", id))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void deveEncerrarRepassandoAJustificativa() throws Exception {
        UUID id = UUID.randomUUID();
        when(encerramentoService.fechar(eq(id), any())).thenReturn(mock(ChamadoDetalheResponse.class));

        mockMvc.perform(post("/api/v1/chamados/{id}/fechar", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"justificativa_sem_causa_raiz\": \"Falha intermitente, sem causa identificada.\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Chamado encerrado"));

        ArgumentCaptor<FechamentoRequest> request = ArgumentCaptor.forClass(FechamentoRequest.class);
        verify(encerramentoService).fechar(eq(id), request.capture());
        assertEquals("Falha intermitente, sem causa identificada.", request.getValue().justificativaSemCausaRaiz());
    }

    @Test
    @WithMockUser
    void deveEncerrarSemCorpo() throws Exception {
        UUID id = UUID.randomUUID();
        when(encerramentoService.fechar(eq(id), isNull())).thenReturn(mock(ChamadoDetalheResponse.class));

        mockMvc.perform(post("/api/v1/chamados/{id}/fechar", id))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void deveResponder422ListandoAsPendencias() throws Exception {
        UUID id = UUID.randomUUID();
        when(encerramentoService.fechar(eq(id), any())).thenThrow(new OperacaoNaoPermitidaException(
            "O chamado tem pendências de rastreabilidade.",
            List.of("Atribua um responsável ao chamado.", "Registre ao menos um contato do solicitante.")));

        mockMvc.perform(post("/api/v1/chamados/{id}/fechar", id))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.errors[0]").value("Atribua um responsável ao chamado."))
            .andExpect(jsonPath("$.errors[1]").value("Registre ao menos um contato do solicitante."));
    }

    @Test
    @WithMockUser
    void deveRecusarJustificativaMaiorQue500Caracteres() throws Exception {
        mockMvc.perform(post("/api/v1/chamados/{id}/fechar", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"justificativa_sem_causa_raiz\": \"" + "x".repeat(501) + "\"}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(encerramentoService);
    }

    @Test
    void deveExigirLogin() throws Exception {
        mockMvc.perform(get("/api/v1/chamados/{id}/verificacao-encerramento", UUID.randomUUID()))
            .andExpect(status().isUnauthorized());
    }
}
