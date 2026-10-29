import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { ChamadoService } from '../../../core/services/chamado.service';
import { CustomDropdown } from '../../../shared/components/custom-dropdown/custom-dropdown';
import {
  CANAIS_ORIGEM,
  CanalOrigem,
  ChamadoResumo,
  PRIORIDADES_CHAMADO,
  PrioridadeChamado,
  STATUS_CHAMADO,
  SituacaoSla,
  StatusChamado,
} from '../../../shared/models/chamado.model';
import { extrairMensagensErro } from '../../../shared/utils/api-error';
import { USUARIO_ATUAL } from '../../../shared/utils/usuario-atual';

const ITENS_POR_PAGINA = 8;
const SEM_PRIORIDADE = 'SEM_PRIORIDADE';

type FiltroPrioridade = PrioridadeChamado | typeof SEM_PRIORIDADE | '';

interface Indicador {
  id: string;
  rotulo: string;
  valor: number;
  corPonto: string;
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
export class FilaChamados implements OnInit {
  private readonly chamadoService = inject(ChamadoService);
  private readonly authService = inject(AuthService);

  protected readonly canais = CANAIS_ORIGEM;
  protected readonly statusOpcoes = STATUS_CHAMADO;
  protected readonly prioridades = PRIORIDADES_CHAMADO;
  protected readonly semPrioridade = SEM_PRIORIDADE;
  protected readonly linhasEsqueleto = Array.from({ length: ITENS_POR_PAGINA }, (_, i) => i);

  protected readonly chamados = signal<ChamadoResumo[]>([]);
  protected readonly carregando = signal(true);
  protected readonly erroCarregamento = signal<string | null>(null);

  protected readonly busca = signal('');

  constructor() {
    inject(ActivatedRoute)
      .queryParamMap.pipe(takeUntilDestroyed())
      .subscribe((params) => {
        const termo = params.get('busca');
        if (termo !== null) {
          this.busca.set(termo);
          this.pagina.set(1);
        }
      });
  }
  protected readonly filtroUnidade = signal('');
  protected readonly filtroStatus = signal<StatusChamado | ''>('');
  protected readonly filtroPrioridade = signal<FiltroPrioridade>('');
  protected readonly filtroCanal = signal<CanalOrigem | ''>('');
  protected readonly somenteMeus = signal(false);
  protected readonly pagina = signal(1);

  protected readonly unidades = computed(() => {
    const mapa = new Map<string, string>();
    for (const c of this.chamados()) mapa.set(c.unidade.id, c.unidade.nome);
    return [...mapa]
      .map(([id, nome]) => ({ id, nome }))
      .sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
  });

  protected readonly unidadesOpcoes = computed(() =>
    this.unidades().map((u) => ({ valor: u.id, rotulo: u.nome })),
  );

  protected readonly prioridadesOpcoes = computed(() => [
    ...this.prioridades,
    { valor: this.semPrioridade, rotulo: 'Sem prioridade' },
  ]);

  protected readonly indicadores = computed<Indicador[]>(() => {
    const lista = this.chamados();
    const contar = (fn: (c: ChamadoResumo) => boolean) => lista.filter(fn).length;
    return [
      { id: 'abertos', rotulo: 'Abertos', corPonto: 'bg-[#ff6a3d]', valor: contar((c) => c.status === 'ABERTO' || c.status === 'REABERTO') },
      { id: 'triagem', rotulo: 'Em triagem', corPonto: 'bg-[#14b8a6]', valor: contar((c) => c.status === 'EM_TRIAGEM') },
      { id: 'atendimento', rotulo: 'Em atendimento', corPonto: 'bg-[#0ea5e9]', valor: contar((c) => c.status === 'EM_ATENDIMENTO') },
      { id: 'aguardando', rotulo: 'Aguardando', corPonto: 'bg-[#f0a202]', valor: contar((c) => c.status === 'AGUARDANDO') },
      { id: 'fora-meta', rotulo: 'Fora da meta', corPonto: 'bg-[#dc2626]', valor: contar((c) => c.sla.situacao === 'ESTOURADO') },
    ];
  });

  protected readonly filtrados = computed(() => {
    const termo = this.normalizar(this.busca());
    const unidade = this.filtroUnidade();
    const status = this.filtroStatus();
    const prioridade = this.filtroPrioridade();
    const canal = this.filtroCanal();
    const meus = this.somenteMeus();
    const idUsuarioAtual = this.authService.usuario()?.id ?? USUARIO_ATUAL.id;

    return this.chamados().filter((c) => {
      if (unidade && c.unidade.id !== unidade) return false;
      if (status && c.status !== status) return false;
      if (canal && c.canal_origem !== canal) return false;
      if (prioridade === SEM_PRIORIDADE && c.prioridade !== null) return false;
      if (prioridade && prioridade !== SEM_PRIORIDADE && c.prioridade !== prioridade) return false;
      if (meus && c.responsavel?.id !== idUsuarioAtual && c.responsavel?.id !== USUARIO_ATUAL.id) {
        return false;
      }
      if (termo && !this.normalizar(`${c.codigo} ${c.titulo}`).includes(termo)) return false;
      return true;
    });
  });

  protected readonly totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.filtrados().length / ITENS_POR_PAGINA)),
  );

  protected readonly paginas = computed(() =>
    Array.from({ length: this.totalPaginas() }, (_, i) => i + 1),
  );

  protected readonly paginaAtual = computed(() => Math.min(this.pagina(), this.totalPaginas()));

  protected readonly itensPagina = computed(() => {
    const inicio = (this.paginaAtual() - 1) * ITENS_POR_PAGINA;
    return this.filtrados().slice(inicio, inicio + ITENS_POR_PAGINA);
  });

  protected readonly faixaExibida = computed(() => {
    const total = this.filtrados().length;
    if (total === 0) return { inicio: 0, fim: 0, total };
    const inicio = (this.paginaAtual() - 1) * ITENS_POR_PAGINA + 1;
    return { inicio, fim: inicio + this.itensPagina().length - 1, total };
  });

  protected readonly temFiltroAtivo = computed(
    () =>
      !!this.busca().trim() ||
      !!this.filtroUnidade() ||
      !!this.filtroStatus() ||
      !!this.filtroPrioridade() ||
      !!this.filtroCanal() ||
      this.somenteMeus(),
  );

  ngOnInit(): void {
    this.carregar();
  }

  protected carregar(): void {
    this.carregando.set(true);
    this.erroCarregamento.set(null);
    this.chamadoService
      .listar()
      .pipe(finalize(() => this.carregando.set(false)))
      .subscribe({
        next: (lista) => this.chamados.set(lista),
        error: (erro) => this.erroCarregamento.set(extrairMensagensErro(erro)[0]),
      });
  }

  protected atualizarFiltro<T>(alvo: { set(valor: T): void }, valor: T): void {
    alvo.set(valor);
    this.pagina.set(1);
  }

  protected limparFiltros(): void {
    this.busca.set('');
    this.filtroUnidade.set('');
    this.filtroStatus.set('');
    this.filtroPrioridade.set('');
    this.filtroCanal.set('');
    this.somenteMeus.set(false);
    this.pagina.set(1);
  }

  protected irParaPagina(pagina: number): void {
    this.pagina.set(Math.min(Math.max(1, pagina), this.totalPaginas()));
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

  private normalizar(valor: string): string {
    return valor.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
  }
}
