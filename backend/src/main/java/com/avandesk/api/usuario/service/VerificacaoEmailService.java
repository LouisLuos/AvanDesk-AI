package com.avandesk.api.usuario.service;

import com.avandesk.api.chamado.entity.Solicitante;
import com.avandesk.api.chamado.repository.ChamadoRepository;
import com.avandesk.api.chamado.repository.SolicitanteRepository;
import com.avandesk.api.core.exception.TokenVerificacaoInvalidoException;
import com.avandesk.api.core.exception.TokenVerificacaoInvalidoException.Motivo;
import com.avandesk.api.usuario.config.SolicitanteProperties;
import com.avandesk.api.usuario.dto.VerificacaoEmailResponse;
import com.avandesk.api.usuario.entity.PerfilUsuario;
import com.avandesk.api.usuario.entity.StatusConta;
import com.avandesk.api.usuario.entity.TokenVerificacaoEmail;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.repository.TokenVerificacaoEmailRepository;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class VerificacaoEmailService {

    private final TokenVerificacaoEmailRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final SolicitanteRepository solicitanteRepository;
    private final ChamadoRepository chamadoRepository;
    private final TokenVerificacaoService tokenVerificacaoService;
    private final SolicitanteProperties propriedades;

    @Transactional(noRollbackFor = TokenVerificacaoInvalidoException.class)
    public VerificacaoEmailResponse verificar(String token) {
        TokenVerificacaoEmail verificacao = tokenRepository.travarPorHash(TokenVerificacaoService.hash(token.trim()))
            .orElseThrow(() -> new TokenVerificacaoInvalidoException(Motivo.TOKEN_INVALIDO,
                "Link de verificação inválido. Confira se copiou o endereço inteiro ou peça um novo link."));
        Instant agora = Instant.now();
        if (verificacao.utilizado()) {
            throw new TokenVerificacaoInvalidoException(Motivo.TOKEN_UTILIZADO,
                "Este link já foi usado e a sua conta já está confirmada. É só entrar.");
        }
        if (verificacao.revogado()) {
            throw new TokenVerificacaoInvalidoException(Motivo.TOKEN_SUBSTITUIDO,
                "Este link foi substituído por um mais novo. Use o último e-mail que enviamos.");
        }
        if (verificacao.expiradoEm(agora)) {
            throw new TokenVerificacaoInvalidoException(Motivo.TOKEN_EXPIRADO,
                "Este link expirou. Peça um novo link de verificação.");
        }

        Usuario usuario = verificacao.getUsuario();
        verificacao.marcarUtilizado(agora);
        usuario.setStatusConta(StatusConta.ATIVA);
        Solicitante solicitante = vincularSolicitante(usuario);
        return new VerificacaoEmailResponse(usuario.getEmail(), chamadoRepository.countBySolicitanteId(solicitante.getId()));
    }

    @Transactional
    public void reenviar(String email) {
        usuarioRepository.findByEmail(email)
            .filter(usuario -> usuario.getPerfil() == PerfilUsuario.SOLICITANTE)
            .filter(usuario -> usuario.getStatusConta() == StatusConta.PENDENTE_VERIFICACAO)
            .filter(this::passouOIntervaloDeReenvio)
            .ifPresent(tokenVerificacaoService::emitir);
    }

    private boolean passouOIntervaloDeReenvio(Usuario usuario) {
        return tokenRepository.findFirstByUsuarioIdOrderByCriadoEmDesc(usuario.getId())
            .map(ultimo -> !ultimo.getCriadoEm().plus(propriedades.intervaloReenvio()).isAfter(Instant.now()))
            .orElse(true);
    }

    private Solicitante vincularSolicitante(Usuario usuario) {
        Solicitante solicitante = solicitanteRepository.findByEmail(usuario.getEmail()).orElseGet(() -> {
            Solicitante novo = new Solicitante();
            novo.setEmail(usuario.getEmail());
            return novo;
        });
        if (solicitante.getNome() == null) {
            solicitante.setNome(usuario.getNome());
        }
        solicitante.setUsuario(usuario);
        return solicitanteRepository.save(solicitante);
    }
}
