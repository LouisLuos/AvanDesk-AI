package com.avandesk.api.usuario.service;

import com.avandesk.api.catalogo.repository.CategoriaRepository;
import com.avandesk.api.chamado.dto.ChamadoRequest;
import com.avandesk.api.chamado.dto.SolicitanteRequest;
import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.Solicitante;
import com.avandesk.api.chamado.repository.SolicitanteRepository;
import com.avandesk.api.chamado.service.ChamadoService;
import com.avandesk.api.core.exception.TokenVerificacaoInvalidoException;
import com.avandesk.api.core.exception.TokenVerificacaoInvalidoException.Motivo;
import com.avandesk.api.notificacao.port.NotificacaoPort;
import com.avandesk.api.unidade.repository.UnidadeRepository;
import com.avandesk.api.usuario.dto.CadastroSolicitanteRequest;
import com.avandesk.api.usuario.dto.LoginRequest;
import com.avandesk.api.usuario.dto.VerificacaoEmailResponse;
import com.avandesk.api.usuario.entity.PerfilUsuario;
import com.avandesk.api.usuario.entity.StatusConta;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@RecordApplicationEvents
@WithMockUser(username = "analista1@lumen.local")
class VerificacaoEmailIntegracaoTest {

    private static final String SENHA_FORTE = "Coleta#2026ok";

    @MockitoBean
    private NotificacaoPort notificacaoPort;

    @Autowired
    private CadastroSolicitanteService cadastroSolicitanteService;

    @Autowired
    private VerificacaoEmailService verificacaoEmailService;

    @Autowired
    private AuthService authService;

    @Autowired
    private ChamadoService chamadoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SolicitanteRepository solicitanteRepository;

    @Autowired
    private UnidadeRepository unidadeRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ApplicationEvents eventosPublicados;

    private final String email = "verificacao.hu17." + UUID.randomUUID() + "@gmail.com";

