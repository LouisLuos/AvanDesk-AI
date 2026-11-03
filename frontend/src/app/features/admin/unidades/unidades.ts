import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, finalize, forkJoin, map, of } from 'rxjs';
import { ChamadoService } from '../../../core/services/chamado.service';
import { ToastService } from '../../../core/services/toast.service';
import { UnidadeService } from '../../../core/services/unidade.service';
import {
  CustomDropdown,
  OpcaoDropdown,
} from '../../../shared/components/custom-dropdown/custom-dropdown';
import { Modal } from '../../../shared/components/modal/modal';
import { Unidade, UnidadeRequest } from '../../../shared/models/unidade.model';
import { extrairMensagensErro } from '../../../shared/utils/api-error';

type FiltroSituacao = '' | 'ATIVAS' | 'INATIVAS';
type CampoTexto = 'nome' | 'identificacao' | 'localizacao';

/** Janela da coluna "Chamados (30 d)". */
const DIAS_CONTAGEM = 30;

const CLASSE_CAMPO =
  'h-9 w-full rounded-lg border bg-white px-3 text-sm text-[#14161b] shadow-2xs transition placeholder:text-[#9aa2b1] focus:outline-none focus:ring-2';
const CAMPO_VALIDO = 'border-[#e2e5ec] focus:border-[#2d5bff] focus:ring-[#e8edff]';
const CAMPO_INVALIDO = 'border-[#e5484d] focus:border-[#e5484d] focus:ring-[#e5484d]/15';

