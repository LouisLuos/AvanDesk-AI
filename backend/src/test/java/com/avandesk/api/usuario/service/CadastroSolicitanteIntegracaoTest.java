package com.avandesk.api.usuario.service;

import com.avandesk.api.core.exception.ConflitoException;
import com.avandesk.api.core.exception.ContaNaoVerificadaException;
import com.avandesk.api.core.exception.RegraNegocioException;
import com.avandesk.api.notificacao.port.NotificacaoPort;
import com.avandesk.api.unidade.entity.Unidade;
import com.avandesk.api.unidade.repository.UnidadeRepository;
import com.avandesk.api.usuario.dto.CadastroSolicitanteRequest;
import com.avandesk.api.usuario.dto.CadastroSolicitanteResponse;
import com.avandesk.api.usuario.dto.LoginRequest;
import com.avandesk.api.usuario.entity.PerfilUsuario;
import com.avandesk.api.usuario.entity.StatusConta;
import com.avandesk.api.usuario.entity.TokenVerificacaoEmail;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.repository.TokenVerificacaoEmailRepository;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@RecordApplicationEvents
class CadastroSolicitanteIntegracaoTest {

    private static final String SENHA_FORTE = "Coleta#2026ok";

    @MockitoBean
    private NotificacaoPort notificacaoPort;

    @Autowired
    private CadastroSolicitanteService cadastroSolicitanteService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenVerificacaoEmailRepository tokenRepository;

    @Autowired
    private UnidadeRepository unidadeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ApplicationEvents eventosPublicados;

    private final String email = "solicitante.hu17." + UUID.randomUUID() + "@gmail.com";

    @Test
    void deveCriarAContaPendenteComTokenSoEmHashEPedirOEmail() {
        emTransacaoDesfeita(() -> {
            Unidade unidade = unidadeAtiva();

            CadastroSolicitanteResponse resposta = cadastroSolicitanteService.registrar(
                new CadastroSolicitanteRequest("  Mariana Alves ", email.toUpperCase(), unidade.getId(), SENHA_FORTE));
            entityManager.flush();

            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
            assertThat(resposta.email()).isEqualTo(email);
            assertThat(resposta.statusConta()).isEqualTo(StatusConta.PENDENTE_VERIFICACAO);
            assertThat(usuario.getNome()).isEqualTo("Mariana Alves");
            assertThat(usuario.getPerfil()).isEqualTo(PerfilUsuario.SOLICITANTE);
            assertThat(usuario.getUnidade().getId()).isEqualTo(unidade.getId());
            assertThat(usuario.getSenhaHash()).isNotEqualTo(SENHA_FORTE);
            assertThat(passwordEncoder.matches(SENHA_FORTE, usuario.getSenhaHash())).isTrue();
            assertThat(usuario.podeEntrar()).isFalse();

            VerificacaoEmailSolicitada pedido = eventosPublicados.stream(VerificacaoEmailSolicitada.class)
                .reduce((primeiro, segundo) -> segundo).orElseThrow();
            TokenVerificacaoEmail token = tokenRepository.findFirstByUsuarioIdOrderByCriadoEmDesc(usuario.getId())
                .orElseThrow();
            assertThat(pedido.email()).isEqualTo(email);
            assertThat(pedido.token()).hasSize(43).matches("[A-Za-z0-9_-]+");
            assertThat(token.getTokenHash()).isEqualTo(TokenVerificacaoService.hash(pedido.token()))
                .isNotEqualTo(pedido.token());
            assertThat(token.getExpiraEm()).isEqualTo(resposta.verificacaoExpiraEm())
                .isBetween(Instant.now().plus(Duration.ofHours(23)), Instant.now().plus(Duration.ofHours(25)));
            assertThat(pedido.toString()).doesNotContain(pedido.token());
        });
    }

    @Test
    void deveRecusarEmailJaCadastrado() {
        emTransacaoDesfeita(() -> {
            assertThatThrownBy(() -> cadastroSolicitanteService.registrar(
                new CadastroSolicitanteRequest("Outra Pessoa", "analista1@lumen.local", unidadeAtiva().getId(),
                    SENHA_FORTE)))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("Já existe uma conta com este e-mail");
        });
    }

    @Test
    void deveRecusarSenhaFracaComAsPendencias() {
        emTransacaoDesfeita(() -> assertThatThrownBy(() -> cadastroSolicitanteService.registrar(
            new CadastroSolicitanteRequest("Mariana Alves", email, unidadeAtiva().getId(), "coleta2026")))
            .isInstanceOf(RegraNegocioException.class)
            .hasMessage("A senha precisa ter uma letra maiúscula, ter um símbolo (ex.: ! @ # $ %)."));
    }

    @Test
    void deveRecusarUnidadeInativa() {
        emTransacaoDesfeita(() -> {
            Unidade inativa = unidadeAtiva();
            inativa.setAtivo(false);
            assertThatThrownBy(() -> cadastroSolicitanteService.registrar(
                new CadastroSolicitanteRequest("Mariana Alves", email, inativa.getId(), SENHA_FORTE)))
                .isInstanceOf(RegraNegocioException.class);
        });
    }

    @Test
    void deveBloquearOLoginDaContaPendenteSoDepoisDeConferirASenha() {
        emTransacaoDesfeita(() -> {
            cadastroSolicitanteService.registrar(
                new CadastroSolicitanteRequest("Mariana Alves", email, unidadeAtiva().getId(), SENHA_FORTE));

            assertThatThrownBy(() -> authService.autenticar(new LoginRequest(email, "Senha#Errada1")))
                .isInstanceOf(BadCredentialsException.class);
            assertThatThrownBy(() -> authService.autenticar(new LoginRequest(email, SENHA_FORTE)))
                .isInstanceOf(ContaNaoVerificadaException.class)
                .hasMessageContaining("Confirme o seu e-mail");
        });
    }

    @Test
    void deveListarSoAsUnidadesAtivasParaOCadastro() {
        emTransacaoDesfeita(() -> {
            Unidade inativa = new Unidade();
            inativa.setNome("Unidade Inativa HU17");
            inativa.setIdentificacao("CD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            inativa.setAtivo(false);
            unidadeRepository.save(inativa);

            assertThat(cadastroSolicitanteService.unidadesParaCadastro())
                .isNotEmpty()
                .noneMatch(unidade -> unidade.id().equals(inativa.getId()));
        });
    }

    private Unidade unidadeAtiva() {
        return unidadeRepository.findByAtivoOrderByNomeAsc(true).getFirst();
    }

    private void emTransacaoDesfeita(Runnable bloco) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            status.setRollbackOnly();
            bloco.run();
        });
    }
}
