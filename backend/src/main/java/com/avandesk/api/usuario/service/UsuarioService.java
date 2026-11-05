package com.avandesk.api.usuario.service;

import com.avandesk.api.core.exception.ConflitoException;
import com.avandesk.api.core.exception.OperacaoNaoPermitidaException;
import com.avandesk.api.core.exception.RecursoNaoEncontradoException;
import com.avandesk.api.core.exception.RegraNegocioException;
import com.avandesk.api.usuario.config.UsuarioProperties;
import com.avandesk.api.usuario.dto.AnalistaCadastradoResponse;
import com.avandesk.api.usuario.dto.AnalistaRequest;
import com.avandesk.api.usuario.dto.AnalistaResponse;
import com.avandesk.api.usuario.entity.PerfilUsuario;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final GeradorSenhaTemporaria geradorSenhaTemporaria;
    private final UsuarioProperties usuarioProperties;

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

    // ---------- Gestão de analistas (HU10) ----------

    @Transactional(readOnly = true)
    public List<AnalistaResponse> listarAnalistas(Boolean ativo) {
        List<Usuario> analistas = ativo == null
            ? usuarioRepository.findByPerfilOrderByNomeAsc(PerfilUsuario.ANALISTA_SUPORTE)
            : usuarioRepository.findByPerfilAndAtivoOrderByNomeAsc(PerfilUsuario.ANALISTA_SUPORTE, ativo);
        return analistas.stream().map(AnalistaResponse::de).toList();
    }

    @Transactional
    public AnalistaCadastradoResponse cadastrarAnalista(AnalistaRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw emailDuplicado(email);
        }
        String senhaTemporaria = geradorSenhaTemporaria.gerar();

        Usuario analista = new Usuario();
        analista.setNome(request.nome().trim());
        analista.setEmail(email);
        analista.setSenhaHash(passwordEncoder.encode(senhaTemporaria));
        analista.setPerfil(PerfilUsuario.ANALISTA_SUPORTE);
        usuarioRepository.save(analista);

        return new AnalistaCadastradoResponse(AnalistaResponse.de(analista), senhaTemporaria);
    }

    @Transactional
    public AnalistaResponse editarAnalista(UUID id, AnalistaRequest request) {
        Usuario analista = buscarAnalista(id);
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailAndIdNot(email, id)) {
            throw emailDuplicado(email);
        }

        analista.setNome(request.nome().trim());
        analista.setEmail(email);
        return AnalistaResponse.de(analista);
    }

    /** Desativação lógica: o histórico de autoria continua apontando para o analista. */
    @Transactional
    public AnalistaResponse desativarAnalista(UUID id) {
        Usuario analista = buscarAnalista(id);
        analista.setAtivo(false);
        return AnalistaResponse.de(analista);
    }

    @Transactional
    public AnalistaResponse reativarAnalista(UUID id) {
        Usuario analista = buscarAnalista(id);
        analista.setAtivo(true);
        return AnalistaResponse.de(analista);
    }

    private Usuario buscarAnalista(UUID id) {
        return usuarioRepository.findById(id)
            .filter(usuario -> usuario.getPerfil() == PerfilUsuario.ANALISTA_SUPORTE)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Analista não encontrado."));
    }

    /** E-mail em minúsculas (constraint ck_usuarios_email_minusculo) e de um domínio corporativo aceito. */
    private String normalizarEmail(String email) {
        String normalizado = email.trim().toLowerCase(Locale.ROOT);
        String dominio = normalizado.substring(normalizado.indexOf('@') + 1);
        boolean corporativo = usuarioProperties.dominiosEmail().stream()
            .anyMatch(aceito -> aceito.trim().equalsIgnoreCase(dominio));
        if (!corporativo) {
            throw new RegraNegocioException("Use um e-mail corporativo (@%s)."
                .formatted(String.join(", @", usuarioProperties.dominiosEmail())));
        }
        return normalizado;
    }

    private static ConflitoException emailDuplicado(String email) {
        return new ConflitoException("Já existe um usuário com o e-mail '%s'.".formatted(email));
    }
}
