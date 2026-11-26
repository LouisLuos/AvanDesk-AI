package com.avandesk.api.notificacao.service;

import com.avandesk.api.catalogo.repository.CategoriaRepository;
import com.avandesk.api.chamado.dto.ChamadoRequest;
import com.avandesk.api.chamado.dto.ChamadoResponse;
import com.avandesk.api.chamado.dto.SolicitanteRequest;
import com.avandesk.api.chamado.dto.StatusRequest;
import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.MotivoEspera;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.chamado.service.ChamadoService;
import com.avandesk.api.notificacao.entity.ModeloNotificacao;
import com.avandesk.api.notificacao.entity.NotificacaoEmail;
import com.avandesk.api.notificacao.entity.SituacaoNotificacao;
import com.avandesk.api.notificacao.port.MensagemEmail;
import com.avandesk.api.notificacao.port.NotificacaoPort;
import com.avandesk.api.notificacao.repository.NotificacaoEmailRepository;
import com.avandesk.api.unidade.entity.Unidade;
import com.avandesk.api.unidade.repository.UnidadeRepository;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@RecordApplicationEvents
@WithMockUser(username = "analista1@lumen.local")
class NotificacaoSolicitanteIntegracaoTest {

    @MockitoBean
    private NotificacaoPort notificacaoPort;

    @Autowired
    private ChamadoService chamadoService;

    @Autowired
    private EntregaNotificacaoService entregaNotificacaoService;

    @Autowired
    private ReenvioNotificacoesJob reenvioNotificacoesJob;

    @Autowired
    private NotificacaoEmailRepository notificacaoEmailRepository;

    @Autowired
    private UnidadeRepository unidadeRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ApplicationEvents eventosPublicados;

    private final String email = "solicitante.hu16." + UUID.randomUUID() + "@lumen.local";
    private Unidade unidade;
    private UUID categoriaId;
    private UUID analistaId;

