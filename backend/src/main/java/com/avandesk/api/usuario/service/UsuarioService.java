package com.avandesk.api.usuario.service;

import com.avandesk.api.core.exception.OperacaoNaoPermitidaException;
import com.avandesk.api.core.exception.RecursoNaoEncontradoException;
import com.avandesk.api.usuario.dto.UsuarioAutenticadoResponse;
import com.avandesk.api.usuario.entity.PerfilUsuario;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public Usuario obterAnalistaAtivo(UUID id) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        if (usuario.getPerfil() != PerfilUsuario.ANALISTA_SUPORTE) {
            throw new OperacaoNaoPermitidaException("Só analistas de suporte podem ser responsáveis por chamados.");
        }
        if (!usuario.isAtivo()) {
            throw new OperacaoNaoPermitidaException("Analista inativo não pode receber chamados.");
        }
        return usuario;
    }

    @Transactional(readOnly = true)
    public List<UsuarioAutenticadoResponse> listarAnalistasAtivos() {
        return usuarioRepository.findByPerfilAndAtivoTrueOrderByNomeAsc(PerfilUsuario.ANALISTA_SUPORTE)
            .stream()
            .map(UsuarioAutenticadoResponse::de)
            .toList();
    }
}
