import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { catchError, finalize, of } from 'rxjs';
import { DashboardService } from '../../../core/services/dashboard.service';
import { ToastService } from '../../../core/services/toast.service';
import { UnidadeService } from '../../../core/services/unidade.service';
import {
  CustomDropdown,
  OpcaoDropdown,
} from '../../../shared/components/custom-dropdown/custom-dropdown';
import { DashboardMetricas } from '../../../shared/models/dashboard.model';
import { Unidade } from '../../../shared/models/unidade.model';

type PeriodoDias = '7' | '30' | '90';

@Component({
  selector: 'app-painel-administracao',
  imports: [CustomDropdown],
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
  ];

  protected readonly periodoSelecionado = signal<PeriodoDias>('30');
  protected readonly unidadeSelecionada = signal<string>('');
  protected readonly unidades = signal<Unidade[]>([]);
  protected readonly metricas = signal<DashboardMetricas | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erroCarregamento = signal(false);

  protected readonly opcoesUnidades = computed<OpcaoDropdown[]>(() =>
    this.unidades().map((u) => ({ valor: u.id, rotulo: u.nome })),
  );

  protected readonly rotuloPeriodo = computed(() => `${this.periodoSelecionado()} dias`);

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
    this.periodoSelecionado.set((valor || '30') as PeriodoDias);
    this.carregar();
  }

  protected alterarUnidade(valor: string): void {
    this.unidadeSelecionada.set(valor);
    this.carregar();
  }

  protected carregar(): void {
    this.carregando.set(true);
    this.erroCarregamento.set(false);

    const dias = Number(this.periodoSelecionado());
    const fim = new Date();
    const inicio = new Date(fim.getTime() - dias * 24 * 60 * 60 * 1000);
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
      `KPI;Chamados rastreaveis;${Math.round(m.kpis.chamados_rastreaveis.percentual)}%;Meta >= ${m.kpis.chamados_rastreaveis.meta_inicial_percentual}%`,
      `KPI;Dentro da meta (SLA);${Math.round(m.kpis.dentro_da_meta_sla.percentual)}%;Meta >= ${m.kpis.dentro_da_meta_sla.meta_inicial_percentual}%`,
      ...m.chamados_por_unidade.map(
        (u) =>
          `Unidade;${u.unidade_nome};${u.total_chamados};${Math.round(u.percentual_dentro_da_meta)}% na meta - tempo medio ${this.formatarDuracao(u.tempo_medio_atendimento_segundos)}`,
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
    link.download = `painel-administracao-${this.periodoSelecionado()}d.csv`;
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
}
