package com.avandesk.api.usuario.controller;

import com.avandesk.api.core.config.SecurityConfig;
import com.avandesk.api.usuario.dto.LoginRequest;
import com.avandesk.api.usuario.dto.LoginResponse;
import com.avandesk.api.usuario.dto.UsuarioAutenticadoResponse;
import com.avandesk.api.usuario.entity.PerfilUsuario;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import com.avandesk.api.usuario.security.JwtAuthenticationFilter;
import com.avandesk.api.usuario.service.AuthService;
import com.avandesk.api.usuario.service.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração da camada Web/MVC para autenticação, cookies HttpOnly, /me, CSRF e logout (HU09, HU26, QA2-02, RNF-010).
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("HU26 / 200 OK: Deve logar emitindo JWT em cookie HttpOnly sem expor access_token no JSON")
    void deveAutenticarComSucessoERetornar200ComCookieHttpOnlyEUsuario() throws Exception {
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
            .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(allOf(
                containsString("nexo_access_token=token-jwt-simulado"),
                containsString("HttpOnly"),
                containsString("SameSite=Lax"),
                containsString("Path=/api")
            ))))
            .andExpect(cookie().exists("XSRF-TOKEN"))
            .andExpect(cookie().httpOnly("XSRF-TOKEN", false))
            .andExpect(cookie().path("XSRF-TOKEN", "/"))
            .andExpect(jsonPath("$.status_code").value(200))
            .andExpect(jsonPath("$.message").value("Autenticação realizada com sucesso"))
            .andExpect(jsonPath("$.data.access_token").doesNotExist())
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

    @Test
    @DisplayName("HU26 / 200 OK: Deve realizar logout expirando cookie HttpOnly com Max-Age=0")
    void deveRealizarLogoutERetornar200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isOk())
            .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(allOf(
                containsString("nexo_access_token="),
                containsString("Max-Age=0"),
                containsString("HttpOnly"),
                containsString("SameSite=Lax"),
                containsString("Path=/api")
            ))))
            .andExpect(jsonPath("$.status_code").value(200))
            .andExpect(jsonPath("$.message").value("Logout realizado com sucesso"));
    }

    @Test
    @DisplayName("HU26 / 200 OK: GET /auth/me deve retornar usuário autenticado via cookie HttpOnly")
    void deveRetornarUsuarioAtualQuandoAutenticadoViaCookie() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setNome("Carlos Analista");
        usuario.setEmail("carlos@lumendiagnosticos.com.br");
        usuario.setPerfil(PerfilUsuario.ANALISTA_SUPORTE);
        usuario.setAtivo(true);

        String token = jwtService.gerarToken(usuario);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(authService.obterUsuarioAutenticado()).thenReturn(UsuarioAutenticadoResponse.de(usuario));

        mockMvc.perform(get("/api/v1/auth/me")
                .cookie(new Cookie(JwtAuthenticationFilter.COOKIE_ACCESS_TOKEN, token)))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("XSRF-TOKEN"))
            .andExpect(cookie().httpOnly("XSRF-TOKEN", false))
            .andExpect(cookie().path("XSRF-TOKEN", "/"))
            .andExpect(jsonPath("$.status_code").value(200))
            .andExpect(jsonPath("$.data.id").value(usuarioId.toString()))
            .andExpect(jsonPath("$.data.email").value("carlos@lumendiagnosticos.com.br"))
            .andExpect(jsonPath("$.data.perfil").value("ANALISTA_SUPORTE"));
    }

    @Test
    @DisplayName("HU26 / 401 Unauthorized: GET /auth/me deve rejeitar requisição sem autenticação")
    void deveRetornar401EmMeSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status_code").value(401));
    }

    @Test
    @DisplayName("HU26 / 403 Forbidden: Mutação POST autenticada via cookie HttpOnly sem token CSRF deve ser bloqueada")
    void deveBloquearMutacaoViaCookieSemTokenCsrf() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setNome("Carlos Analista");
        usuario.setEmail("carlos@lumendiagnosticos.com.br");
        usuario.setPerfil(PerfilUsuario.ANALISTA_SUPORTE);
        usuario.setAtivo(true);

        String token = jwtService.gerarToken(usuario);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        mockMvc.perform(post("/api/v1/chamados")
                .cookie(new Cookie(JwtAuthenticationFilter.COOKIE_ACCESS_TOKEN, token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status_code").value(403));
    }

    @Test
    @DisplayName("HU26: Primeira mutação com cookie HttpOnly e X-XSRF-TOKEN válido deve passar pela validação CSRF")
    void devePermitirPrimeiraMutacaoQuandoCookieEHeaderCsrfForemEnviados() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setNome("Carlos Analista");
        usuario.setEmail("carlos@lumendiagnosticos.com.br");
        usuario.setPerfil(PerfilUsuario.ANALISTA_SUPORTE);
        usuario.setAtivo(true);

        String token = jwtService.gerarToken(usuario);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(authService.autenticar(any(LoginRequest.class)))
            .thenReturn(new LoginResponse("token-jwt-simulado", UsuarioAutenticadoResponse.de(usuario)));

        Cookie xsrfCookie = mockMvc.perform(get("/api/v1/auth/me")
                .cookie(new Cookie(JwtAuthenticationFilter.COOKIE_ACCESS_TOKEN, token)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "carlos@lumendiagnosticos.com.br",
                      "senha": "senha-segura-123",
                      "manter_conectado": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(allOf(
                containsString("nexo_access_token=token-jwt-simulado"),
                containsString("Max-Age=604800")
            ))));

        mockMvc.perform(post("/api/v1/chamados")
                .cookie(new Cookie(JwtAuthenticationFilter.COOKIE_ACCESS_TOKEN, token), xsrfCookie)
                .header("X-XSRF-TOKEN", xsrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isNotFound());
    }
}
