package com.avandesk.api.dashboard.controller;

import com.avandesk.api.core.config.SecurityConfig;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.AlertasProcessoDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.ChamadosPorResponsavelDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.ChamadosPorUnidadeDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.IndicadoresProcessoDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.KpiChamadosAbertosDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.KpiMetaPercentualDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.KpiTempoResolucaoDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.KpisPrincipaisDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.PeriodoAnalisadoDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.ProblemaRecorrenteDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.SlaPorPrioridadeDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.TempoTerceirosFornecedorDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.VolumeItemDto;
import com.avandesk.api.dashboard.service.DashboardService;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import com.avandesk.api.usuario.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Controle de acesso (RN06/T08.3) e contrato HTTP do Painel da Administração (HU08). */
@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void deveRecusarSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/metricas"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(dashboardService);
    }

    @Test
    @WithMockUser(roles = "ANALISTA_SUPORTE")
    void deveResponder403QuandoAnalistaTentaAcessarPainel() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/metricas"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Acesso negado para o perfil do usuário."));

        verifyNoInteractions(dashboardService);
    }

    @Test
    @WithMockUser(roles = "LIDERANCA_TI")
    void deveRetornarMetricasEmSnakeCaseParaLiderancaTi() throws Exception {
        UUID unidadeId = UUID.randomUUID();
        Instant inicio = Instant.parse("2026-10-19T00:00:00Z");
        Instant fim = Instant.parse("2026-11-18T00:00:00Z");

        when(dashboardService.consultarMetricas(any(), any(), any())).thenReturn(respostaExemplo(inicio, fim, unidadeId));

        mockMvc.perform(get("/api/v1/dashboard/metricas")
                .param("inicio", inicio.toString())
                .param("fim", fim.toString())
                .param("unidade_id", unidadeId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status_code").value(200))
            .andExpect(jsonPath("$.data.kpis.chamados_abertos.total_atual").value(182))
            .andExpect(jsonPath("$.data.kpis.chamados_abertos.variacao_percentual").value(8.0))
            .andExpect(jsonPath("$.data.kpis.tempo_resolucao.mediano_segundos").value(10080.0))
            .andExpect(jsonPath("$.data.kpis.tempo_resolucao.p90_segundos").value(16200.0))
            .andExpect(jsonPath("$.data.kpis.chamados_rastreaveis.percentual").value(91.0))
            .andExpect(jsonPath("$.data.kpis.dentro_da_meta_sla.percentual").value(84.0))
            .andExpect(jsonPath("$.data.indicadores_processo.m06_taxa_reabertura_percentual").value(5.0))
            .andExpect(jsonPath("$.data.indicadores_processo.m07_total_duplicados").value(3))
            .andExpect(jsonPath("$.data.alertas.alerta_divergencia_prioridade_m08").value(false))
            .andExpect(jsonPath("$.data.alertas.alerta_tempo_registro_m11").value(false))
            .andExpect(jsonPath("$.data.alertas.alerta_critico_privacidade_m12").value(true))
            .andExpect(jsonPath("$.data.chamados_por_unidade[0].unidade_nome").value("U.03 Boa Viagem"))
            .andExpect(jsonPath("$.data.chamados_por_responsavel[0].responsavel_nome").value("Analista 01"))
            .andExpect(jsonPath("$.data.sla_por_prioridade[0].rotulo").value("Alta (P1)"))
            .andExpect(jsonPath("$.data.problemas_recorrentes[0].problema").value("Lentidão no agendamento"))
            .andExpect(jsonPath("$.data.tempo_em_terceiros[0].fornecedor").value("Fornecedor de Agendamento Ltda."));

        verify(dashboardService).consultarMetricas(inicio, fim, unidadeId);
    }

    private static DashboardMetricasResponse respostaExemplo(Instant inicio, Instant fim, UUID unidadeId) {
        return new DashboardMetricasResponse(
            new PeriodoAnalisadoDto(inicio, fim, inicio.minusSeconds(2592000), inicio, unidadeId),
            new KpisPrincipaisDto(
                new KpiChamadosAbertosDto(182L, 168L, 8.0),
                new KpiTempoResolucaoDto(10080.0, 9800.0, 16200.0, -720.0, -600.0),
                new KpiMetaPercentualDto(91.0, 90.0, true, 100L, 91L),
                new KpiMetaPercentualDto(84.0, 80.0, true, 100L, 84L)
            ),
            new IndicadoresProcessoDto(10800.0, 480.0, 9800.0, 16200.0, 5.0, 1.65, 3L, 45.0, 18.0, 1L),
            new AlertasProcessoDto(false, false, true),
            List.of(new ChamadosPorUnidadeDto(unidadeId, "U.03 Boa Viagem", "LUM-BVG", 52L, 48L, 78.0, 5400.0)),
            List.of(new ChamadosPorResponsavelDto(UUID.randomUUID(), "Analista 01", 8L, 2L)),
            List.of(new SlaPorPrioridadeDto("ALTA", "Alta (P1)", 7200L, 32L, 25L, 78.0, true)),
            List.of(new ProblemaRecorrenteDto("Lentidão no agendamento", 6L, fim, List.of("U.07 Recife Centro", "U.03 Boa Viagem"))),
            List.of(new TempoTerceirosFornecedorDto("Fornecedor de Agendamento Ltda.", 4L, 4L, 183600.0)),
            List.of(new VolumeItemDto(UUID.randomUUID(), "Sistema hospitalar", 80L)),
            List.of(new VolumeItemDto(UUID.randomUUID(), "Impressora de etiquetas", 40L))
        );
    }
}
