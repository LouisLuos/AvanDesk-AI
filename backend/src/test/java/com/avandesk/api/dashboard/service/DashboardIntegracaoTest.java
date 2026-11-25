package com.avandesk.api.dashboard.service;

import com.avandesk.api.catalogo.entity.Categoria;
import com.avandesk.api.catalogo.entity.SistemaEquipamento;
import com.avandesk.api.catalogo.repository.CategoriaRepository;
import com.avandesk.api.catalogo.repository.SistemaEquipamentoRepository;
import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.DirecaoContato;
import com.avandesk.api.chamado.entity.Escalonamento;
import com.avandesk.api.chamado.entity.EventoHistorico;
import com.avandesk.api.chamado.entity.OrigemDado;
import com.avandesk.api.chamado.entity.Prioridade;
import com.avandesk.api.chamado.entity.RegistroContato;
import com.avandesk.api.chamado.entity.Solicitante;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.chamado.entity.TipoEvento;
import com.avandesk.api.chamado.repository.ChamadoRepository;
import com.avandesk.api.chamado.repository.EscalonamentoRepository;
import com.avandesk.api.chamado.repository.EventoHistoricoRepository;
import com.avandesk.api.chamado.repository.RegistroContatoRepository;
import com.avandesk.api.chamado.repository.SolicitanteRepository;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.ChamadosPorUnidadeDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.SlaPorPrioridadeDto;
import com.avandesk.api.unidade.entity.Unidade;
import com.avandesk.api.unidade.repository.UnidadeRepository;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes integrados do Painel da Administração (HU08) no PostgreSQL real.
 * Roda em transação revertida no fim (setRollbackOnly) para manter o banco de desenvolvimento limpo.
 */
@SpringBootTest
@WithMockUser(roles = "LIDERANCA_TI")
class DashboardIntegracaoTest {

    private static final Instant AGORA = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private EventoHistoricoRepository eventoHistoricoRepository;

    @Autowired
    private RegistroContatoRepository registroContatoRepository;

    @Autowired
    private EscalonamentoRepository escalonamentoRepository;

    @Autowired
    private UnidadeRepository unidadeRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private SistemaEquipamentoRepository sistemaEquipamentoRepository;

