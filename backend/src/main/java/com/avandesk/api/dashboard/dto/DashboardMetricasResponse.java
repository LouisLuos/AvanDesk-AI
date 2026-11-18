package com.avandesk.api.dashboard.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Contrato completo do Painel da Administração (HU08 + Seção 9.7).
 * Serializado em snake_case pelo Jackson conforme application.yml.
 */
public record DashboardMetricasResponse(
    PeriodoAnalisadoDto periodo,
    KpisPrincipaisDto kpis,
    IndicadoresProcessoDto indicadoresProcesso,
    AlertasProcessoDto alertas,
    List<ChamadosPorUnidadeDto> chamadosPorUnidade,
    List<SlaPorPrioridadeDto> slaPorPrioridade,
    List<ProblemaRecorrenteDto> problemasRecorrentes,
    List<TempoTerceirosFornecedorDto> tempoEmTerceiros,
    List<VolumeItemDto> volumePorCategoria,
    List<VolumeItemDto> volumePorEquipamento
) {

    public record PeriodoAnalisadoDto(
        Instant inicio,
        Instant fim,
        Instant inicioPeriodoAnterior,
        Instant fimPeriodoAnterior,
        UUID unidadeIdFiltro
    ) {}

    /** Os 4 cards principais do topo do Painel da Administração. */
    public record KpisPrincipaisDto(
        KpiChamadosAbertosDto chamadosAbertos,
        KpiTempoResolucaoDto tempoResolucao,
        KpiMetaPercentualDto chamadosRastreaveis,
        KpiMetaPercentualDto dentroDaMetaSla
    ) {}

    public record KpiChamadosAbertosDto(
        long totalAtual,
        long totalPeriodoAnterior,
        Double variacaoPercentual
    ) {}

    public record KpiTempoResolucaoDto(
        double medianoSegundos,
        double medioSegundos,
        Double diferencaMedianaSegundosAnterior,
        Double diferencaMediaSegundosAnterior
    ) {}

    public record KpiMetaPercentualDto(
        double percentual,
        double metaInicialPercentual,
        boolean dentroDaMeta,
        long totalAvaliados,
        long totalConformes
    ) {}

    /** Indicadores operacionais da Seção 9.7 (TT, TPR, TR, M11, M08, M12). */
    public record IndicadoresProcessoDto(
        double ttMedioSegundos,
        double tprMedioSegundos,
        double trMedioSegundos,
        double m11TempoRegistroMedioSegundos,
        double m08PercentualDivergenciaPrioridade,
        long m12IncidentesPrivacidade
    ) {}

    /** Alertas visuais automáticos da Seção 9.7. */
    public record AlertasProcessoDto(
        boolean alertaDivergenciaPrioridadeM08,
        boolean alertaTempoRegistroM11,
        boolean alertaCriticoPrivacidadeM12
    ) {}

    /** Gráfico de barras horizontais: volume total e % na meta por unidade. */
    public record ChamadosPorUnidadeDto(
        UUID unidadeId,
        String unidadeNome,
        String unidadeIdentificacao,
        long totalChamados,
        long chamadosResolvidos,
        double percentualDentroDaMeta,
        double tempoMedioAtendimentoSegundos
    ) {}

    /** Card "SLA por prioridade" (Alta P1, Média P2, Baixa P3). */
    public record SlaPorPrioridadeDto(
        String prioridade,
        String rotulo,
        long metaSegundos,
        long totalAvaliados,
        long totalDentroDaMeta,
        double percentualDentroDaMeta,
        boolean abaixoDaMetaInicial
    ) {}

    /** Tabela "Problemas recorrentes". */
    public record ProblemaRecorrenteDto(
        String problema,
        long ocorrencias,
        Instant ultimaOcorrencia,
        List<String> unidadesMaisAfetadas
    ) {}

    /** Card "Tempo em terceiros" (retorno médio por fornecedor externo - M05). */
    public record TempoTerceirosFornecedorDto(
        String fornecedor,
        long totalEscalonamentos,
        long escalonamentosConcluidos,
        double tempoMedioRetornoSegundos
    ) {}

    /** Gráficos complementares de volume por categoria e sistema/equipamento. */
    public record VolumeItemDto(
        UUID id,
        String nome,
        long totalChamados
    ) {}
}
