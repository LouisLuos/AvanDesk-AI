package com.avandesk.api.usuario.service;

import com.avandesk.api.usuario.config.SolicitanteProperties;
import com.avandesk.api.usuario.entity.TokenVerificacaoEmail;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.repository.TokenVerificacaoEmailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class TokenVerificacaoService {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int BYTES_DO_TOKEN = 32;

    private final TokenVerificacaoEmailRepository tokenRepository;
    private final ApplicationEventPublisher publicadorDeEventos;
    private final SolicitanteProperties propriedades;

    @Transactional(propagation = Propagation.MANDATORY)
    public Instant emitir(Usuario usuario) {
        Instant agora = Instant.now();
        tokenRepository.abertosDoUsuario(usuario.getId()).forEach(token -> token.revogar(agora));
        String token = novoToken();
        Instant expiraEm = agora.plus(propriedades.validadeTokenVerificacao());
        tokenRepository.save(new TokenVerificacaoEmail(usuario, hash(token), agora, expiraEm));
        publicadorDeEventos.publishEvent(
            new VerificacaoEmailSolicitada(usuario.getId(), usuario.getNome(), usuario.getEmail(), token, expiraEm));
        return expiraEm;
    }

    static String novoToken() {
        byte[] bytes = new byte[BYTES_DO_TOKEN];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String token) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException("SHA-256 indisponível na JVM.", erro);
        }
    }
}
