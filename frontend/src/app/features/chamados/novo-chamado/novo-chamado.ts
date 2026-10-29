import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { debounceTime, finalize, forkJoin } from 'rxjs';
import { ChamadoService } from '../../../core/services/chamado.service';
import { ListasChamadoService } from '../../../core/services/listas-chamado.service';
import { ToastService } from '../../../core/services/toast.service';
import { UnidadeService } from '../../../core/services/unidade.service';
import {
  CANAIS_ORIGEM,
  CanalOrigem,
  ChamadoRequest,
  ChamadoResponse,
  ChamadoResumo,
  ItemLista,
  PrioridadeChamado,
  StatusChamado,
} from '../../../shared/models/chamado.model';
import { Unidade } from '../../../shared/models/unidade.model';
import { extrairMensagensErro } from '../../../shared/utils/api-error';
import { contatoValidator, normalizarContato } from '../../../shared/utils/contato';

const LIMITE_DESCRICAO = 2000;
const TAMANHO_MAXIMO_ANEXO = 10 * 1024 * 1024; // 10 MB
const TIPOS_ANEXO = ['image/png', 'image/jpeg', 'image/jpg', 'application/pdf'];
const SIMILARIDADE_MINIMA = 0.25;
const MAXIMO_DUPLICIDADES = 3;

export type OpcaoPrioridade = 'TRIAGEM' | PrioridadeChamado;

export interface AnexoItem {
  id: string;
  arquivo: File;
  nome: string;
  tamanhoFormatado: string;
}

export interface DuplicidadeItem {
  chamado: ChamadoResumo;
  percentual: number;
}

type CampoChamado =
  | 'canalOrigem'
  | 'unidadeId'
  | 'contato'
  | 'nome'
  | 'categoriaId'
  | 'sistemaId'
  | 'descricao'
  | 'prioridade'
  | 'atribuirAMim';

function radicais(texto: string): Set<string> {
  const palavras = texto
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, ' ')
    .split(/\s+/)
    .filter((p) => p.length >= 3);
  return new Set(palavras.map((p) => p.slice(0, 5)));
}

