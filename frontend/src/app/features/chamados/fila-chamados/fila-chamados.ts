import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription, finalize } from 'rxjs';
import { ChamadoService } from '../../../core/services/chamado.service';
import { UnidadeService } from '../../../core/services/unidade.service';
import { CustomDropdown } from '../../../shared/components/custom-dropdown/custom-dropdown';
import {
  CANAIS_ORIGEM,
  CanalOrigem,
  FilaChamados as PaginaFila,
  FiltrosFila,
  PRIORIDADES_CHAMADO,
  PrioridadeChamado,
  STATUS_CHAMADO,
  SituacaoSla,
  StatusChamado,
  TAMANHO_PAGINA_FILA,
} from '../../../shared/models/chamado.model';
import { extrairMensagensErro } from '../../../shared/utils/api-error';

const SEM_PRIORIDADE = 'SEM_PRIORIDADE';
/** Quantos botões de página aparecem ao redor da página atual. */
const PAGINAS_VISIVEIS = 5;

type FiltroPrioridade = PrioridadeChamado | typeof SEM_PRIORIDADE | '';

interface Indicador {
  id: string;
  rotulo: string;
  /** Nulo quando o dado ainda não existe (ex.: estourados depende do cálculo de SLA). */
  valor: number | null;
  corPonto: string;
  dica?: string;
}

const ESTILO_STATUS: Record<StatusChamado, string> = {
  ABERTO: 'bg-[#ff6a3d]/10 text-[#ff6a3d]',
  EM_TRIAGEM: 'bg-[#14b8a6]/10 text-[#14b8a6]',
  EM_ATENDIMENTO: 'bg-[#0ea5e9]/10 text-[#0ea5e9]',
  AGUARDANDO: 'bg-[#f0a202]/10 text-[#f0a202]',
  RESOLVIDO: 'bg-[#22c55e]/10 text-[#22c55e]',
  REABERTO: 'bg-[#e5484d]/10 text-[#e5484d]',
  FECHADO: 'bg-[#64748b]/10 text-[#64748b]',
  CANCELADO: 'bg-[#94a3b8]/15 text-[#64748b]',
  MESCLADO: 'bg-[#8b5cf6]/10 text-[#8b5cf6]',
};

const ESTILO_PRIORIDADE: Record<PrioridadeChamado, string> = {
  ALTA: 'bg-[#dc2626] text-white',
  MEDIA: 'bg-[#f59e0b] text-white',
  BAIXA: 'bg-[#94a3b8] text-white',
};

const ESTILO_SLA: Record<SituacaoSla, { texto: string; barra: string }> = {
  TRIAGEM: { texto: 'text-[#363b47]', barra: 'bg-[#14b8a6]' },
  DENTRO_DA_META: { texto: 'text-[#14161b]', barra: 'bg-[#22c55e]' },
  EM_RISCO: { texto: 'text-[#f59e0b]', barra: 'bg-[#f59e0b]' },
  ESTOURADO: { texto: 'text-[#dc2626]', barra: 'bg-[#dc2626]' },
  PAUSADO: { texto: 'text-[#6b7280]', barra: 'bg-[#9aa2b1]' },
  CUMPRIDO: { texto: 'text-[#14161b]', barra: 'bg-[#22c55e]' },
  INCORPORADO: { texto: 'text-[#6b7280]', barra: '' },
};

