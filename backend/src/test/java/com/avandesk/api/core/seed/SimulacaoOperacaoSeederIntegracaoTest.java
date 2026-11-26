package com.avandesk.api.core.seed;

import com.avandesk.api.catalogo.repository.CategoriaRepository;
import com.avandesk.api.catalogo.repository.SistemaEquipamentoRepository;
import com.avandesk.api.chamado.entity.OrigemDado;
import com.avandesk.api.chamado.repository.ChamadoRepository;
import com.avandesk.api.chamado.repository.EscalonamentoRepository;
import com.avandesk.api.chamado.repository.EventoHistoricoRepository;
import com.avandesk.api.chamado.repository.RegistroContatoRepository;
import com.avandesk.api.chamado.repository.SolicitanteRepository;
import com.avandesk.api.dashboard.repository.DashboardRepository;
import com.avandesk.api.unidade.repository.UnidadeRepository;
import com.avandesk.api.usuario.entity.PerfilUsuario;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Valida o {@link SimulacaoOperacaoSeeder} (180 chamados, 14 unidades, 7 analistas, 5 fornecedores)
 * contra o PostgreSQL real dentro de uma transação revertida ao final, mantendo o banco local limpo.
 */
@SpringBootTest
class SimulacaoOperacaoSeederIntegracaoTest {

    @Autowired private ChamadoRepository chamadoRepository;
    @Autowired private RegistroContatoRepository registroContatoRepository;
    @Autowired private EventoHistoricoRepository eventoHistoricoRepository;
    @Autowired private EscalonamentoRepository escalonamentoRepository;
    @Autowired private SolicitanteRepository solicitanteRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private UnidadeRepository unidadeRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private SistemaEquipamentoRepository sistemaEquipamentoRepository;
    @Autowired private DashboardRepository dashboardRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void devePopular180ChamadosSimuladosComIdempotenciaEAlimentarTodasAsMetricasDoDashboard() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            status.setRollbackOnly();

            SimulacaoOperacaoSeeder seeder = new SimulacaoOperacaoSeeder(
                chamadoRepository,
                registroContatoRepository,
                eventoHistoricoRepository,
                escalonamentoRepository,
                solicitanteRepository,
                usuarioRepository,
                unidadeRepository,
                categoriaRepository,
                sistemaEquipamentoRepository,
                tx
            );

            // Escolhe um bucket de origem vazio para validar os 180 inserts reais mesmo se SIMULADO já tiver sido rodado localmente
            OrigemDado origemTeste = Arrays.stream(new OrigemDado[]{OrigemDado.SIMULADO, OrigemDado.TESTE, OrigemDado.REAL})
                .filter(o -> chamadoRepository.countByOrigemDado(o) == 0)
                .findFirst()
                .orElse(OrigemDado.SIMULADO);

            boolean bucketVazio = chamadoRepository.countByOrigemDado(origemTeste) == 0;
            int primeiraExecucao = seeder.popularSeNecessario(origemTeste);
            entityManager.flush();

            if (bucketVazio) {
                assertEquals(180, primeiraExecucao);
                assertEquals(180L, chamadoRepository.countByOrigemDado(origemTeste));
                assertTrue(unidadeRepository.findByAtivoOrderByNomeAsc(true).size() >= 14);
                assertTrue(usuarioRepository.findByPerfilAndAtivoOrderByNomeAsc(PerfilUsuario.ANALISTA_SUPORTE, true).size() >= 7);
            } else {
                assertEquals(0, primeiraExecucao);
            }

            // Idempotência garantida em qualquer cenário
            assertEquals(0, seeder.popularSeNecessario(origemTeste));

            Instant fim = Instant.now().truncatedTo(ChronoUnit.MICROS);
            Instant inicio = fim.minus(Duration.ofDays(30));
            Instant inicioAnterior = inicio.minus(Duration.ofDays(30));

            var resumo = dashboardRepository.buscarResumoIndicadores(
                inicio, fim, inicioAnterior, inicio, null, origemTeste
            );
            assertTrue(resumo.totalAtual() >= 100, "Esperava >= 100 chamados no período atual, veio " + resumo.totalAtual());
            assertTrue(resumo.totalAnterior() >= 68, "Esperava >= 68 chamados no período anterior, veio " + resumo.totalAnterior());
            assertTrue(resumo.slaAvaliados() >= 50);
            assertTrue(resumo.rastreaveisAvaliados() >= 50);
            assertTrue(resumo.m06TaxaReabertura() > 0.0);
            assertTrue(resumo.m07TotalDuplicados() >= 6);
            assertTrue(resumo.m08PercentualDivergencia() > 0.0);
            assertTrue(resumo.m11MedioSegundos() > 0.0);

            var porUnidade = dashboardRepository.buscarChamadosPorUnidade(inicio, fim, origemTeste);
            assertTrue(porUnidade.size() >= 14, "Esperava >= 14 unidades ativas no gráfico");

            var porResponsavel = dashboardRepository.buscarChamadosAbertosPorResponsavel(
                inicio, fim, null, origemTeste
            );
            assertTrue(porResponsavel.stream().anyMatch(r -> r.responsavelId() == null),
                "Esperava chamados sem responsável explícitos no gráfico M02");

            var slaPorPrioridade = dashboardRepository.buscarSlaPorPrioridade(
                inicio, fim, null, origemTeste
            );
            assertEquals(3, slaPorPrioridade.size());
            assertTrue(slaPorPrioridade.stream().allMatch(s -> s.totalAvaliados() >= 10));

            var recorrentes = dashboardRepository.buscarProblemasRecorrentes(
                inicio, fim, null, origemTeste
            );
            assertTrue(recorrentes.size() >= 6, "Esperava >= 6 problemas recorrentes, veio " + recorrentes.size());

            var terceiros = dashboardRepository.buscarTempoEmTerceiros(
                inicio, fim, null, origemTeste
            );
            assertEquals(5, terceiros.size(), "Esperava todos os 5 fornecedores no card M05");
        });
    }
}