@Component({
  selector: 'app-novo-chamado',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './novo-chamado.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NovoChamado implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly unidadeService = inject(UnidadeService);
  private readonly chamadoService = inject(ChamadoService);
  private readonly listasChamadoService = inject(ListasChamadoService);
  private readonly toast = inject(ToastService);

  protected readonly canais = CANAIS_ORIGEM;
  protected readonly limiteDescricao = LIMITE_DESCRICAO;

  /** Opções vindas da API (GET /categorias e GET /sistemas-equipamentos), só itens ativos. */
  protected readonly categorias = signal<ItemLista[]>([]);
  protected readonly sistemas = signal<ItemLista[]>([]);
  protected readonly carregandoListas = signal(true);
  protected readonly erroListas = signal(false);

  protected readonly prioridades: ReadonlyArray<{ valor: OpcaoPrioridade; rotulo: string }> = [
    { valor: 'TRIAGEM', rotulo: 'Definir na triagem' },
    { valor: 'ALTA', rotulo: 'Alta' },
    { valor: 'MEDIA', rotulo: 'Média' },
    { valor: 'BAIXA', rotulo: 'Baixa' },
  ];

  protected readonly prazos = [
    { prioridade: 'Alta (P1)', estilo: 'bg-[#dc2626] text-white', prazo: '15 min · 2 h' },
    { prioridade: 'Média (P2)', estilo: 'bg-[#f59e0b] text-white', prazo: '1 h · 8 h úteis' },
    { prioridade: 'Baixa (P3)', estilo: 'bg-[#94a3b8] text-white', prazo: '4 h · 3 dias úteis' },
  ];

  protected readonly unidades = signal<Unidade[]>([]);
  protected readonly carregandoUnidades = signal(true);
  protected readonly erroUnidades = signal(false);
  protected readonly enviando = signal(false);
  protected readonly errosApi = signal<string[]>([]);
  protected readonly chamadoCriado = signal<ChamadoResponse | null>(null);

  protected readonly chamadosExistentes = signal<ChamadoResumo[]>([]);
  protected readonly vinculadoId = signal<string | null>(null);
  protected readonly anexos = signal<AnexoItem[]>([]);
  protected readonly erroAnexo = signal<string | null>(null);
  protected readonly arrastando = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    canalOrigem: ['LIGACAO' as CanalOrigem, Validators.required],
    unidadeId: ['', Validators.required],
    contato: ['', [Validators.required, contatoValidator]],
    nome: ['', Validators.maxLength(120)],
    // Sem valor pré-selecionado: o analista escolhe, para a categoria não sair enviesada nas métricas.
    categoriaId: ['', Validators.required],
    sistemaId: [''],
    descricao: [
      '',
      [Validators.required, Validators.minLength(10), Validators.maxLength(LIMITE_DESCRICAO)],
    ],
    prioridade: ['TRIAGEM' as OpcaoPrioridade],
    atribuirAMim: [true],
  });

  private readonly valores = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });

  private readonly valoresDebounced = toSignal(
    this.form.valueChanges.pipe(debounceTime(300)),
    { initialValue: this.form.getRawValue() },
  );

  protected readonly tamanhoDescricao = computed(() => this.valores().descricao?.length ?? 0);
  protected readonly canalSelecionado = computed(() => this.valores().canalOrigem);
  protected readonly prioridadeSelecionada = computed(() => this.valores().prioridade);

  protected readonly duplicidades = computed<DuplicidadeItem[]>(() => {
    const { descricao = '', sistemaId = '', unidadeId = '' } = this.valoresDebounced();
    const sistema = this.sistemas().find((s) => s.id === sistemaId)?.nome ?? '';
    const termos = radicais(`${descricao} ${sistema}`);
    if (termos.size === 0) return [];

    return this.chamadosExistentes()
      .filter((c) => c.status !== 'MESCLADO')
      .map((chamado) => {
        const termosChamado = radicais(`${chamado.titulo} ${chamado.unidade.nome}`);
        const comuns = [...termosChamado].filter((t) => termos.has(t)).length;
        const base = comuns / Math.max(1, Math.min(termos.size, termosChamado.size));
        const score = base * 0.8 + (chamado.unidade.id === unidadeId ? 0.2 : 0);
        return { chamado, comuns, percentual: Math.min(99, Math.round(score * 100)) };
      })
      .filter((d) => d.comuns > 0 && d.percentual >= SIMILARIDADE_MINIMA * 100)
      .sort((a, b) => b.percentual - a.percentual)
      .slice(0, MAXIMO_DUPLICIDADES)
      .map(({ chamado, percentual }) => ({ chamado, percentual }));
  });

  /**
   * Instante em que a tela abriu (métrica M11: tempo de registro manual). Vai no payload e não
   * aparece para o usuário. Cada acesso a /chamados/novo cria a tela de novo, com nova medição.
   */
  private registroIniciadoEm = new Date();

  ngOnInit(): void {
    this.registroIniciadoEm = new Date();
    this.carregarUnidades();
    this.carregarListas();
    this.chamadoService.listar().subscribe({
      next: (lista) => this.chamadosExistentes.set(lista),
      error: () => this.chamadosExistentes.set([]),
    });
  }

  protected carregarListas(): void {
    this.carregandoListas.set(true);
    this.erroListas.set(false);
    forkJoin({
      categorias: this.listasChamadoService.listarCategorias(),
      sistemas: this.listasChamadoService.listarSistemasEquipamentos(),
    })
      .pipe(finalize(() => this.carregandoListas.set(false)))
      .subscribe({
        next: ({ categorias, sistemas }) => {
          this.categorias.set(categorias);
          this.sistemas.set(sistemas);
        },
        error: () => this.erroListas.set(true),
      });
  }

  protected carregarUnidades(): void {
    const controle = this.form.controls.unidadeId;
    controle.disable();
    this.carregandoUnidades.set(true);
    this.erroUnidades.set(false);
    this.unidadeService
      .listarAtivas()
      .pipe(finalize(() => this.carregandoUnidades.set(false)))
      .subscribe({
        next: (lista) => {
          const ordenadas = [...lista].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
          this.unidades.set(ordenadas);
          if (ordenadas.length) {
            controle.enable();
            if (!this.form.controls.unidadeId.value) {
              this.form.controls.unidadeId.setValue(ordenadas[0].id);
            }
          }
        },
        error: () => this.erroUnidades.set(true),
      });
  }

  protected selecionarCanal(canal: CanalOrigem): void {
    this.form.controls.canalOrigem.setValue(canal);
  }

  protected selecionarPrioridade(prioridade: OpcaoPrioridade): void {
    this.form.controls.prioridade.setValue(prioridade);
  }

  protected alternarVinculo(id: string): void {
    this.vinculadoId.update((atual) => (atual === id ? null : id));
  }

  protected aoSelecionarArquivos(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    this.adicionarArquivos(input.files);
    input.value = '';
  }

  protected aoArrastar(evento: DragEvent, ativo: boolean): void {
    evento.preventDefault();
    this.arrastando.set(ativo);
  }

  protected aoSoltar(evento: DragEvent): void {
    evento.preventDefault();
    this.arrastando.set(false);
    this.adicionarArquivos(evento.dataTransfer?.files ?? null);
  }

  protected removerAnexo(id: string): void {
    this.anexos.update((lista) => lista.filter((a) => a.id !== id));
  }

  private adicionarArquivos(arquivos: FileList | null): void {
    if (!arquivos?.length) return;
    this.erroAnexo.set(null);
    const validos: AnexoItem[] = [];
    for (const arquivo of Array.from(arquivos)) {
      if (!TIPOS_ANEXO.includes(arquivo.type)) {
        this.erroAnexo.set(`"${arquivo.name}" não é um arquivo PNG, JPG ou PDF válido.`);
        continue;
      }
      if (arquivo.size > TAMANHO_MAXIMO_ANEXO) {
        this.erroAnexo.set(`"${arquivo.name}" ultrapassa o limite de 10 MB.`);
        continue;
      }
      validos.push({
        id: crypto.randomUUID(),
        arquivo,
        nome: arquivo.name,
        tamanhoFormatado: this.formatarTamanho(arquivo.size),
      });
    }
    this.anexos.update((lista) => [...lista, ...validos]);
  }

  private formatarTamanho(bytes: number): string {
    if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1).replace('.', ',')} MB`;
  }

  protected estiloStatus(status: StatusChamado): string {
    const estilos: Record<StatusChamado, string> = {
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
    return estilos[status] ?? 'bg-[#e2e5ec] text-[#363b47]';
  }

  protected rotuloStatus(status: StatusChamado): string {
    const labels: Record<StatusChamado, string> = {
      ABERTO: 'Aberto',
      EM_TRIAGEM: 'Em Triagem',
      EM_ATENDIMENTO: 'Em Atendimento',
      AGUARDANDO: 'Aguardando',
      RESOLVIDO: 'Resolvido',
      REABERTO: 'Reaberto',
      FECHADO: 'Fechado',
      CANCELADO: 'Cancelado',
      MESCLADO: 'Mesclado',
    };
    return labels[status] ?? status;
  }

  protected invalido(campo: CampoChamado): boolean {
    const control = this.form.controls[campo];
    return control.invalid && (control.touched || control.dirty);
  }

  protected registrar(): void {
    this.errosApi.set([]);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valor = this.form.getRawValue();
    const nome = valor.nome.trim();
    const vinculado = this.vinculadoId();

    const payload: ChamadoRequest = {
      unidade_id: valor.unidadeId,
      canal_origem: valor.canalOrigem,
      solicitante: {
        ...(nome ? { nome } : {}),
        contato: normalizarContato(valor.contato),
      },
      descricao: valor.descricao.trim(),
      categoria_id: valor.categoriaId,
      ...(valor.sistemaId ? { sistema_equipamento_id: valor.sistemaId } : {}),
      ...(valor.prioridade !== 'TRIAGEM' ? { prioridade: valor.prioridade } : {}),
      atribuir_a_mim: valor.atribuirAMim,
      ...(vinculado ? { chamado_relacionado_id: vinculado } : {}),
      registro_iniciado_em: this.registroIniciadoEm.toISOString(),
    };

    this.enviando.set(true);
    this.chamadoService
      .registrar(payload)
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: (chamado) => {
          // Dado de teste ou simulado nunca pode parecer real (seção 9.1): mostra a origem no aviso.
          const origem =
            chamado.origem_dado && chamado.origem_dado !== 'REAL'
              ? ` (dado ${chamado.origem_dado})`
              : '';
          this.toast.sucesso(`Chamado ${chamado.codigo} registrado com sucesso${origem}.`);
          this.router.navigate(['/chamados']);
        },
        error: (erro) => this.errosApi.set(extrairMensagensErro(erro)),
      });
  }

  protected cancelar(): void {
    this.form.reset({
      canalOrigem: 'LIGACAO',
      unidadeId: this.unidades()[0]?.id ?? '',
      contato: '',
      nome: '',
      categoriaId: '',
      sistemaId: '',
      descricao: '',
      prioridade: 'TRIAGEM',
      atribuirAMim: true,
    });
    this.vinculadoId.set(null);
    this.anexos.set([]);
    this.errosApi.set([]);
    this.router.navigate(['/chamados']);
  }
}