    @Test
    void deveAtivarAContaEVincularOsChamadosAbertosAntesPelaEquipe() {
        emTransacaoDesfeita(() -> {
            UUID unidadeId = unidadeAtiva();
            registrarChamadoDaEquipe(unidadeId);
            registrarChamadoDaEquipe(unidadeId);
            criarConta(unidadeId);

            VerificacaoEmailResponse resposta = verificacaoEmailService.verificar(ultimoToken());
            entityManager.flush();
            entityManager.clear();

            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
            Solicitante solicitante = solicitanteRepository.findByEmail(email).orElseThrow();
            assertThat(resposta.email()).isEqualTo(email);
            assertThat(resposta.chamadosVinculados()).isEqualTo(2);
            assertThat(usuario.getStatusConta()).isEqualTo(StatusConta.ATIVA);
            assertThat(solicitante.getUsuario().getId()).isEqualTo(usuario.getId());
            assertThat(solicitante.getNome()).isEqualTo("Mariana Alves");
            assertThat(solicitante.getTelefone()).isEqualTo("+5581988887777");
            assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tokens_verificacao_email WHERE usuario_id = ? AND utilizado_em IS NOT NULL",
                Long.class, usuario.getId())).isEqualTo(1);
            assertThat(authService.autenticar(new LoginRequest(email, SENHA_FORTE)).usuario().perfil())
                .isEqualTo(PerfilUsuario.SOLICITANTE);
        });
    }

    @Test
    void deveCriarOSolicitanteQuandoAEquipeNuncaAbriuChamadoComOEmail() {
        emTransacaoDesfeita(() -> {
            criarConta(unidadeAtiva());

            VerificacaoEmailResponse resposta = verificacaoEmailService.verificar(ultimoToken());
            entityManager.flush();

            Solicitante solicitante = solicitanteRepository.findByEmail(email).orElseThrow();
            assertThat(resposta.chamadosVinculados()).isZero();
            assertThat(solicitante.getNome()).isEqualTo("Mariana Alves");
            assertThat(solicitante.getUsuario().getEmail()).isEqualTo(email);
        });
    }

    @Test
    void deveRecusarTokenJaUtilizado() {
        emTransacaoDesfeita(() -> {
            criarConta(unidadeAtiva());
            String token = ultimoToken();
            verificacaoEmailService.verificar(token);

            assertMotivo(() -> verificacaoEmailService.verificar(token), Motivo.TOKEN_UTILIZADO);
        });
    }

    @Test
    void deveRecusarTokenExpiradoEManterAContaPendente() {
        emTransacaoDesfeita(() -> {
            criarConta(unidadeAtiva());
            String token = ultimoToken();
            entityManager.flush();
            jdbcTemplate.update("""
                    UPDATE tokens_verificacao_email
                    SET criado_em = now() - interval '2 days', expira_em = now() - interval '1 day'
                    WHERE usuario_id = (SELECT id FROM usuarios WHERE email = ?)""", email);
            entityManager.clear();

            assertMotivo(() -> verificacaoEmailService.verificar(token), Motivo.TOKEN_EXPIRADO);
            assertThat(jdbcTemplate.queryForObject("SELECT status_conta FROM usuarios WHERE email = ?", String.class,
                email)).isEqualTo("PENDENTE_VERIFICACAO");
        });
    }

    @Test
    void deveRecusarTokenInexistente() {
        emTransacaoDesfeita(() ->
            assertMotivo(() -> verificacaoEmailService.verificar("token-que-nao-existe"), Motivo.TOKEN_INVALIDO));
    }

    @Test
    void deveRespeitarOIntervaloDoReenvioEInvalidarOLinkAnterior() {
        emTransacaoDesfeita(() -> {
            criarConta(unidadeAtiva());
            String primeiro = ultimoToken();

            verificacaoEmailService.reenviar(email);
            assertThat(eventosPublicados.stream(VerificacaoEmailSolicitada.class)).hasSize(1);

            entityManager.flush();
            jdbcTemplate.update("""
                    UPDATE tokens_verificacao_email SET criado_em = criado_em - interval '2 minutes'
                    WHERE usuario_id = (SELECT id FROM usuarios WHERE email = ?)""", email);
            entityManager.clear();
            verificacaoEmailService.reenviar(email);
            String segundo = ultimoToken();

            assertThat(eventosPublicados.stream(VerificacaoEmailSolicitada.class)).hasSize(2);
            assertThat(segundo).isNotEqualTo(primeiro);
            assertMotivo(() -> verificacaoEmailService.verificar(primeiro), Motivo.TOKEN_SUBSTITUIDO);
            assertThat(verificacaoEmailService.verificar(segundo).email()).isEqualTo(email);
        });
    }

    @Test
    void naoDeveReenviarParaContaAtivaNemParaEmailDesconhecido() {
        emTransacaoDesfeita(() -> {
            verificacaoEmailService.reenviar("analista1@lumen.local");
            verificacaoEmailService.reenviar("ninguem." + UUID.randomUUID() + "@gmail.com");

            assertThat(eventosPublicados.stream(VerificacaoEmailSolicitada.class)).isEmpty();
        });
    }

    private void criarConta(UUID unidadeId) {
        cadastroSolicitanteService.registrar(
            new CadastroSolicitanteRequest("Mariana Alves", email, unidadeId, SENHA_FORTE));
    }

    private void registrarChamadoDaEquipe(UUID unidadeId) {
        chamadoService.registrar(new ChamadoRequest(unidadeId, CanalOrigem.LIGACAO,
            new SolicitanteRequest(null, email, "81988887777"),
            "Impressora de etiquetas da coleta não imprime.",
            categoriaRepository.findByAtivoTrueOrderByNomeAsc().getFirst().getId(), null, null, false));
    }

    private String ultimoToken() {
        return eventosPublicados.stream(VerificacaoEmailSolicitada.class)
            .reduce((primeiro, segundo) -> segundo).orElseThrow().token();
    }

    private UUID unidadeAtiva() {
        return unidadeRepository.findByAtivoOrderByNomeAsc(true).getFirst().getId();
    }

    private static void assertMotivo(ThrowingCallable chamada, Motivo motivo) {
        assertThatThrownBy(chamada)
            .isInstanceOf(TokenVerificacaoInvalidoException.class)
            .extracting(erro -> ((TokenVerificacaoInvalidoException) erro).getMotivo())
            .isEqualTo(motivo);
    }

    private void emTransacaoDesfeita(Runnable bloco) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            status.setRollbackOnly();
            bloco.run();
        });
    }
}