    @Autowired
    private SolicitanteRepository solicitanteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void deveSegregarOrigemDadoDescontarPausaSolicitanteECalcularSlaEAlertas() {
        emTransacaoDesfeita(dados -> {
            Unidade unidadeAlvo = dados.unidade("U.99 Teste Painel");
            Unidade unidadeSemChamados = dados.unidade("U.98 Sem Chamados");

            Instant inicio = AGORA.minus(30, ChronoUnit.DAYS);
            Instant fim = AGORA;

            // 1. Chamado ALTA resolvido em 3h corridas, mas com 2h pausadas em SOLICITANTE => TR = 1h (DENTRO da meta de 2h)
            // Evento informativo (NOTIFICACAO_ENVIADA) no meio da pausa NÃO pode interromper a contagem do LEAD()
            Instant aberturaC1 = AGORA.minus(2, ChronoUnit.DAYS);
            Chamado c1 = dados.chamadoResolvido(
                unidadeAlvo, OrigemDado.TESTE, aberturaC1, aberturaC1.minusSeconds(90), // M11 = 90s (> 60s)
                aberturaC1.plusSeconds(300), aberturaC1.plus(3, ChronoUnit.HOURS),
                Prioridade.ALTA, Prioridade.ALTA, null,
                "Lentidão no agendamento", true, true
            );
            dados.evento(c1, TipoEvento.CHAMADO_AGUARDANDO, aberturaC1.plus(30, ChronoUnit.MINUTES), dados.analista1,
                Map.of("motivo_espera", "SOLICITANTE"));
            dados.evento(c1, TipoEvento.NOTIFICACAO_ENVIADA, aberturaC1.plus(60, ChronoUnit.MINUTES), dados.analista1,
                Map.of("canal", "EMAIL"));
            dados.evento(c1, TipoEvento.RETORNO_RECEBIDO, aberturaC1.plus(150, ChronoUnit.MINUTES), dados.analista1,
                Map.of("status_anterior", "AGUARDANDO"));
            dados.evento(c1, TipoEvento.CHAMADO_REABERTO, aberturaC1.plus(190, ChronoUnit.MINUTES), dados.analista1,
                Map.of("motivo", "Recorrência imediata"));

            // 2. Chamado ALTA escalonado a fornecedor por 2h (FORNECEDOR NÃO pausa SLA) => TT = TR = 3h (FORA da meta de 2h)
            // Também diverge da prioridade sugerida (MEDIA -> ALTA => 50% de divergência M08 > 30%)
            Instant aberturaC2 = AGORA.minus(1, ChronoUnit.DAYS);
            Chamado c2 = dados.chamadoResolvido(
                unidadeAlvo, OrigemDado.TESTE, aberturaC2, aberturaC2.minusSeconds(70), // M11 = 70s (> 60s)
                aberturaC2.plusSeconds(600), aberturaC2.plus(3, ChronoUnit.HOURS),
                Prioridade.MEDIA, Prioridade.ALTA, "Impacto crítico na recepção.",
                "Lentidão no agendamento", true, false // sem contato/evento -> não rastreável (1 de 2 = 50% < 90%)
            );
            dados.escalonamentoConcluido(c2, "Fornecedor Teste Ltda.", aberturaC2.plus(30, ChronoUnit.MINUTES),
                aberturaC2.plus(150, ChronoUnit.MINUTES));

            // 3. Chamado do período anterior (35 dias atrás) resolvido em 4h => mediana atual = 2h (7200s), anterior = 4h (14400s)
            Instant aberturaAnterior = AGORA.minus(35, ChronoUnit.DAYS);
            dados.chamadoResolvido(
                unidadeAlvo, OrigemDado.TESTE, aberturaAnterior, null,
                aberturaAnterior.plusSeconds(300), aberturaAnterior.plus(4, ChronoUnit.HOURS),
                Prioridade.BAIXA, Prioridade.BAIXA, null, null, false, true
            );

            // 4. Chamado SIMULADO no período atual deve ser completamente ignorado (nexo.origem-dado = TESTE)
            dados.chamadoResolvido(
                unidadeAlvo, OrigemDado.SIMULADO, AGORA.minus(3, ChronoUnit.HOURS), null,
                AGORA.minus(2, ChronoUnit.HOURS), AGORA.minus(1, ChronoUnit.HOURS),
                Prioridade.ALTA, Prioridade.ALTA, null, "Ruído simulado", true, true
            );

            // 5. Incidente M12: Liderança visualizou anexo
            dados.evento(c1, TipoEvento.ANEXO_VISUALIZADO, AGORA.minus(1, ChronoUnit.HOURS), dados.lideranca, Map.of());

            entityManager.flush();

            DashboardMetricasResponse resposta = dashboardService.consultarMetricas(inicio, fim, unidadeAlvo.getId());

            // KPI 1: 2 chamados TESTE no período atual vs 1 no anterior (+100%)
            assertEquals(2L, resposta.kpis().chamadosAbertos().totalAtual());
            assertEquals(1L, resposta.kpis().chamadosAbertos().totalPeriodoAnterior());
            assertEquals(100.0, resposta.kpis().chamadosAbertos().variacaoPercentual());

            // KPI 2: TR mediano de [3600s, 10800s] = 7200s vs anterior 14400s (-7200s = 2h mais rápido); P90 = 10080s
            assertEquals(7200.0, resposta.kpis().tempoResolucao().medianoSegundos());
            assertEquals(10080.0, resposta.kpis().tempoResolucao().p90Segundos());
            assertEquals(-7200.0, resposta.kpis().tempoResolucao().diferencaMedianaSegundosAnterior());

            // M06 (c1 teve evento CHAMADO_REABERTO entre 2 resolvidos => 50%) e M07 (0 mesclados)
            assertEquals(50.0, resposta.indicadoresProcesso().m06TaxaReaberturaPercentual());
            assertEquals(0L, resposta.indicadoresProcesso().m07TotalDuplicados());
            assertEquals(0.0, resposta.indicadoresProcesso().m07TaxaDuplicidadePercentual());

            // KPI 3: Rastreabilidade (c1 completo, c2 sem contato/evento => 50%)
            assertEquals(50.0, resposta.kpis().chamadosRastreaveis().percentual());
            assertFalse(resposta.kpis().chamadosRastreaveis().dentroDaMeta());

            // KPI 4: SLA (c1 dentro de 2h, c2 fora de 2h => 50%)
            assertEquals(50.0, resposta.kpis().dentroDaMetaSla().percentual());
            assertFalse(resposta.kpis().dentroDaMetaSla().dentroDaMeta());

            // Alertas Seção 9.7: M08 = 50% (>30%), M11 = 80s (>60s), M12 = 1 (>0)
            assertEquals(50.0, resposta.indicadoresProcesso().m08PercentualDivergenciaPrioridade());
            assertEquals(80.0, resposta.indicadoresProcesso().m11TempoRegistroMedioSegundos());
            assertEquals(1L, resposta.indicadoresProcesso().m12IncidentesPrivacidade());
            assertTrue(resposta.alertas().alertaDivergenciaPrioridadeM08());
            assertTrue(resposta.alertas().alertaTempoRegistroM11());
            assertTrue(resposta.alertas().alertaCriticoPrivacidadeM12());

            // SLA por prioridade (P1 = 50% e marcada abaixo da meta)
            SlaPorPrioridadeDto p1 = resposta.slaPorPrioridade().stream()
                .filter(p -> "ALTA".equals(p.prioridade())).findFirst().orElseThrow();
            assertEquals(2L, p1.totalAvaliados());
            assertEquals(1L, p1.totalDentroDaMeta());
            assertEquals(50.0, p1.percentualDentroDaMeta());
            assertTrue(p1.abaixoDaMetaInicial());

            // Problemas recorrentes e Tempo em terceiros
            assertEquals(1, resposta.problemasRecorrentes().size());
            assertEquals("Lentidão no agendamento", resposta.problemasRecorrentes().getFirst().problema());
            assertEquals(2L, resposta.problemasRecorrentes().getFirst().ocorrencias());

            assertEquals(1, resposta.tempoEmTerceiros().size());
            assertEquals("Fornecedor Teste Ltda.", resposta.tempoEmTerceiros().getFirst().fornecedor());
            assertEquals(7200.0, resposta.tempoEmTerceiros().getFirst().tempoMedioRetornoSegundos());

            // Comparativo de unidades deve trazer a unidade ativa mesmo com 0 chamados
            ChamadosPorUnidadeDto semChamadosDto = resposta.chamadosPorUnidade().stream()
                .filter(u -> u.unidadeId().equals(unidadeSemChamados.getId()))
                .findFirst()
                .orElse(null);
            assertNotNull(semChamadosDto);
            assertEquals(0L, semChamadosDto.totalChamados());
        });
    }