@Component({
  selector: 'app-fila-chamados',
  imports: [RouterLink, CustomDropdown],
  templateUrl: './fila-chamados.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
/**
 * Fila de chamados (HU14). Filtros, busca e paginação são feitos pelo servidor (GET /chamados):
 * mudar um filtro recarrega a página 1 mantendo os demais; trocar de página mantém os filtros.
 */
export class FilaChamados implements OnInit {
  private readonly chamadoService = inject(ChamadoService);
  private readonly unidadeService = inject(UnidadeService);
  private requisicao?: Subscription;
  /** A busca do cabeçalho só recarrega depois do primeiro carregamento (ngOnInit). */
  private iniciado = false;

  protected readonly canais = CANAIS_ORIGEM;
  protected readonly statusOpcoes = STATUS_CHAMADO;
  protected readonly prioridades = PRIORIDADES_CHAMADO;
  protected readonly semPrioridade = SEM_PRIORIDADE;
  protected readonly linhasEsqueleto = Array.from({ length: 8 }, (_, i) => i);

  protected readonly fila = signal<PaginaFila | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erroCarregamento = signal<string | null>(null);

  /** Texto digitado; só vira filtro (código exato) ao apertar Enter ou ao ser apagado. */
  protected readonly busca = signal('');
  protected readonly codigoBuscado = signal('');

  constructor() {
    // Busca global do cabeçalho: /chamados?busca=LUM-2026-000127
    inject(ActivatedRoute)
      .queryParamMap.pipe(takeUntilDestroyed())
      .subscribe((params) => {
        const termo = params.get('busca');
        if (termo !== null) {
          this.busca.set(termo);
          this.codigoBuscado.set(termo.trim());
          this.pagina.set(1);
          if (this.iniciado) this.carregar();
        }
      });
  }


  protected readonly filtroUnidade = signal('');
  protected readonly filtroStatus = signal<StatusChamado | ''>('');
  protected readonly filtroPrioridade = signal<FiltroPrioridade>('');
  protected readonly filtroCanal = signal<CanalOrigem | ''>('');
  protected readonly somenteMeus = signal(false);
  protected readonly somenteSemResponsavel = signal(false);
  /** Página exibida, começando em 1 (a API começa em 0). */
  protected readonly pagina = signal(1);

  /** Todas as unidades, inclusive inativas, para filtrar também chamados antigos. */
  protected readonly unidadesOpcoes = signal<{ valor: string; rotulo: string }[]>([]);

  protected readonly prioridadesOpcoes = computed(() => [
    ...this.prioridades,
    { valor: this.semPrioridade, rotulo: 'Sem prioridade' },
  ]);

  protected readonly itensPagina = computed(() => this.fila()?.itens ?? []);

  /** Cartões do topo: contagem do servidor com os filtros atuais, menos o de status. */
  protected readonly indicadores = computed<Indicador[]>(() => {
    const totais = this.fila()?.totais_por_status;
    const contar = (...status: StatusChamado[]) =>
      status.reduce((soma, s) => soma + (totais?.[s] ?? 0), 0);
    return [
      { id: 'abertos', rotulo: 'Abertos', corPonto: 'bg-[#ff6a3d]', valor: contar('ABERTO', 'REABERTO') },
      { id: 'triagem', rotulo: 'Em triagem', corPonto: 'bg-[#14b8a6]', valor: contar('EM_TRIAGEM') },
      { id: 'atendimento', rotulo: 'Em atendimento', corPonto: 'bg-[#0ea5e9]', valor: contar('EM_ATENDIMENTO') },
      { id: 'aguardando', rotulo: 'Aguardando', corPonto: 'bg-[#f0a202]', valor: contar('AGUARDANDO') },
      {
        id: 'estourados',
        rotulo: 'Estourados',
        corPonto: 'bg-[#dc2626]',
        valor: null,
        dica: 'Disponível quando o cálculo de SLA estiver pronto.',
      },
    ];
  });

  protected readonly totalPaginas = computed(() => Math.max(1, this.fila()?.total_paginas ?? 1));

  protected readonly paginaAtual = computed(() => Math.min(this.pagina(), this.totalPaginas()));

  /** Botões de página ao redor da atual, para não listar dezenas de páginas. */
  protected readonly paginas = computed(() => {
    const total = this.totalPaginas();
    const inicio = Math.max(
      1,
      Math.min(this.paginaAtual() - Math.floor(PAGINAS_VISIVEIS / 2), total - PAGINAS_VISIVEIS + 1),
    );
    const fim = Math.min(total, inicio + PAGINAS_VISIVEIS - 1);
    return Array.from({ length: fim - inicio + 1 }, (_, i) => inicio + i);
  });

  protected readonly faixaExibida = computed(() => {
    const fila = this.fila();
    const total = fila?.total_itens ?? 0;
    if (!fila || total === 0) return { inicio: 0, fim: 0, total };
    const inicio = fila.pagina * fila.tamanho + 1;
    return { inicio, fim: inicio + fila.itens.length - 1, total };
  });

  /** Algum chamado da página traz SLA (só no modo simulado, por enquanto). */
  protected readonly temSla = computed(() => this.itensPagina().some((c) => !!c.sla));

  protected readonly temFiltroAtivo = computed(
    () =>
      !!this.codigoBuscado() ||
      !!this.filtroUnidade() ||
      !!this.filtroStatus() ||
      !!this.filtroPrioridade() ||
      !!this.filtroCanal() ||
      this.somenteMeus() ||
      this.somenteSemResponsavel(),
  );

  ngOnInit(): void {
    this.iniciado = true;
    this.carregarUnidades();
    this.carregar();
  }

  protected carregar(): void {
    this.requisicao?.unsubscribe();
    this.carregando.set(true);
    this.erroCarregamento.set(null);
    this.requisicao = this.chamadoService
      .listar(this.filtrosAtuais(), this.pagina() - 1, TAMANHO_PAGINA_FILA)
      .pipe(finalize(() => this.carregando.set(false)))
      .subscribe({
        next: (fila) => this.fila.set(fila),
        error: (erro) => this.erroCarregamento.set(extrairMensagensErro(erro)[0]),
      });
  }

  protected atualizarFiltro<T>(alvo: { set(valor: T): void }, valor: T): void {
    alvo.set(valor);
    this.pagina.set(1);
    this.carregar();
  }

  /** "Meus" e "sem responsável" se excluem: juntos, o resultado seria sempre vazio. */
  protected alternarMeus(marcado: boolean): void {
    this.somenteMeus.set(marcado);
    if (marcado) this.somenteSemResponsavel.set(false);
    this.pagina.set(1);
    this.carregar();
  }

  protected alternarSemResponsavel(marcado: boolean): void {
    this.somenteSemResponsavel.set(marcado);
    if (marcado) this.somenteMeus.set(false);
    this.pagina.set(1);
    this.carregar();
  }

  protected digitarBusca(texto: string): void {
    this.busca.set(texto);
    // Apagar o campo volta a lista completa, sem precisar apertar Enter.
    if (!texto.trim() && this.codigoBuscado()) this.aplicarBusca();
  }

  protected aplicarBusca(): void {
    this.codigoBuscado.set(this.busca().trim());
    this.pagina.set(1);
    this.carregar();
  }

  protected limparFiltros(): void {
    this.busca.set('');
    this.codigoBuscado.set('');
    this.filtroUnidade.set('');
    this.filtroStatus.set('');
    this.filtroPrioridade.set('');
    this.filtroCanal.set('');
    this.somenteMeus.set(false);
    this.somenteSemResponsavel.set(false);
    this.pagina.set(1);
    this.carregar();
  }

  protected irParaPagina(pagina: number): void {
    const destino = Math.min(Math.max(1, pagina), this.totalPaginas());
    if (destino === this.paginaAtual()) return;
    this.pagina.set(destino);
    this.carregar();
  }

  private filtrosAtuais(): FiltrosFila {
    const prioridade = this.filtroPrioridade();
    return {
      status: this.filtroStatus() ? [this.filtroStatus() as StatusChamado] : undefined,
      unidade_id: this.filtroUnidade() || undefined,
      prioridade: prioridade && prioridade !== SEM_PRIORIDADE ? prioridade : undefined,
      sem_prioridade: prioridade === SEM_PRIORIDADE,
      canal_origem: this.filtroCanal() || undefined,
      meus: this.somenteMeus(),
      sem_responsavel: this.somenteSemResponsavel(),
      codigo: this.codigoBuscado() || undefined,
    };
  }

  private carregarUnidades(): void {
    this.unidadeService.listar().subscribe({
      next: (unidades) =>
        this.unidadesOpcoes.set(
          [...unidades]
            .sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'))
            .map((u) => ({ valor: u.id, rotulo: u.nome })),
        ),
      error: () => this.unidadesOpcoes.set([]),
    });
  }

  protected valorInput(evento: Event): string {
    return (evento.target as HTMLInputElement | HTMLSelectElement).value;
  }

  protected marcado(evento: Event): boolean {
    return (evento.target as HTMLInputElement).checked;
  }

  protected rotuloStatus(status: StatusChamado): string {
    return STATUS_CHAMADO.find((s) => s.valor === status)?.rotulo ?? status;
  }

  protected rotuloPrioridade(prioridade: PrioridadeChamado): string {
    return PRIORIDADES_CHAMADO.find((p) => p.valor === prioridade)?.rotulo ?? prioridade;
  }

  protected rotuloCanal(canal: CanalOrigem): string {
    return CANAIS_ORIGEM.find((c) => c.valor === canal)?.rotulo ?? canal;
  }

  protected estiloStatus(status: StatusChamado): string {
    return ESTILO_STATUS[status];
  }

  protected estiloPrioridade(prioridade: PrioridadeChamado): string {
    return ESTILO_PRIORIDADE[prioridade];
  }

  protected estiloSla(situacao: SituacaoSla): { texto: string; barra: string } {
    return ESTILO_SLA[situacao];
  }

  protected iniciais(nome: string): string {
    return nome
      .split(' ')
      .filter(Boolean)
      .map((p) => p[0])
      .slice(0, 2)
      .join('')
      .toUpperCase();
  }

  protected tempoRelativo(iso: string): string {
    const minutos = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 60_000));
    if (minutos < 1) return 'agora';
    if (minutos < 60) return `há ${minutos} min`;
    const horas = Math.floor(minutos / 60);
    if (horas < 24) return `há ${horas} h`;
    const dias = Math.floor(horas / 24);
    return dias === 1 ? 'ontem' : `há ${dias} dias`;
  }

}
