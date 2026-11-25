import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { catchError, finalize, of } from 'rxjs';
import { DashboardService } from '../../../core/services/dashboard.service';
import { ToastService } from '../../../core/services/toast.service';
import { UnidadeService } from '../../../core/services/unidade.service';
import {
  CustomDropdown,
  OpcaoDropdown,
} from '../../../shared/components/custom-dropdown/custom-dropdown';
import { InfoTooltip } from '../../../shared/components/info-tooltip/info-tooltip';
import { DashboardMetricas } from '../../../shared/models/dashboard.model';
import { Unidade } from '../../../shared/models/unidade.model';

type PeriodoDias = '7' | '30' | '90' | 'custom';

@Component({
  selector: 'app-painel-administracao',
  imports: [CustomDropdown, InfoTooltip],
  templateUrl: './painel-administracao.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PainelAdministracao implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly unidadeService = inject(UnidadeService);
  private readonly toast = inject(ToastService);

  protected readonly opcoesPeriodo: OpcaoDropdown[] = [
    { valor: '7', rotulo: 'Últimos 7 dias' },
    { valor: '30', rotulo: 'Últimos 30 dias' },
    { valor: '90', rotulo: 'Últimos 90 dias' },
    { valor: 'custom', rotulo: 'Período personalizado' },
  ];

  protected readonly tooltips = {
    kpiChamadosAbertos:
      'Total de chamados abertos na janela filtrada, comparado percentualmente ao período imediatamente anterior de mesma duração.',
    kpiTempoResolucao:
      'Mediana (P50) do tempo líquido de resolução — desconta pausas aguardando o solicitante e evita distorções por casos fora da curva.',
    kpiP90:
      'Percentil 90 do tempo líquido: indica o prazo máximo em que 90% dos chamados do período foram resolvidos.',
    kpiRastreaveis:
      'Percentual de chamados resolvidos ou fechados com unidade, responsável, descrição (>= 10 caracteres), resolução, contato registrado e evento de resolução (M01). Meta inicial: >= 90%.',
    kpiSla:
      'Percentual de chamados resolvidos dentro da meta da sua prioridade (P1: 2h, P2: 8h, P3: 24h), já descontadas as pausas por solicitante (M04). Meta inicial: >= 80%.',
    chamadosPorUnidade:
      'Compara todas as unidades ativas (inclusive as sem ocorrências) por volume de chamados, tempo médio líquido de atendimento e conformidade com a meta de SLA.',
    slaPorPrioridade:
      'Conformidade de SLA por faixa da Matriz 3D: Alta (P1 até 2h), Média (P2 até 8h) e Baixa (P3 até 24h). P1 abaixo de 80% indica revisão de capacidade.',
    problemasRecorrentes:
      'Top 10 causas raiz ou incidentes sinalizados como recorrentes no período, ordenados por volume de ocorrências e listando as 3 unidades mais afetadas.',
    tempoEmTerceiros:
      'Tempo médio entre envio e retorno de escalonamentos por fornecedor externo (M05). Conforme RN07, a espera em fornecedor não pausa o relógio de SLA global.',
    volumeCategoria:
      'Distribuição de chamados abertos no período por categoria ativa do catálogo de atendimento.',
    volumeEquipamento:
      'Ranking de sistemas e equipamentos vinculados aos chamados no período, destacando ativos mais críticos.',
    cargaResponsavel:
      'Fila de chamados ainda em andamento por analista (exclui resolvidos, fechados, cancelados e mesclados), evidenciando P1 e itens sem responsável (M02).',
    secao97:
      'Indicadores operacionais e de qualidade de processo da Seção 9.7 para acompanhar eficiência, triagem pela Matriz 3D, usabilidade do registro e conformidade LGPD.',
    tt: 'Tempo Total médio corrido entre a abertura e a resolução do chamado, sem descontar pausas de espera.',
    tpr: 'Tempo médio de Primeira Resposta entre a abertura do chamado e a primeira atuação registrada do analista.',
    tr: 'Tempo médio Líquido de Resolução: desconta pausas aguardando o solicitante, mantendo o tempo em fornecedor externo.',
    p90: 'Percentil 90 do tempo líquido de resolução: 90% dos chamados encerrados foram solucionados dentro deste prazo.',
    m06: 'Taxa de Reabertura: percentual de chamados resolvidos/fechados que voltaram a ser reabertos — mede efetividade da solução.',
    m07: 'Taxa de Duplicidade: percentual e volume de chamados identificados como duplicados e mesclados a um chamado principal.',
    m08: 'Divergência de Prioridade: percentual de chamados triados cuja prioridade final divergiu da Matriz 3D. Acima de 30% dispara alerta de recalibração.',
    m11: 'Tempo de Tela no Registro: média entre iniciar o formulário e concluir a abertura do chamado. Acima de 60s dispara alerta de atrito.',
    m12: 'Incidentes de Privacidade (RN06 / LGPD): contagem de visualizações indevidas de anexos. Qualquer ocorrência (> 0) gera alerta crítico.',
  } as const;

  protected readonly periodoSelecionado = signal<PeriodoDias>('30');
  protected readonly dataInicioCustom = signal<string>(this.formatarDataInput(this.diasAtras(30)));
  protected readonly dataFimCustom = signal<string>(this.formatarDataInput(new Date()));
  protected readonly unidadeSelecionada = signal<string>('');
  protected readonly unidades = signal<Unidade[]>([]);
  protected readonly metricas = signal<DashboardMetricas | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erroCarregamento = signal(false);

  protected readonly opcoesUnidades = computed<OpcaoDropdown[]>(() =>
    this.unidades().map((u) => ({ valor: u.id, rotulo: u.nome })),
  );

  protected readonly rotuloPeriodo = computed(() => {
    if (this.periodoSelecionado() === 'custom') {
      const ini = this.dataInicioCustom();
      const fim = this.dataFimCustom();
      if (ini && fim) {
        return `${this.formatarDataCurta(`${ini}T00:00:00Z`)} – ${this.formatarDataCurta(`${fim}T00:00:00Z`)}`;
      }
      return 'Período personalizado';
    }
    return `${this.periodoSelecionado()} dias`;
  });

  // Alertas automáticos da Seção 9.7 (Signals reativos)
  protected readonly alertaM12 = computed(
    () => this.metricas()?.alertas.alerta_critico_privacidade_m12 ?? false,
  );
  protected readonly alertaM08 = computed(
    () => this.metricas()?.alertas.alerta_divergencia_prioridade_m08 ?? false,
  );
  protected readonly alertaM11 = computed(
    () => this.metricas()?.alertas.alerta_tempo_registro_m11 ?? false,
  );
  protected readonly possuiAlertas = computed(
    () => this.alertaM12() || this.alertaM08() || this.alertaM11(),
  );

  protected readonly maxChamadosUnidade = computed(() => {
    const lista = this.metricas()?.chamados_por_unidade ?? [];
    return Math.max(1, ...lista.map((u) => u.total_chamados));
  });

  protected readonly maxChamadosResponsavel = computed(() => {
    const lista = this.metricas()?.chamados_por_responsavel ?? [];
    return Math.max(1, ...lista.map((r) => r.total_em_aberto));
  });

  protected readonly maxTempoTerceiros = computed(() => {
    const lista = this.metricas()?.tempo_em_terceiros ?? [];
    return Math.max(1, ...lista.map((f) => f.tempo_medio_retorno_segundos));
  });

  protected readonly maxVolumeCategoria = computed(() => {
    const lista = this.metricas()?.volume_por_categoria ?? [];
    return Math.max(1, ...lista.map((c) => c.total_chamados));
  });

  protected readonly maxVolumeEquipamento = computed(() => {
    const lista = this.metricas()?.volume_por_equipamento ?? [];
    return Math.max(1, ...lista.map((e) => e.total_chamados));
  });

  protected readonly p1AbaixoDaMeta = computed(() => {
    const p1 = this.metricas()?.sla_por_prioridade.find((p) => p.prioridade === 'ALTA');
    return p1?.abaixo_da_meta_inicial ?? false;
  });

  ngOnInit(): void {
    this.unidadeService
      .listarAtivas()
      .pipe(catchError(() => of([])))
      .subscribe((lista) => this.unidades.set(lista));

    this.carregar();
  }

  protected alterarPeriodo(valor: string): void {
    const periodo = (valor || '30') as PeriodoDias;
    this.periodoSelecionado.set(periodo);
    if (periodo !== 'custom') {
      this.carregar();
    }
  }

  protected alterarDataInicio(valor: string): void {
    this.dataInicioCustom.set(valor);
  }

  protected alterarDataFim(valor: string): void {
    this.dataFimCustom.set(valor);
  }

  protected aplicarPeriodoPersonalizado(): void {
    const ini = this.dataInicioCustom();
    const fim = this.dataFimCustom();
    if (!ini || !fim) {
      this.toast.erro('Informe a data inicial e a data final.');
      return;
    }
    if (ini > fim) {
      this.toast.erro('A data inicial não pode ser posterior à data final.');
      return;
    }
    this.carregar();
  }

  protected alterarUnidade(valor: string): void {
    this.unidadeSelecionada.set(valor);
    this.carregar();
  }

  protected carregar(): void {
    this.carregando.set(true);
    this.erroCarregamento.set(false);

    let inicio: Date;
    let fim: Date;

    if (this.periodoSelecionado() === 'custom') {
      inicio = new Date(`${this.dataInicioCustom()}T00:00:00.000Z`);
      fim = new Date(`${this.dataFimCustom()}T23:59:59.999Z`);
    } else {
      const dias = Number(this.periodoSelecionado());
      fim = new Date();
      inicio = new Date(fim.getTime() - dias * 24 * 60 * 60 * 1000);
    }
    const unidadeId = this.unidadeSelecionada();

    this.dashboardService
      .obterMetricas({
        inicio: inicio.toISOString(),
        fim: fim.toISOString(),
        unidade_id: unidadeId || undefined,
      })
      .pipe(finalize(() => this.carregando.set(false)))
      .subscribe({
        next: (dados) => this.metricas.set(dados),
        error: () => this.erroCarregamento.set(true),
      });
  }

  protected exportarCsv(): void {
    const m = this.metricas();
    if (!m) return;

    const linhas: string[] = [
      'Secao;Indicador;Valor;Detalhe',
      `KPI;Chamados abertos (${this.rotuloPeriodo()});${m.kpis.chamados_abertos.total_atual};${this.formatarVariacaoChamados(m.kpis.chamados_abertos.variacao_percentual)}`,
      `KPI;Tempo mediano de resolucao;${this.formatarDuracao(m.kpis.tempo_resolucao.mediano_segundos)};${this.formatarDiferencaTempo(m.kpis.tempo_resolucao.diferenca_mediana_segundos_anterior)}`,
      `KPI;Tempo de resolucao (P90);${this.formatarDuracao(m.kpis.tempo_resolucao.p90_segundos)};90% resolvidos ate este prazo`,
      `KPI;Chamados rastreaveis;${Math.round(m.kpis.chamados_rastreaveis.percentual)}%;Meta >= ${m.kpis.chamados_rastreaveis.meta_inicial_percentual}%`,
      `KPI;Dentro da meta (SLA);${Math.round(m.kpis.dentro_da_meta_sla.percentual)}%;Meta >= ${m.kpis.dentro_da_meta_sla.meta_inicial_percentual}%`,
      `Processo;M06 Taxa de reabertura;${m.indicadores_processo.m06_taxa_reabertura_percentual}%;Sobre chamados resolvidos/fechados`,
      `Processo;M07 Duplicidade (mesclados);${m.indicadores_processo.m07_taxa_duplicidade_percentual}%;${m.indicadores_processo.m07_total_duplicados} chamado(s) mesclado(s)`,
      ...m.chamados_por_unidade.map(
        (u) =>
          `Unidade;${u.unidade_nome};${u.total_chamados};Tempo medio: ${this.formatarDuracao(u.tempo_medio_atendimento_segundos)} | ${Math.round(u.percentual_dentro_da_meta)}% na meta`,
      ),
      ...m.chamados_por_responsavel.map(
        (r) =>
          `Responsavel (M02);${r.responsavel_nome};${r.total_em_aberto};${r.total_alta_prioridade} P1 em aberto`,
      ),
      ...m.sla_por_prioridade.map(
        (p) => `SLA Prioridade;${p.rotulo};${Math.round(p.percentual_dentro_da_meta)}%;${p.total_dentro_da_meta}/${p.total_avaliados}`,
      ),
      ...m.problemas_recorrentes.map(
        (r) =>
          `Problema Recorrente;${r.problema};${r.ocorrencias};${this.formatarUnidadesAfetadas(r.unidades_mais_afetadas)}`,
      ),
      ...m.tempo_em_terceiros.map(
        (t) =>
          `Tempo em Terceiros;${t.fornecedor};${this.formatarDuracao(t.tempo_medio_retorno_segundos)};${t.escalonamentos_concluidos}/${t.total_escalonamentos} concluidos`,
      ),
    ];

    const conteudo = '\uFEFF' + linhas.join('\n');
    const blob = new Blob([conteudo], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `painel-administracao-${this.periodoSelecionado()}.csv`;
    link.click();
    URL.revokeObjectURL(url);
    this.toast.sucesso('Relatório CSV exportado.');
  }

  protected larguraPercentual(valor: number, maximo: number): number {
    if (valor <= 0 || maximo <= 0) return 0;
    return Math.min(100, Math.max(4, Math.round((valor / maximo) * 100)));
  }

  protected larguraSla(percentual: number): number {
    if (percentual <= 0) return 0;
    return Math.min(100, Math.max(4, Math.round(percentual)));
  }

  protected formatarPercentual(valor: number): string {
    return `${Math.round(valor)}%`;
  }

  protected formatarVariacaoChamados(variacao: number | null): string {
    if (variacao === null) return 'Sem histórico no período anterior';
    const arredondado = Math.round(variacao);
    if (arredondado > 0) return `+${arredondado}% em relação ao período anterior`;
    if (arredondado < 0) return `${arredondado}% em relação ao período anterior`;
    return 'Estável em relação ao período anterior';
  }

  protected formatarDiferencaTempo(diferencaSegundos: number | null): string {
    if (diferencaSegundos === null) return 'Sem base comparativa anterior';
    const abs = Math.abs(diferencaSegundos);
    if (abs < 60) return 'Estável em relação ao período anterior';
    const textoTempo = this.formatarDuracao(abs);
    return diferencaSegundos < 0
      ? `${textoTempo} mais rápido que o período anterior`
      : `${textoTempo} mais lento que o período anterior`;
  }

  protected formatarDuracao(segundos: number): string {
    if (!segundos || segundos <= 0) return '—';
    const totalMin = Math.round(segundos / 60);
    if (totalMin < 1) return `${Math.round(segundos)} s`;
    if (totalMin < 60) return `${totalMin} min`;

    const totalHoras = Math.floor(totalMin / 60);
    const minRestantes = totalMin % 60;
    if (totalHoras < 24) {
      return minRestantes === 0 ? `${totalHoras} h` : `${totalHoras} h ${minRestantes}`;
    }

    const dias = Math.floor(totalHoras / 24);
    const horasRestantes = totalHoras % 24;
    return horasRestantes === 0 ? `${dias} d` : `${dias} d ${horasRestantes} h`;
  }

  protected formatarDataCurta(iso: string): string {
    const data = new Date(iso);
    if (isNaN(data.getTime())) return '—';
    return data.toLocaleDateString('pt-BR', { timeZone: 'UTC' });
  }

  protected formatarUnidadesAfetadas(unidades: string[]): string {
    if (!unidades.length) return '—';
    return unidades.map((nome) => this.abreviarUnidade(nome)).join(' · ');
  }

  protected classePilulaPrioridade(prioridade: string): string {
    switch (prioridade) {
      case 'ALTA':
        return 'bg-[#e5484d] text-white';
      case 'MEDIA':
        return 'bg-[#f59e0b] text-white';
      default:
        return 'bg-[#6b8299] text-white';
    }
  }

  private abreviarUnidade(nome: string): string {
    const match = nome.match(/^(U\.\d+)/i);
    return match ? match[1] : nome;
  }

  private diasAtras(dias: number): Date {
    return new Date(Date.now() - dias * 24 * 60 * 60 * 1000);
  }

  private formatarDataInput(data: Date): string {
    return data.toISOString().slice(0, 10);
  }
}
