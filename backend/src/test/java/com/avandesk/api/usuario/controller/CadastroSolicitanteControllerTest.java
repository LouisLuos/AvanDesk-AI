package com.avandesk.api.usuario.controller;

import com.avandesk.api.core.config.SecurityConfig;
import com.avandesk.api.core.exception.ConflitoException;
import com.avandesk.api.core.exception.TokenVerificacaoInvalidoException;
import com.avandesk.api.usuario.dto.VerificacaoEmailResponse;
import com.avandesk.api.usuario.service.VerificacaoEmailService;
import com.avandesk.api.unidade.dto.UnidadeResumoResponse;
import com.avandesk.api.usuario.dto.CadastroSolicitanteRequest;
import com.avandesk.api.usuario.dto.CadastroSolicitanteResponse;
import com.avandesk.api.usuario.entity.StatusConta;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import com.avandesk.api.usuario.service.CadastroSolicitanteService;
import com.avandesk.api.usuario.service.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CadastroSolicitanteController.class)
@Import(SecurityConfig.class)
class CadastroSolicitanteControllerTest {

    private static final UUID UNIDADE_ID = UUID.randomUUID();
    private static final String CORPO_VALIDO = """
        {
          "nome": "Mariana Alves",
          "email": " Mariana.Alves@Gmail.com ",
          "unidade_id": "%s",
          "senha": "Coleta#2026ok"
        }
        """.formatted(UNIDADE_ID);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CadastroSolicitanteService cadastroSolicitanteService;

    @MockitoBean
    private VerificacaoEmailService verificacaoEmailService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void deveConfirmarOEmailSemLogin() throws Exception {
        when(verificacaoEmailService.verificar("abc_123")).thenReturn(
            new VerificacaoEmailResponse("mariana.alves@gmail.com", 2));

        mockMvc.perform(post("/api/v1/auth/verificar-email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"abc_123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value("mariana.alves@gmail.com"))
            .andExpect(jsonPath("$.data.chamados_vinculados").value(2));
    }

    @Test
    void deveResponder422ComOMotivoDoTokenRecusado() throws Exception {
        when(verificacaoEmailService.verificar("vencido")).thenThrow(new TokenVerificacaoInvalidoException(
            TokenVerificacaoInvalidoException.Motivo.TOKEN_EXPIRADO, "Este link expirou. Peça um novo link de verificação."));

        mockMvc.perform(post("/api/v1/auth/verificar-email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"vencido\"}"))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.message").value("Este link expirou. Peça um novo link de verificação."))
            .andExpect(jsonPath("$.errors[0]").value("TOKEN_EXPIRADO"));
    }

    @Test
    void deveResponder202ComAMesmaMensagemNoReenvio() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reenviar-verificacao").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \" Mariana.Alves@Gmail.com \"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.message")
                .value("Se houver uma conta pendente com este e-mail, enviamos um novo link de confirmação."));

        verify(verificacaoEmailService).reenviar("mariana.alves@gmail.com");
    }

    @Test
    void deveCriarAContaSemLoginNemCsrfERetornar201() throws Exception {
        Instant expiraEm = Instant.parse("2026-11-27T18:00:00Z");
        when(cadastroSolicitanteService.registrar(any(CadastroSolicitanteRequest.class))).thenReturn(
            new CadastroSolicitanteResponse("mariana.alves@gmail.com", StatusConta.PENDENTE_VERIFICACAO, expiraEm));

        mockMvc.perform(post("/api/v1/auth/registrar").contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.email").value("mariana.alves@gmail.com"))
            .andExpect(jsonPath("$.data.status_conta").value("PENDENTE_VERIFICACAO"))
            .andExpect(jsonPath("$.data.verificacao_expira_em").value("2026-11-27T18:00:00Z"));

        ArgumentCaptor<CadastroSolicitanteRequest> enviado = ArgumentCaptor.forClass(CadastroSolicitanteRequest.class);
        verify(cadastroSolicitanteService).registrar(enviado.capture());
        assertEquals("mariana.alves@gmail.com", enviado.getValue().email());
        assertEquals(UNIDADE_ID, enviado.getValue().unidadeId());
    }

    @Test
    void deveRecusarCadastroSemUnidadeEComEmailInvalido() throws Exception {
        String invalido = CORPO_VALIDO.replace(" Mariana.Alves@Gmail.com ", "mariana@gmail")
            .replaceFirst("\"unidade_id\": \"[^\"]+\",", "");

        mockMvc.perform(post("/api/v1/auth/registrar").contentType(MediaType.APPLICATION_JSON).content(invalido))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.length()").value(2));

        verifyNoInteractions(cadastroSolicitanteService);
    }

    @Test
    void deveResponder409ParaEmailJaCadastrado() throws Exception {
        when(cadastroSolicitanteService.registrar(any(CadastroSolicitanteRequest.class)))
            .thenThrow(new ConflitoException("Já existe uma conta com este e-mail."));

        mockMvc.perform(post("/api/v1/auth/registrar").contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errors[0]").value("Já existe uma conta com este e-mail."));
    }

    @Test
    void deveListarAsUnidadesSemLogin() throws Exception {
        when(cadastroSolicitanteService.unidadesParaCadastro())
            .thenReturn(List.of(new UnidadeResumoResponse(UNIDADE_ID, "U.03 Boa Viagem")));

        mockMvc.perform(get("/api/v1/auth/unidades"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(UNIDADE_ID.toString()))
            .andExpect(jsonPath("$.data[0].nome").value("U.03 Boa Viagem"));
    }
}
