package com.avandesk.api.core.config;

import com.avandesk.api.core.dto.ApiResponse;
import com.avandesk.api.usuario.entity.PerfilUsuario;
import com.avandesk.api.usuario.security.JwtAuthenticationFilter;
import com.avandesk.api.usuario.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Configuração de segurança da plataforma Nexo (HU09, HU26, RN06).
 * Arquitetura Stateless com autenticação via cookie HttpOnly (e fallback Bearer) e suporte a CORS/CSRF.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Import({JwtService.class, JwtAuthenticationFilter.class})
public class SecurityConfig {

    private static final List<String> ROTAS_PUBLICAS_DA_CONTA = List.of(
        "/api/v1/auth/registrar",
        "/api/v1/auth/unidades",
        "/api/v1/auth/verificar-email",
        "/api/v1/auth/reenviar-verificacao");

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${nexo.cors.allowed-origins:http://localhost:4200}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        RequestMatcher csrfMatcher = request -> {
            if (!CsrfFilter.DEFAULT_CSRF_MATCHER.matches(request)) {
                return false;
            }
            String uri = request.getRequestURI();
            if ("/api/v1/auth/login".equals(uri) || "/auth/login".equals(uri)
                || "/api/v1/auth/logout".equals(uri) || "/auth/logout".equals(uri)
                || ROTAS_PUBLICAS_DA_CONTA.contains(uri)) {
                return false;
            }
            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                return false;
            }
            return Arrays.stream(cookies)
                .anyMatch(c -> JwtAuthenticationFilter.COOKIE_ACCESS_TOKEN.equals(c.getName()));
        };

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .requireCsrfProtectionMatcher(csrfMatcher)
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> {
                ObjectMapper snakeCaseMapper = new ObjectMapper()
                    .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
                ex
                    .authenticationEntryPoint((request, response, authException) ->
                        escreverErroJson(response, snakeCaseMapper, HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos."))
                    .accessDeniedHandler((request, response, accessDeniedException) ->
                        escreverErroJson(response, snakeCaseMapper, HttpStatus.FORBIDDEN, "Acesso negado para o perfil do usuário."));
            })
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/health",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api-docs",
                    "/api-docs/**",
                    "/api-docs.yaml",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/api/v1/auth/login",
                    "/api/v1/auth/logout",
                    "/auth/login",
                    "/auth/logout"
                ).permitAll()
                .requestMatchers(ROTAS_PUBLICAS_DA_CONTA.toArray(String[]::new)).permitAll()
                .requestMatchers("/api/v1/auth/me", "/auth/me").authenticated()
                .anyRequest().hasAnyRole(PerfilUsuario.PERFIS_DA_EQUIPE)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void escreverErroJson(HttpServletResponse response, ObjectMapper mapper,
                                         HttpStatus status, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapper.writeValue(response.getOutputStream(), ApiResponse.erro(status, mensagem, null));
    }
}
