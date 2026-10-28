package com.avandesk.api.usuario.controller;

import com.avandesk.api.core.config.SecurityConfig;
import com.avandesk.api.usuario.dto.LoginRequest;
import com.avandesk.api.usuario.dto.LoginResponse;
import com.avandesk.api.usuario.dto.UsuarioAutenticadoResponse;
import com.avandesk.api.usuario.entity.PerfilUsuario;
import com.avandesk.api.usuario.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração da camada Web/MVC para autenticação e login (HU09, QA2-02, RNF-010).
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("HU09 / 200 OK: Deve logar com sucesso e devolver envelope ApiResponse com token e usuário")
    void deveAutenticarComSucessoERetornar200ComTokenEUsuario() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        UsuarioAutenticadoResponse usuarioResponse = new UsuarioAutenticadoResponse(
            usuarioId, "Carlos Analista", "carlos@lumendiagnosticos.com.br", PerfilUsuario.ANALISTA_SUPORTE);
        LoginResponse loginResponse = new LoginResponse("token-jwt-simulado", usuarioResponse);

        when(authService.autenticar(any(LoginRequest.class))).thenReturn(loginResponse);

        String payload = """
            {
              "email": "carlos@lumendiagnosticos.com.br",
              "senha": "senha-segura-123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status_code").value(200))
            .andExpect(jsonPath("$.message").value("Autenticação realizada com sucesso"))
            .andExpect(jsonPath("$.data.access_token").value("token-jwt-simulado"))
            .andExpect(jsonPath("$.data.usuario.id").value(usuarioId.toString()))
            .andExpect(jsonPath("$.data.usuario.email").value("carlos@lumendiagnosticos.com.br"))
            .andExpect(jsonPath("$.data.usuario.perfil").value("ANALISTA_SUPORTE"));
    }

    @Test
    @DisplayName("QA2-02 / 401 Unauthorized: Senha incorreta deve retornar 401 com mensagem genérica anti-enumeração")
    void deveRetornar401ComMensagemGenericaAoErrarSenha() throws Exception {
        when(authService.autenticar(any(LoginRequest.class)))
            .thenThrow(new BadCredentialsException("E-mail ou senha inválidos."));

        String payload = """
            {
              "email": "carlos@lumendiagnosticos.com.br",
              "senha": "senha-errada"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status_code").value(401))
            .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."));
    }

    @Test
    @DisplayName("QA2-02 / 401 Unauthorized: Usuário inativo deve receber 401 com mensagem genérica (RN06)")
    void deveRetornar401ComMensagemGenericaParaUsuarioInativo() throws Exception {
        when(authService.autenticar(any(LoginRequest.class)))
            .thenThrow(new BadCredentialsException("E-mail ou senha inválidos."));

        String payload = """
            {
              "email": "inativo@lumendiagnosticos.com.br",
              "senha": "qualquer-senha"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status_code").value(401))
            .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."));
    }

    @Test
    @DisplayName("HU09 / 400 Bad Request: Validação de campos obrigatórios (email em branco ou formato inválido)")
    void deveRetornar400QuandoCamposObrigatoriosEstiveremInvalidos() throws Exception {
        String payloadInvalido = """
            {
              "email": "email-invalido-sem-arroba",
              "senha": ""
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadInvalido))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status_code").value(400))
            .andExpect(jsonPath("$.message").value("Erro de validação"));
    }
}
