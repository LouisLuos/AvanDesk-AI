package com.avandesk.api.usuario.service;

import com.avandesk.api.usuario.dto.LoginRequest;
import com.avandesk.api.usuario.dto.LoginResponse;
import com.avandesk.api.usuario.dto.UsuarioAutenticadoResponse;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
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

        // Validação da senha contra hash BCrypt armazenado
        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new BadCredentialsException("E-mail ou senha inválidos.");
        }

        // Emissão do token JWT com claims e expiração de 1h
        String token = jwtService.gerarToken(usuario);

        return new LoginResponse(token, UsuarioAutenticadoResponse.de(usuario));
    }
}
