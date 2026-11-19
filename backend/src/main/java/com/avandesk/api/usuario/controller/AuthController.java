package com.avandesk.api.usuario.controller;

import com.avandesk.api.core.dto.ApiResponse;
import com.avandesk.api.usuario.dto.LoginRequest;
import com.avandesk.api.usuario.dto.LoginResponse;
import com.avandesk.api.usuario.dto.UsuarioAutenticadoResponse;
import com.avandesk.api.usuario.security.JwtAuthenticationFilter;
import com.avandesk.api.usuario.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Controller de autenticação da plataforma Nexo (HU09 / HU26).
 * Gerencia emissão de cookie HttpOnly no login, consulta da sessão atual (/me) e expiração no logout.
 */
@RestController
@RequestMapping({"/api/v1/auth", "/auth"})
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de login e controle de sessão")
public class AuthController {

    private final AuthService authService;

    @Value("${nexo.jwt.expiration-ms:3600000}")
    private long expirationMs;

    @Value("${nexo.jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${nexo.jwt.cookie-same-site:Lax}")
    private String cookieSameSite;

    @Value("${nexo.jwt.cookie-path:/api}")
    private String cookiePath;

    @PostMapping("/login")
    @Operation(summary = "Autentica um usuário e emite o JWT em cookie HttpOnly")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.autenticar(request);
        ResponseCookie cookie = construirCookieSessao(response.accessToken(), Duration.ofMillis(expirationMs));
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(ApiResponse.sucesso(HttpStatus.OK, "Autenticação realizada com sucesso", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna os dados do usuário autenticado na sessão atual")
    public ResponseEntity<ApiResponse<UsuarioAutenticadoResponse>> me() {
        UsuarioAutenticadoResponse usuario = authService.obterUsuarioAutenticado();
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Sessão ativa recuperada com sucesso", usuario));
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerra a sessão ativa invalidando o cookie HttpOnly")
    public ResponseEntity<ApiResponse<Void>> logout() {
        SecurityContextHolder.clearContext();
        ResponseCookie cookie = construirCookieSessao("", Duration.ZERO);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(ApiResponse.sucesso(HttpStatus.OK, "Logout realizado com sucesso", null));
    }

    private ResponseCookie construirCookieSessao(String valor, Duration maxAge) {
        return ResponseCookie.from(JwtAuthenticationFilter.COOKIE_ACCESS_TOKEN, valor)
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite(cookieSameSite)
            .path(cookiePath)
            .maxAge(maxAge)
            .build();
    }
}