    @Test
    void deveCalcularM02PorResponsavelEM07Duplicidade() {
        emTransacaoDesfeita(dados -> {
            Unidade unidade = dados.unidade("U.97 Teste M02 M07");
            Instant inicio = AGORA.minus(7, ChronoUnit.DAYS);
            Instant fim = AGORA;

            Chamado principal = dados.chamadoEmAberto(unidade, dados.analista1, Prioridade.ALTA, StatusChamado.EM_ATENDIMENTO);
            dados.chamadoEmAberto(unidade, null, Prioridade.MEDIA, StatusChamado.ABERTO);
            dados.chamadoMesclado(unidade, principal);

            entityManager.flush();

            DashboardMetricasResponse resposta = dashboardService.consultarMetricas(inicio, fim, unidade.getId());

            // M07: 1 mesclado entre 3 chamados no período (33.33%)
            assertEquals(1L, resposta.indicadoresProcesso().m07TotalDuplicados());
            assertEquals(33.33, resposta.indicadoresProcesso().m07TaxaDuplicidadePercentual());

            // M02: 2 chamados em aberto (1 com analista1 P1, 1 sem responsável)
            assertEquals(2, resposta.chamadosPorResponsavel().size());
            var comAnalista = resposta.chamadosPorResponsavel().stream()
                .filter(r -> dados.analista1.getId().equals(r.responsavelId()))
                .findFirst()
                .orElseThrow();
            assertEquals(1L, comAnalista.totalEmAberto());
            assertEquals(1L, comAnalista.totalAltaPrioridade());

            var semResponsavel = resposta.chamadosPorResponsavel().stream()
                .filter(r -> r.responsavelId() == null)
                .findFirst()
                .orElseThrow();
            assertEquals("Sem responsável", semResponsavel.responsavelNome());
            assertEquals(1L, semResponsavel.totalEmAberto());
            assertEquals(0L, semResponsavel.totalAltaPrioridade());
        });
    }

