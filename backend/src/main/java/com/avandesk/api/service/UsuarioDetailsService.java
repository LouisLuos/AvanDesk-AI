package com.avandesk.api.service;

import com.avandesk.api.entity.Usuario;
import com.avandesk.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Carrega o usuário do login básico temporário pela tabela usuarios (até a HU09).
 * O "username" do login é o e-mail.
 */
@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));

        return User.withUsername(usuario.getEmail())
            .password(usuario.getSenhaHash())
            .roles(usuario.getPerfil().name())
            .disabled(!usuario.isAtivo())
            .build();
    }
}
