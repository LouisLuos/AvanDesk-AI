package com.avandesk.api.dashboard.service;

import com.avandesk.api.chamado.entity.OrigemDado;
import com.avandesk.api.core.config.NexoProperties;
import com.avandesk.api.core.exception.RegraNegocioException;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.ChamadosPorUnidadeDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.ProblemaRecorrenteDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.SlaPorPrioridadeDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.TempoTerceirosFornecedorDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.VolumeItemDto;
import com.avandesk.api.dashboard.repository.DashboardRepository;
import com.avandesk.api.dashboard.repository.DashboardRepository.ResumoIndicadoresRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(dashboardRepository, new NexoProperties(OrigemDado.REAL));
    }

    @Test
    void deveCalcularKpisComparativosMetasEAlertasDaSecao97() {
        Instant inicio = Instant.parse("2026-10-01T00:00:00Z");
        Instant fim = Instant.parse("2026-10-31T00:00:00Z");
        UUID unidadeId = UUID.randomUUID();

        ResumoIndicadoresRow resumo = new ResumoIndicadoresRow(
            182L, 168L,
            100L, 90L,
            10080.0, 10800.0, // 2h48 vs 3h00 (-720s = 12 min mais rápido)
            9500.0, 10100.0,
            100L, 91L,        // 91% rastreáveis (meta >= 90%)
            100L, 84L,        // 84% dentro do SLA (meta >= 80%)
            11000.0,
            600.0,
            65.5,             // M11 > 60s -> alerta
            35.0,             // M08 > 30% -> alerta
            1L                // M12 > 0 -> alerta crítico
        );

        when(dashboardRepository.buscarResumoIndicadores(any(), any(), any(), any(), eq(unidadeId), eq(OrigemDado.REAL)))
            .thenReturn(resumo);
        when(dashboardRepository.buscarChamadosPorUnidade(inicio, fim, OrigemDado.REAL))
            .thenReturn(List.of(new ChamadosPorUnidadeDto(unidadeId, "U.03 Boa Viagem", "LUM-BVG", 52L, 48L, 78.0, 3200.0)));
        when(dashboardRepository.buscarSlaPorPrioridade(inicio, fim, unidadeId, OrigemDado.REAL))
            .thenReturn(List.of(new SlaPorPrioridadeDto("ALTA", "Alta (P1)", 7200L, 20L, 15L, 75.0, true)));
        when(dashboardRepository.buscarProblemasRecorrentes(inicio, fim, unidadeId, OrigemDado.REAL))
            .thenReturn(List.of(new ProblemaRecorrenteDto("Lentidão no agendamento", 6L, fim, List.of("U.07 Recife Centro"))));
        when(dashboardRepository.buscarTempoEmTerceiros(inicio, fim, unidadeId, OrigemDado.REAL))
            .thenReturn(List.of(new TempoTerceirosFornecedorDto("Fornecedor X", 4L, 3L, 183600.0)));
        when(dashboardRepository.buscarVolumePorCategoria(inicio, fim, unidadeId, OrigemDado.REAL))
            .thenReturn(List.of(new VolumeItemDto(UUID.randomUUID(), "Sistema hospitalar", 80L)));
        when(dashboardRepository.buscarVolumePorEquipamento(inicio, fim, unidadeId, OrigemDado.REAL))
            .thenReturn(List.of(new VolumeItemDto(UUID.randomUUID(), "Impressora de etiquetas", 40L)));

        DashboardMetricasResponse resposta = dashboardService.consultarMetricas(inicio, fim, unidadeId);

        assertEquals(182L, resposta.kpis().chamadosAbertos().totalAtual());
        assertEquals(8.33, resposta.kpis().chamadosAbertos().variacaoPercentual());
        assertEquals(10080.0, resposta.kpis().tempoResolucao().medianoSegundos());
        assertEquals(-720.0, resposta.kpis().tempoResolucao().diferencaMedianaSegundosAnterior());

        assertEquals(91.0, resposta.kpis().chamadosRastreaveis().percentual());
        assertTrue(resposta.kpis().chamadosRastreaveis().dentroDaMeta());
        assertEquals(84.0, resposta.kpis().dentroDaMetaSla().percentual());
        assertTrue(resposta.kpis().dentroDaMetaSla().dentroDaMeta());

        assertTrue(resposta.alertas().alertaDivergenciaPrioridadeM08());
        assertTrue(resposta.alertas().alertaTempoRegistroM11());
        assertTrue(resposta.alertas().alertaCriticoPrivacidadeM12());

        verify(dashboardRepository).buscarResumoIndicadores(
            eq(inicio),
            eq(fim),
            eq(Instant.parse("2026-09-01T00:00:00Z")),
            eq(inicio),
            eq(unidadeId),
            eq(OrigemDado.REAL)
        );
    }

    @Test
    void deveLidarComPeriodoSemHistoricoSemDivisaoPorZero() {
        Instant inicio = Instant.parse("2026-11-01T00:00:00Z");
        Instant fim = Instant.parse("2026-11-18T00:00:00Z");

        ResumoIndicadoresRow vazio = new ResumoIndicadoresRow(
            10L, 0L,
            5L, 0L,
            1800.0, 0.0,
            2000.0, 0.0,
            0L, 0L,
            0L, 0L,
            2000.0, 300.0, 42.0, 15.0, 0L
        );

        when(dashboardRepository.buscarResumoIndicadores(any(), any(), any(), any(), any(), eq(OrigemDado.REAL)))
            .thenReturn(vazio);

        DashboardMetricasResponse resposta = dashboardService.consultarMetricas(inicio, fim, null);

        assertNull(resposta.kpis().chamadosAbertos().variacaoPercentual());
        assertNull(resposta.kpis().tempoResolucao().diferencaMedianaSegundosAnterior());
        assertNull(resposta.kpis().tempoResolucao().diferencaMediaSegundosAnterior());
        assertFalse(resposta.kpis().chamadosRastreaveis().dentroDaMeta());
        assertFalse(resposta.kpis().dentroDaMetaSla().dentroDaMeta());
        assertFalse(resposta.alertas().alertaDivergenciaPrioridadeM08());
        assertFalse(resposta.alertas().alertaTempoRegistroM11());
        assertFalse(resposta.alertas().alertaCriticoPrivacidadeM12());
    }

    @Test
    void deveRejeitarPeriodoComInicioPosteriorAoFim() {
        Instant inicio = Instant.parse("2026-11-18T00:00:00Z");
        Instant fim = Instant.parse("2026-11-01T00:00:00Z");

        RegraNegocioException ex = assertThrows(
            RegraNegocioException.class,
            () -> dashboardService.consultarMetricas(inicio, fim, null)
        );

        assertEquals("A data inicial do filtro não pode ser posterior à data final.", ex.getMessage());
        verifyNoInteractions(dashboardRepository);
    }
}
