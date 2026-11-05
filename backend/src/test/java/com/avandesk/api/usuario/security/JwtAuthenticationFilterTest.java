package com.avandesk.api.usuario.security;

import com.avandesk.api.usuario.entity.PerfilUsuario;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import com.avandesk.api.usuario.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para o JwtAuthenticationFilter (HU09, HU10, RNF-005).
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final UUID ID_ANALISTA = UUID.randomUUID();

    @Mock
    private JwtService jwtService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, usuarioRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("HU09: Deve autenticar requisição no SecurityContext quando houver Bearer token válido")
    void deveAutenticarRequisicaoQuandoBearerTokenEstiverValido() throws ServletException, IOException {
        MockHttpServletRequest request = requisicaoComToken("token-valido-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.isTokenValido("token-valido-123")).thenReturn(true);
        when(jwtService.obterUsuarioId("token-valido-123")).thenReturn(ID_ANALISTA);
        when(usuarioRepository.findById(ID_ANALISTA)).thenReturn(Optional.of(analista(true)));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
            .isEqualTo("analista@lumen.local");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_ANALISTA_SUPORTE");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("HU10: Não deve autenticar analista desativado, mesmo com token ainda válido")
    void deveIgnorarTokenDeAnalistaDesativado() throws ServletException, IOException {
        MockHttpServletRequest request = requisicaoComToken("token-de-desativado");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.isTokenValido("token-de-desativado")).thenReturn(true);
        when(jwtService.obterUsuarioId("token-de-desativado")).thenReturn(ID_ANALISTA);
        when(usuarioRepository.findById(ID_ANALISTA)).thenReturn(Optional.of(analista(false)));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("HU10: Deve usar o e-mail e o perfil atuais do banco, não os gravados no token")
    void deveUsarEmailDoBanco() throws ServletException, IOException {
        MockHttpServletRequest request = requisicaoComToken("token-antigo");
        Usuario editado = analista(true);
        editado.setEmail("analista.novo@lumen.local");

        when(jwtService.isTokenValido("token-antigo")).thenReturn(true);
        when(jwtService.obterUsuarioId("token-antigo")).thenReturn(ID_ANALISTA);
        when(usuarioRepository.findById(ID_ANALISTA)).thenReturn(Optional.of(editado));

        filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
            .isEqualTo("analista.novo@lumen.local");
    }

    @Test
    @DisplayName("HU09: Não deve alterar SecurityContext se o header Authorization estiver ausente")
    void deveIgnorarQuandoHeaderAuthorizationAusente() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("HU09: Não deve autenticar se o token estiver adulterado ou expirado")
    void deveIgnorarQuandoTokenForInvalido() throws ServletException, IOException {
        MockHttpServletRequest request = requisicaoComToken("token-expirado");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.isTokenValido("token-expirado")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(usuarioRepository);
        verify(filterChain).doFilter(request, response);
    }

    private static MockHttpServletRequest requisicaoComToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private static Usuario analista(boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setId(ID_ANALISTA);
        usuario.setEmail("analista@lumen.local");
        usuario.setPerfil(PerfilUsuario.ANALISTA_SUPORTE);
        usuario.setAtivo(ativo);
        return usuario;
    }
}