@Component({
  selector: 'app-unidades',
  imports: [ReactiveFormsModule, Modal, CustomDropdown],
  templateUrl: './unidades.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Unidades implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly unidadeService = inject(UnidadeService);
  private readonly chamadoService = inject(ChamadoService);
  private readonly toast = inject(ToastService);

  protected readonly diasContagem = DIAS_CONTAGEM;
  protected readonly linhasEsqueleto = [1, 2, 3, 4, 5];
  protected readonly opcoesSituacao: OpcaoDropdown[] = [
    { valor: 'ATIVAS', rotulo: 'Ativas' },
    { valor: 'INATIVAS', rotulo: 'Inativas' },
  ];
  protected readonly estiloSim = 'bg-[#22c55e]/10 text-[#16a34a]';
  protected readonly estiloNao = 'bg-[#f1f3f8] text-[#6b7280]';

  // Listagem
  protected readonly unidades = signal<Unidade[]>([]);
  /** Chamados por unidade na janela; nulo quando a contagem não está disponível. */
  protected readonly chamadosPorUnidade = signal<ReadonlyMap<string, number> | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erroCarregamento = signal(false);
  protected readonly busca = signal('');
  protected readonly filtroSituacao = signal<FiltroSituacao>('');

  protected readonly subtitulo = computed(() => {
    const total = this.unidades().length;
    const ativas = this.unidades().filter((u) => u.ativo).length;
    return `${total} ${total === 1 ? 'unidade cadastrada' : 'unidades cadastradas'}, ${ativas} ${ativas === 1 ? 'ativa' : 'ativas'}.`;
  });

  /** Ativas primeiro, depois por nome. */
  protected readonly unidadesFiltradas = computed(() => {
    const termo = normalizar(this.busca());
    const situacao = this.filtroSituacao();
    return this.unidades()
      .filter((u) => !situacao || (situacao === 'ATIVAS') === u.ativo)
      .filter(
        (u) =>
          !termo || [u.nome, u.identificacao, u.localizacao].some((t) => normalizar(t).includes(termo)),
      )
      .sort((a, b) => Number(b.ativo) - Number(a.ativo) || a.nome.localeCompare(b.nome, 'pt-BR'));
  });

  // Formulário (cadastro/edição)
  protected readonly modalFormAberto = signal(false);
  protected readonly unidadeEmEdicao = signal<Unidade | null>(null);
  protected readonly salvando = signal(false);
  protected readonly errosForm = signal<string[]>([]);

  protected readonly form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    identificacao: [
      '',
      [Validators.required, Validators.maxLength(20), Validators.pattern(/^[A-Za-z0-9-]+$/)],
    ],
    localizacao: ['', [Validators.maxLength(255)]],
    atendimento_24h: [false],
  });

  // Desativação
  protected readonly unidadeParaDesativar = signal<Unidade | null>(null);
  protected readonly desativando = signal(false);

  ngOnInit(): void {
    this.carregar();
  }

  protected carregar(): void {
    this.carregando.set(true);
    this.erroCarregamento.set(false);
    forkJoin({
      unidades: this.unidadeService.listar(),
      totais: this.chamadoService.totaisPorUnidade(DIAS_CONTAGEM).pipe(
        map((totais) => new Map(totais.map((t) => [t.unidade_id, t.total]))),
        // A contagem é complementar: se falhar, a coluna mostra "—" e a tela segue.
        catchError(() => of(null)),
      ),
    })
      .pipe(finalize(() => this.carregando.set(false)))
      .subscribe({
        next: ({ unidades, totais }) => {
          this.unidades.set(unidades);
          this.chamadosPorUnidade.set(totais);
        },
        error: () => this.erroCarregamento.set(true),
      });
  }

  protected alterarBusca(evento: Event): void {
    this.busca.set((evento.target as HTMLInputElement).value);
  }

  protected alterarSituacao(valor: string): void {
    this.filtroSituacao.set(valor as FiltroSituacao);
  }

  protected chamadosNaJanela(unidade: Unidade): number | null {
    const contagem = this.chamadosPorUnidade();
    return contagem ? (contagem.get(unidade.id) ?? 0) : null;
  }

  // ---------- Cadastro / edição ----------

  protected abrirCadastro(): void {
    this.unidadeEmEdicao.set(null);
    this.form.reset();
    this.errosForm.set([]);
    this.modalFormAberto.set(true);
  }

  protected abrirEdicao(unidade: Unidade): void {
    this.unidadeEmEdicao.set(unidade);
    this.form.reset({
      nome: unidade.nome,
      identificacao: unidade.identificacao,
      localizacao: unidade.localizacao ?? '',
      atendimento_24h: unidade.atendimento_24h,
    });
    this.errosForm.set([]);
    this.modalFormAberto.set(true);
  }

  protected fecharForm(): void {
    if (!this.salvando()) this.modalFormAberto.set(false);
  }

  protected invalido(campo: CampoTexto): boolean {
    const control = this.form.controls[campo];
    return control.invalid && (control.touched || control.dirty);
  }

  protected classeCampo(campo: CampoTexto, extra = ''): string {
    return `${CLASSE_CAMPO} ${this.invalido(campo) ? CAMPO_INVALIDO : CAMPO_VALIDO} ${extra}`;
  }

  protected salvar(): void {
    this.errosForm.set([]);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valor = this.form.getRawValue();
    const payload: UnidadeRequest = {
      nome: valor.nome.trim(),
      identificacao: valor.identificacao.trim().toUpperCase(),
      localizacao: valor.localizacao.trim(),
      atendimento_24h: valor.atendimento_24h,
    };
    const emEdicao = this.unidadeEmEdicao();
    const requisicao = emEdicao
      ? this.unidadeService.editar(emEdicao.id, payload)
      : this.unidadeService.cadastrar(payload);

    this.salvando.set(true);
    requisicao.pipe(finalize(() => this.salvando.set(false))).subscribe({
      next: (unidade) => {
        this.substituirOuIncluir(unidade);
        this.modalFormAberto.set(false);
        this.toast.sucesso(
          emEdicao ? `Unidade "${unidade.nome}" atualizada.` : `Unidade "${unidade.nome}" cadastrada.`,
        );
      },
      error: (erro) => this.errosForm.set(extrairMensagensErro(erro)),
    });
  }

  // ---------- Desativação ----------

  /** Sai do formulário de edição e pede confirmação, para não empilhar dois modais. */
  protected solicitarDesativacao(): void {
    const unidade = this.unidadeEmEdicao();
    if (!unidade) return;
    this.modalFormAberto.set(false);
    this.unidadeParaDesativar.set(unidade);
  }

  protected cancelarDesativacao(): void {
    if (!this.desativando()) this.unidadeParaDesativar.set(null);
  }

  protected desativar(): void {
    const unidade = this.unidadeParaDesativar();
    if (!unidade) return;

    this.desativando.set(true);
    this.unidadeService
      .desativar(unidade.id)
      .pipe(finalize(() => this.desativando.set(false)))
      .subscribe({
        next: (atualizada) => {
          this.substituirOuIncluir(atualizada);
          this.unidadeParaDesativar.set(null);
          this.toast.sucesso(`Unidade "${atualizada.nome}" desativada.`);
        },
        error: (erro) => this.toast.erro(extrairMensagensErro(erro).join(' ')),
      });
  }

  // ---------- Helpers ----------

  private substituirOuIncluir(unidade: Unidade): void {
    this.unidades.update((lista) => {
      const existe = lista.some((u) => u.id === unidade.id);
      return existe ? lista.map((u) => (u.id === unidade.id ? unidade : u)) : [...lista, unidade];
    });
  }
}

function normalizar(texto?: string | null): string {
  return (texto ?? '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .trim();
}
