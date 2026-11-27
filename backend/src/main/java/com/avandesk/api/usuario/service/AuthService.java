package com.avandesk.api.usuario.service;

import com.avandesk.api.core.exception.ContaNaoVerificadaException;
import com.avandesk.api.usuario.dto.LoginRequest;
import com.avandesk.api.usuario.dto.LoginResponse;
import com.avandesk.api.usuario.dto.UsuarioAutenticadoResponse;
import com.avandesk.api.usuario.entity.StatusConta;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Serviço de autenticação da plataforma Nexo (HU09, RN06).
 * Valida credenciais contra hash BCrypt e emite tokens JWT stateless.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${nexo.jwt.expiration-ms:3600000}")
    private long expirationMs = 3600000L;

    @Value("${nexo.jwt.remember-me-expiration-ms:604800000}")
    private long rememberMeExpirationMs = 604800000L;

    @Transactional(readOnly = true)
    public LoginResponse autenticar(LoginRequest request) {
        String emailNormalizado = request.email().trim().toLowerCase(Locale.ROOT);

        // Busca o usuário pelo e-mail institucional
        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
            .orElseThrow(() -> new BadCredentialsException("E-mail ou senha inválidos."));

        // Bloqueia usuários inativos (RN06) com a mesma mensagem para evitar enumeração
        if (!usuario.isAtivo()) {
            throw new BadCredentialsException("E-mail ou senha inválidos.");
        }

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new BadCredentialsException("E-mail ou senha inválidos.");
        }
        if (usuario.getStatusConta() == StatusConta.PENDENTE_VERIFICACAO) {
            throw new ContaNaoVerificadaException(
                "Confirme o seu e-mail para entrar. Se o link expirou, peça um novo na tela de verificação.");
        }

        // Emissão do token JWT (1h padrão ou 7 dias com "Manter conectado")
        long validadeMs = Boolean.TRUE.equals(request.manterConectado()) ? rememberMeExpirationMs : expirationMs;
        String token = jwtService.gerarToken(usuario, validadeMs);

        return new LoginResponse(token, UsuarioAutenticadoResponse.de(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioAutenticadoResponse obterUsuarioAutenticado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()
            || autenticacao instanceof AnonymousAuthenticationToken) {
            throw new BadCredentialsException("Sessão inválida ou expirada.");
        }

        Usuario usuario = usuarioRepository.findByEmail(autenticacao.getName())
            .filter(Usuario::podeEntrar)
            .orElseThrow(() -> new BadCredentialsException("Sessão inválida ou expirada."));

        return UsuarioAutenticadoResponse.de(usuario);
    }
}
