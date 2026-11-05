package com.avandesk.api.usuario.security;

import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import com.avandesk.api.usuario.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Filtro de autenticação JWT executado a cada requisição HTTP (HU09).
 * Intercepta o header 'Authorization: Bearer <token>', valida a assinatura e expiração,
 * e alimenta o SecurityContextHolder do Spring Security de forma stateless.
 *
 * O token só identifica o usuário: situação, e-mail e perfil vêm do banco a cada requisição,
 * para que a desativação de um analista (HU10) valha na hora, sem esperar o token expirar.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extrairToken(request);

        if (StringUtils.hasText(token) && jwtService.isTokenValido(token)
            && SecurityContextHolder.getContext().getAuthentication() == null) {
            buscarUsuarioAtivo(token).ifPresent(usuario -> {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    usuario.getEmail(), null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name())));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }

        filterChain.doFilter(request, response);
    }

    private Optional<Usuario> buscarUsuarioAtivo(String token) {
        try {
            return usuarioRepository.findById(jwtService.obterUsuarioId(token)).filter(Usuario::isAtivo);
        } catch (IllegalArgumentException tokenSemId) {
            return Optional.empty();
        }
    }

    private String extrairToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
