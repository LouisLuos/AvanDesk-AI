package com.avandesk.api.service;

import com.avandesk.api.entity.Usuario;
import com.avandesk.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Devolve o usuário autenticado, que é o autor de chamados e eventos.
 * Temporário até a HU09: hoje lê o login básico; com o JWT, este é o único ponto que muda.
 */
@Service
@RequiredArgsConstructor
public class UsuarioLogadoService {

    private final UsuarioRepository usuarioRepository;

    public Usuario obterUsuarioLogado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()
            || autenticacao instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("Nenhum usuário autenticado.");
        }

        return usuarioRepository.findByEmail(autenticacao.getName())
            .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));
    }
}