    @BeforeEach
    void criarUnidade() {
        unidade = new Unidade();
        unidade.setNome("Unidade de Teste HU16");
        unidade.setIdentificacao("NT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        unidade = unidadeRepository.save(unidade);
        categoriaId = categoriaRepository.findByAtivoTrueOrderByNomeAsc().getFirst().getId();
        analistaId = usuarioRepository.findByEmail("analista1@lumen.local").orElseThrow().getId();
    }

    @AfterEach
    void removerUnidade() {
        unidadeRepository.deleteById(unidade.getId());
    }

    @Test
    void deveEnfileirarAAberturaComOStatusFinalDoRegistro() {
        emTransacaoDesfeita(() -> {
            ChamadoResponse chamado = chamadoService.registrar(new ChamadoRequest(unidade.getId(), CanalOrigem.LIGACAO,
                new SolicitanteRequest("Recepção HU16", email, null),
                "Impressora de etiquetas da coleta não imprime.", categoriaId, null, null, true));

            simularPreCommit();

            NotificacaoEmail notificacao = unicaNotificacao(chamado.id());
            assertThat(notificacao.getModelo()).isEqualTo(ModeloNotificacao.ABERTURA);
            assertThat(notificacao.getStatusAnterior()).isNull();
            assertThat(notificacao.getStatusNovo()).isEqualTo(StatusChamado.EM_ATENDIMENTO);
            assertThat(notificacao.getSituacao()).isEqualTo(SituacaoNotificacao.PENDENTE);
            assertThat(notificacao.getTentativas()).isZero();
            assertThat(notificacao.getProximaTentativaEm()).isAfter(Instant.now().plus(Duration.ofMinutes(1)));
            assertThat(eventosPublicados.stream(NotificacaoEnfileirada.class))
                .extracting(NotificacaoEnfileirada::notificacaoId).containsExactly(notificacao.getId());
        });
    }

    @Test
    void deveEnfileirarUmaUnicaMudancaPorTransacaoComOStatusAnteriorDoBanco() {
        emTransacaoDesfeita(() -> {
            UUID id = inserirChamado("EM_TRIAGEM");
            chamadoService.alterarStatus(id, new StatusRequest(StatusChamado.EM_ATENDIMENTO, null, null, null));
            chamadoService.alterarStatus(id,
                new StatusRequest(StatusChamado.AGUARDANDO, MotivoEspera.SOLICITANTE, null, null));

            simularPreCommit();

            NotificacaoEmail notificacao = unicaNotificacao(id);
            assertThat(notificacao.getModelo()).isEqualTo(ModeloNotificacao.MUDANCA_STATUS);
            assertThat(notificacao.getStatusAnterior()).isEqualTo(StatusChamado.EM_TRIAGEM);
            assertThat(notificacao.getStatusNovo()).isEqualTo(StatusChamado.AGUARDANDO);
            assertThat(notificacao.getMotivoEspera()).isEqualTo(MotivoEspera.SOLICITANTE);
        });
    }

    @Test
    void naoDeveNotificarStatusForaDaLista() {
        emTransacaoDesfeita(() -> {
            UUID id = inserirChamado("ABERTO");
            chamadoService.alterarStatus(id, new StatusRequest(StatusChamado.CANCELADO, null,
                "Chamado aberto em duplicidade durante o teste da HU16.", null));

            simularPreCommit();

            assertThat(notificacaoEmailRepository.findByChamadoIdOrderByCriadaEmAsc(id)).isEmpty();
            assertThat(eventosPublicados.stream(NotificacaoEnfileirada.class)).isEmpty();
        });
    }

    @Test
    void deveManterOEmAtendimentoEAgendarNovaTentativaQuandoOSmtpFalha() {
        emTransacaoDesfeita(() -> {
            UUID id = inserirChamado("EM_TRIAGEM");
            chamadoService.alterarStatus(id, new StatusRequest(StatusChamado.EM_ATENDIMENTO, null, null, null));
            simularPreCommit();
            doThrow(new MailSendException("SMTP indisponível")).when(notificacaoPort).enviar(any());

            entregaNotificacaoService.enviarAgora(unicaNotificacao(id).getId());
            entityManager.flush();

            NotificacaoEmail notificacao = unicaNotificacao(id);
            assertThat(statusNoBanco(id)).isEqualTo("EM_ATENDIMENTO");
            assertThat(notificacao.getSituacao()).isEqualTo(SituacaoNotificacao.FALHOU);
            assertThat(notificacao.getTentativas()).isEqualTo(1);
            assertThat(notificacao.getUltimoErro()).contains("SMTP indisponível");
            assertThat(notificacao.getProximaTentativaEm())
                .isBetween(Instant.now().plusSeconds(50), Instant.now().plusSeconds(70));
            assertThat(contarNotificacoesEnviadasNoHistorico(id)).isZero();
        });
    }

    @Test
    void deveGravarNotificacaoEnviadaNoHistoricoQuandoOEnvioDaCerto() {
        emTransacaoDesfeita(() -> {
            UUID id = inserirChamado("EM_TRIAGEM");
            chamadoService.alterarStatus(id, new StatusRequest(StatusChamado.EM_ATENDIMENTO, null, null, null));
            simularPreCommit();
            UUID notificacaoId = unicaNotificacao(id).getId();

            entregaNotificacaoService.enviarAgora(notificacaoId);
            entityManager.flush();

            ArgumentCaptor<MensagemEmail> mensagem = ArgumentCaptor.forClass(MensagemEmail.class);
            verify(notificacaoPort).enviar(mensagem.capture());
            assertThat(mensagem.getValue().destinatario()).isEqualTo(email);
            NotificacaoEmail notificacao = unicaNotificacao(id);
            assertThat(notificacao.getSituacao()).isEqualTo(SituacaoNotificacao.ENVIADA);
            assertThat(notificacao.getEnviadaEm()).isNotNull();
            assertThat(notificacao.getProximaTentativaEm()).isNull();
            assertThat(jdbcTemplate.queryForList("""
                    SELECT dados->>'notificacao_id' FROM eventos_historico
                    WHERE chamado_id = ? AND tipo = 'NOTIFICACAO_ENVIADA' AND autor_id IS NULL""", String.class, id))
                .containsExactly(notificacaoId.toString());
        });
    }

    @Test
    void deveDesistirDepoisDaQuartaFalha() {
        emTransacaoDesfeita(() -> {
            UUID id = inserirChamado("EM_TRIAGEM");
            chamadoService.alterarStatus(id, new StatusRequest(StatusChamado.EM_ATENDIMENTO, null, null, null));
            simularPreCommit();
            UUID notificacaoId = unicaNotificacao(id).getId();
            doThrow(new MailSendException("SMTP indisponível")).when(notificacaoPort).enviar(any());

            for (int i = 0; i < 5; i++) {
                entregaNotificacaoService.enviarAgora(notificacaoId);
            }

            verify(notificacaoPort, times(4)).enviar(any());
            NotificacaoEmail notificacao = unicaNotificacao(id);
            assertThat(notificacao.getSituacao()).isEqualTo(SituacaoNotificacao.ESGOTADA);
            assertThat(notificacao.getTentativas()).isEqualTo(4);
            assertThat(notificacao.getProximaTentativaEm()).isNull();
        });
    }

    @Test
    void deveReenviarPelaRotinaSoDepoisQueAEsperaVence() {
        emTransacaoDesfeita(() -> {
            UUID id = inserirChamado("EM_TRIAGEM");
            chamadoService.alterarStatus(id, new StatusRequest(StatusChamado.EM_ATENDIMENTO, null, null, null));
            simularPreCommit();
            UUID notificacaoId = unicaNotificacao(id).getId();
            doThrow(new MailSendException("SMTP indisponível")).doNothing().when(notificacaoPort).enviar(any());
            entregaNotificacaoService.enviarAgora(notificacaoId);

            reenvioNotificacoesJob.reenviarVencidas();
            verify(notificacaoPort, times(1)).enviar(any());

            entityManager.flush();
            jdbcTemplate.update("UPDATE notificacoes_email SET proxima_tentativa_em = now() - interval '1 minute' WHERE id = ?",
                notificacaoId);
            entityManager.clear();
            reenvioNotificacoesJob.reenviarVencidas();

            verify(notificacaoPort, times(2)).enviar(any());
            assertThat(unicaNotificacao(id).getSituacao()).isEqualTo(SituacaoNotificacao.ENVIADA);
            assertThat(unicaNotificacao(id).getTentativas()).isEqualTo(2);
        });
    }

    private void simularPreCommit() {
        TransactionSynchronizationUtils.triggerBeforeCommit(false);
        entityManager.flush();
    }

    private NotificacaoEmail unicaNotificacao(UUID chamadoId) {
        List<NotificacaoEmail> notificacoes = notificacaoEmailRepository.findByChamadoIdOrderByCriadaEmAsc(chamadoId);
        assertThat(notificacoes).hasSize(1);
        return notificacoes.getFirst();
    }

    private UUID inserirChamado(String status) {
        UUID solicitanteId = jdbcTemplate.queryForObject(
            "INSERT INTO solicitantes (nome, email) VALUES ('Solicitante HU16', ?) RETURNING id", UUID.class, email);
        return jdbcTemplate.queryForObject("""
                INSERT INTO chamados (codigo, unidade_id, solicitante_id, canal_origem, descricao, categoria_id, status,
                                      criado_por_id, responsavel_id, prioridade_sugerida, prioridade_final, origem_dado)
                VALUES (?, ?, ?, 'LIGACAO', 'Chamado do teste de notificações da HU16.', ?, ?, ?, ?, 'MEDIA', 'MEDIA',
                        'TESTE')
                RETURNING id""", UUID.class,
            "NT-2026-%06d".formatted(ThreadLocalRandom.current().nextInt(1_000_000)), unidade.getId(), solicitanteId,
            categoriaId, status, analistaId, analistaId);
    }

    private String statusNoBanco(UUID chamadoId) {
        return jdbcTemplate.queryForObject("SELECT status FROM chamados WHERE id = ?", String.class, chamadoId);
    }

    private long contarNotificacoesEnviadasNoHistorico(UUID chamadoId) {
        return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM eventos_historico WHERE chamado_id = ? AND tipo = 'NOTIFICACAO_ENVIADA'",
            Long.class, chamadoId);
    }

    private void emTransacaoDesfeita(Runnable bloco) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            status.setRollbackOnly();
            bloco.run();
        });
    }
}