    private void emTransacaoDesfeita(Consumer<Dados> teste) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            status.setRollbackOnly();
            teste.accept(new Dados());
        });
    }

    private final class Dados {

        private final Categoria categoria = categoriaRepository.findByAtivoTrueOrderByNomeAsc().getFirst();
        private final SistemaEquipamento sistema = sistemaEquipamentoRepository.findByAtivoTrueOrderByNomeAsc().getFirst();
        private final Usuario analista1 = usuarioRepository.findByEmail("analista1@lumen.local").orElseThrow();
        private final Usuario lideranca = usuarioRepository.findByEmail("lideranca@lumen.local").orElseThrow();
        private final Solicitante solicitante = criarSolicitante();
        private final int prefixo = ThreadLocalRandom.current().nextInt(100, 999);
        private int sequencia;

        Unidade unidade(String nome) {
            Unidade u = new Unidade();
            u.setNome(nome);
            u.setIdentificacao("DSH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            u.setAtivo(true);
            return unidadeRepository.save(u);
        }

        Chamado chamadoResolvido(
            Unidade unidade,
            OrigemDado origem,
            Instant abertoEm,
            Instant registroIniciadoEm,
            Instant primeiraRespostaEm,
            Instant resolvidoEm,
            Prioridade sugerida,
            Prioridade finalDefinida,
            String justificativa,
            String causaRaiz,
            boolean recorrente,
            boolean rastreavelCompleto
        ) {
            Chamado c = new Chamado();
            c.setCodigo("LUM-2098-%03d%03d".formatted(prefixo, ++sequencia));
            c.setUnidade(unidade);
            c.setSolicitante(solicitante);
            c.setCanalOrigem(CanalOrigem.LIGACAO);
            c.setDescricao("Descrição técnica válida para teste integrado do painel.");
            c.setCategoria(categoria);
            c.setSistemaEquipamento(sistema);
            c.setStatus(StatusChamado.RESOLVIDO);
            c.setCriadoPor(analista1);
            c.setResponsavel(analista1);
            c.setPrioridadeSugerida(sugerida);
            c.setPrioridadeFinal(finalDefinida);
            c.setJustificativaPrioridade(justificativa);
            c.setAbertoEm(abertoEm);
            c.setRegistroIniciadoEm(registroIniciadoEm);
            c.setPrimeiraRespostaEm(primeiraRespostaEm);
            c.setResolvidoEm(resolvidoEm);
            c.setResolucao("Problema corrigido com validação completa.");
            c.setCausaRaiz(causaRaiz);
            c.setPossivelRecorrencia(recorrente);
            c.setOrigemDado(origem);
            Chamado salvo = chamadoRepository.save(c);

            if (rastreavelCompleto) {
                RegistroContato contato = new RegistroContato();
                contato.setChamado(salvo);
                contato.setSolicitante(solicitante);
                contato.setCanal(CanalOrigem.LIGACAO);
                contato.setDirecao(DirecaoContato.RECEBIDO);
                contato.setConteudo("Solicitante relatou lentidão no atendimento.");
                contato.setOcorridoEm(abertoEm);
                contato.setRegistradoPor(analista1);
                contato.setOrigemDado(origem);
                registroContatoRepository.save(contato);

                evento(salvo, TipoEvento.CHAMADO_ABERTO, abertoEm, analista1, Map.of());
                evento(salvo, TipoEvento.CHAMADO_RESOLVIDO, resolvidoEm, analista1, Map.of("resolucao", salvo.getResolucao()));
            }
            return salvo;
        }

        Chamado chamadoEmAberto(Unidade unidade, Usuario responsavel, Prioridade prioridade, StatusChamado status) {
            Chamado c = new Chamado();
            c.setCodigo("LUM-2098-%03d%03d".formatted(prefixo, ++sequencia));
            c.setUnidade(unidade);
            c.setSolicitante(solicitante);
            c.setCanalOrigem(CanalOrigem.WHATSAPP);
            c.setDescricao("Chamado em aberto para validação de M02 por responsável.");
            c.setCategoria(categoria);
            c.setSistemaEquipamento(sistema);
            c.setStatus(status);
            c.setCriadoPor(analista1);
            c.setResponsavel(responsavel);
            c.setPrioridadeSugerida(prioridade);
            c.setPrioridadeFinal(prioridade);
            c.setAbertoEm(AGORA.minus(2, ChronoUnit.HOURS));
            c.setOrigemDado(OrigemDado.TESTE);
            return chamadoRepository.save(c);
        }

        Chamado chamadoMesclado(Unidade unidade, Chamado principal) {
            Chamado c = new Chamado();
            c.setCodigo("LUM-2098-%03d%03d".formatted(prefixo, ++sequencia));
            c.setUnidade(unidade);
            c.setSolicitante(solicitante);
            c.setCanalOrigem(CanalOrigem.EMAIL);
            c.setDescricao("Relato duplicado mesclado no chamado principal (M07).");
            c.setCategoria(categoria);
            c.setSistemaEquipamento(sistema);
            c.setStatus(StatusChamado.MESCLADO);
            c.setChamadoPrincipal(principal);
            c.setCriadoPor(analista1);
            c.setResponsavel(analista1);
            c.setAbertoEm(AGORA.minus(1, ChronoUnit.HOURS));
            c.setOrigemDado(OrigemDado.TESTE);
            return chamadoRepository.save(c);
        }

        void evento(Chamado chamado, TipoEvento tipo, Instant ocorridoEm, Usuario autor, Map<String, Object> dados) {
            eventoHistoricoRepository.save(
                new EventoHistorico(chamado, tipo, ocorridoEm, autor, dados, chamado.getOrigemDado())
            );
        }

        void escalonamentoConcluido(Chamado chamado, String fornecedor, Instant enviadoEm, Instant retornoEm) {
            Escalonamento esc = new Escalonamento();
            esc.setChamado(chamado);
            esc.setFornecedor(fornecedor);
            esc.setResponsavel(analista1);
            esc.setEnviadoPor(analista1);
            esc.setEnviadoEm(enviadoEm);
            esc.setRetorno("Ajuste executado pelo fornecedor.");
            esc.setRetornoEm(retornoEm);
            esc.setRetornoRegistradoPor(analista1);
            esc.setOrigemDado(chamado.getOrigemDado());
            escalonamentoRepository.save(esc);
        }

        private Solicitante criarSolicitante() {
            Solicitante s = new Solicitante();
            s.setNome("Solicitante Dashboard Teste");
            s.setTelefone("+5581911112222");
            return solicitanteRepository.save(s);
        }
    }
}
