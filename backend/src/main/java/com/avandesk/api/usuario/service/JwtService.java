package com.avandesk.api.usuario.service;

import com.avandesk.api.usuario.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Serviço responsável pela emissão, assinatura (HMAC-SHA256) e validação de tokens JWT (HU09).
 */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
        @Value("${nexo.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret,
        @Value("${nexo.jwt.expiration-ms:3600000}") long expirationMs
    ) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * Emite um token JWT stateless com claims de identificação, perfil e validade configurada (RNF-007).
     */
    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiracao = agora.plusMillis(expirationMs);

        return Jwts.builder()
            .subject(usuario.getId().toString())
            .claim("id", usuario.getId().toString())
            .claim("email", usuario.getEmail())
            .claim("role", usuario.getPerfil().name())
            .claim("nome", usuario.getNome())
            .issuedAt(Date.from(agora))
            .expiration(Date.from(expiracao))
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact();
    }

    /**
     * Extrai os claims assinados após verificar a integridade da assinatura criptográfica.
     */
    public Claims extrairClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Valida se a assinatura do token é íntegra e se a data de expiração não foi ultrapassada.
     */
    public boolean isTokenValido(String token) {
        try {
            Claims claims = extrairClaims(token);
            return claims.getExpiration() != null && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String obterEmail(String token) {
        return extrairClaims(token).get("email", String.class);
    }

    public String obterRole(String token) {
        return extrairClaims(token).get("role", String.class);
    }

    public String obterSubject(String token) {
        return extrairClaims(token).getSubject();
    }

    public UUID obterUsuarioId(String token) {
        String idStr = extrairClaims(token).get("id", String.class);
        return UUID.fromString(idStr != null ? idStr : extrairClaims(token).getSubject());
    }
}
