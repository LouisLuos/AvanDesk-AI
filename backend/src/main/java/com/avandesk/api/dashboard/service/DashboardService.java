package com.avandesk.api.dashboard.service;

import com.avandesk.api.chamado.entity.OrigemDado;
import com.avandesk.api.core.config.NexoProperties;
import com.avandesk.api.core.exception.RegraNegocioException;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.AlertasProcessoDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.IndicadoresProcessoDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.KpiChamadosAbertosDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.KpiMetaPercentualDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.KpiTempoResolucaoDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.KpisPrincipaisDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.PeriodoAnalisadoDto;
import com.avandesk.api.dashboard.repository.DashboardRepository;
import com.avandesk.api.dashboard.repository.DashboardRepository.ResumoIndicadoresRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    public static final double META_INICIAL_RASTREABILIDADE_M01 = 90.0;
    public static final double META_INICIAL_SLA_M04 = 80.0;
    public static final double LIMIAR_ALERTA_DIVERGENCIA_M08 = 30.0;
    public static final double LIMIAR_ALERTA_TEMPO_TELA_M11_SEGUNDOS = 60.0;
    private static final Duration JANELA_PADRAO = Duration.ofDays(30);

    private final DashboardRepository dashboardRepository;
    private final NexoProperties nexoProperties;

    @Transactional(readOnly = true)
    public DashboardMetricasResponse consultarMetricas(Instant inicio, Instant fim, UUID unidadeId) {
        Instant fimEfetivo = (fim != null ? fim : Instant.now()).truncatedTo(ChronoUnit.MICROS);
        Instant inicioEfetivo = (inicio != null ? inicio : fimEfetivo.minus(JANELA_PADRAO))
            .truncatedTo(ChronoUnit.MICROS);

        if (inicioEfetivo.isAfter(fimEfetivo)) {
            throw new RegraNegocioException("A data inicial do filtro não pode ser posterior à data final.");
        }

        Duration duracaoPeriodo = Duration.between(inicioEfetivo, fimEfetivo);
        if (duracaoPeriodo.isZero()) {
            duracaoPeriodo = Duration.ofDays(1);
        }
        Instant fimPeriodoAnterior = inicioEfetivo;
        Instant inicioPeriodoAnterior = inicioEfetivo.minus(duracaoPeriodo);

        OrigemDado origemDado = nexoProperties.origemDado();

        ResumoIndicadoresRow resumo = dashboardRepository.buscarResumoIndicadores(
            inicioEfetivo, fimEfetivo, inicioPeriodoAnterior, fimPeriodoAnterior, unidadeId, origemDado
        );

        KpisPrincipaisDto kpis = montarKpisPrincipais(resumo);

        IndicadoresProcessoDto indicadores97 = new IndicadoresProcessoDto(
            arredondar(resumo.ttMedioSegundos()),
            arredondar(resumo.tprMedioSegundos()),
            arredondar(resumo.trMedioAtualSegundos()),
            arredondar(resumo.trP90AtualSegundos()),
            arredondar(resumo.m06TaxaReabertura()),
            arredondar(resumo.m07TaxaDuplicidade()),
            resumo.m07TotalDuplicados(),
            arredondar(resumo.m11MedioSegundos()),
            arredondar(resumo.m08PercentualDivergencia()),
            resumo.m12IncidentesPrivacidade()
        );

        AlertasProcessoDto alertas = new AlertasProcessoDto(
            indicadores97.m08PercentualDivergenciaPrioridade() > LIMIAR_ALERTA_DIVERGENCIA_M08,
            indicadores97.m11TempoRegistroMedioSegundos() > LIMIAR_ALERTA_TEMPO_TELA_M11_SEGUNDOS,
            indicadores97.m12IncidentesPrivacidade() > 0
        );

        return new DashboardMetricasResponse(
            new PeriodoAnalisadoDto(inicioEfetivo, fimEfetivo, inicioPeriodoAnterior, fimPeriodoAnterior, unidadeId),
            kpis,
            indicadores97,
            alertas,
            dashboardRepository.buscarChamadosPorUnidade(inicioEfetivo, fimEfetivo, origemDado),
            dashboardRepository.buscarChamadosAbertosPorResponsavel(inicioEfetivo, fimEfetivo, unidadeId, origemDado),
            dashboardRepository.buscarSlaPorPrioridade(inicioEfetivo, fimEfetivo, unidadeId, origemDado),
            dashboardRepository.buscarProblemasRecorrentes(inicioEfetivo, fimEfetivo, unidadeId, origemDado),
            dashboardRepository.buscarTempoEmTerceiros(inicioEfetivo, fimEfetivo, unidadeId, origemDado),
            dashboardRepository.buscarVolumePorCategoria(inicioEfetivo, fimEfetivo, unidadeId, origemDado),
            dashboardRepository.buscarVolumePorEquipamento(inicioEfetivo, fimEfetivo, unidadeId, origemDado)
        );
    }

    private static KpisPrincipaisDto montarKpisPrincipais(ResumoIndicadoresRow r) {
        Double variacaoChamados = r.totalAnterior() > 0
            ? arredondar(((r.totalAtual() - r.totalAnterior()) * 100.0) / r.totalAnterior())
            : null;

        boolean temBaseResolucaoComparativa = r.resolvidosAtual() > 0 && r.resolvidosAnterior() > 0;
        Double diferencaMediana = temBaseResolucaoComparativa
            ? arredondar(r.trMedianoAtualSegundos() - r.trMedianoAnteriorSegundos())
            : null;
        Double diferencaMedia = temBaseResolucaoComparativa
            ? arredondar(r.trMedioAtualSegundos() - r.trMedioAnteriorSegundos())
            : null;

        double pctRastreaveis = calcularPercentual(r.rastreaveisConformes(), r.rastreaveisAvaliados());
        double pctSla = calcularPercentual(r.slaConformes(), r.slaAvaliados());

        return new KpisPrincipaisDto(
            new KpiChamadosAbertosDto(r.totalAtual(), r.totalAnterior(), variacaoChamados),
            new KpiTempoResolucaoDto(
                arredondar(r.trMedianoAtualSegundos()),
                arredondar(r.trMedioAtualSegundos()),
                arredondar(r.trP90AtualSegundos()),
                diferencaMediana,
                diferencaMedia
            ),
            new KpiMetaPercentualDto(
                pctRastreaveis,
                META_INICIAL_RASTREABILIDADE_M01,
                r.rastreaveisAvaliados() > 0 && pctRastreaveis >= META_INICIAL_RASTREABILIDADE_M01,
                r.rastreaveisAvaliados(),
                r.rastreaveisConformes()
            ),
            new KpiMetaPercentualDto(
                pctSla,
                META_INICIAL_SLA_M04,
                r.slaAvaliados() > 0 && pctSla >= META_INICIAL_SLA_M04,
                r.slaAvaliados(),
                r.slaConformes()
            )
        );
    }

    private static double calcularPercentual(long conformes, long avaliados) {
        if (avaliados == 0) {
            return 0.0;
        }
        return arredondar((conformes * 100.0) / avaliados);
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
