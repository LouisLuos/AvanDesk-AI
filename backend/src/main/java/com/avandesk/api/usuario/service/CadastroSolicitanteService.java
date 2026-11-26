package com.avandesk.api.usuario.service;

import com.avandesk.api.core.exception.ConflitoException;
import com.avandesk.api.core.exception.RegraNegocioException;
import com.avandesk.api.unidade.dto.UnidadeResumoResponse;
import com.avandesk.api.unidade.entity.Unidade;
import com.avandesk.api.unidade.repository.UnidadeRepository;
import com.avandesk.api.unidade.service.UnidadeService;
import com.avandesk.api.usuario.config.SolicitanteProperties;
import com.avandesk.api.usuario.dto.CadastroSolicitanteRequest;
import com.avandesk.api.usuario.dto.CadastroSolicitanteResponse;
import com.avandesk.api.usuario.entity.PerfilUsuario;
import com.avandesk.api.usuario.entity.StatusConta;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CadastroSolicitanteService {

    private final UsuarioRepository usuarioRepository;
    private final UnidadeService unidadeService;
    private final UnidadeRepository unidadeRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenVerificacaoService tokenVerificacaoService;
    private final SolicitanteProperties propriedades;

    @Transactional
    public CadastroSolicitanteResponse registrar(CadastroSolicitanteRequest request) {
        if (!propriedades.aceitaDominioDe(request.email())) {
            throw new RegraNegocioException("Use o seu e-mail corporativo (%s) para criar a conta."
                .formatted(String.join(", ", propriedades.dominiosPermitidos().stream().map(d -> "@" + d).toList())));
        }
        List<String> pendencias = PoliticaSenhaForte.pendencias(request.senha());
        if (!pendencias.isEmpty()) {
            throw new RegraNegocioException("A senha precisa " + String.join(", ", pendencias) + ".");
        }
        Unidade unidade = unidadeService.obterAtivaParaNovoChamado(request.unidadeId());
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ConflitoException(
                "Já existe uma conta com este e-mail. Entre com ela ou peça um novo link de verificação.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setPerfil(PerfilUsuario.SOLICITANTE);
        usuario.setStatusConta(StatusConta.PENDENTE_VERIFICACAO);
        usuario.setUnidade(unidade);
        usuarioRepository.save(usuario);

        Instant expiraEm = tokenVerificacaoService.emitir(usuario);
        return new CadastroSolicitanteResponse(usuario.getEmail(), usuario.getStatusConta(), expiraEm);
    }

    @Transactional(readOnly = true)
    public List<UnidadeResumoResponse> unidadesParaCadastro() {
        return unidadeRepository.findByAtivoOrderByNomeAsc(true).stream()
            .map(unidade -> new UnidadeResumoResponse(unidade.getId(), unidade.getNome()))
            .toList();
    }
}
