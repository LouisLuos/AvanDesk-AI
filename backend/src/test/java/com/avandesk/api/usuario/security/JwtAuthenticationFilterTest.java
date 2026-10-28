package com.avandesk.api.usuario.security;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para o JwtAuthenticationFilter (HU09, RNF-005).
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("HU09: Deve autenticar requisição no SecurityContext quando houver Bearer token válido")
    void deveAutenticarRequisicaoQuandoBearerTokenEstiverValido() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-valido-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.isTokenValido("token-valido-123")).thenReturn(true);
        when(jwtService.obterEmail("token-valido-123")).thenReturn("analista@lumendiagnosticos.com.br");
        when(jwtService.obterRole("token-valido-123")).thenReturn("ANALISTA_SUPORTE");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
            .isEqualTo("analista@lumendiagnosticos.com.br");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_ANALISTA_SUPORTE");

        verify(filterChain).doFilter(request, response);
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
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-expirado");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.isTokenValido("token-expirado")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
