import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ToastService } from '../../../core/services/toast.service';
import { UnidadeService } from '../../../core/services/unidade.service';
import { Modal } from '../../../shared/components/modal/modal';
import { Unidade, UnidadeRequest } from '../../../shared/models/unidade.model';
import { extrairMensagensErro } from '../../../shared/utils/api-error';

type FiltroSituacao = 'TODAS' | 'ATIVAS' | 'INATIVAS';

@Component({
  selector: 'app-unidades',
  imports: [ReactiveFormsModule, Modal],
  templateUrl: './unidades.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Unidades implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly unidadeService = inject(UnidadeService);
  private readonly toast = inject(ToastService);

  // Listagem
  protected readonly unidades = signal<Unidade[]>([]);
  protected readonly carregando = signal(true);
  protected readonly erroCarregamento = signal(false);
  protected readonly busca = signal('');
  protected readonly filtroSituacao = signal<FiltroSituacao>('TODAS');

  protected readonly totais = computed(() => {
    const lista = this.unidades();
    const ativas = lista.filter((u) => u.ativo).length;
    return { total: lista.length, ativas, inativas: lista.length - ativas };
  });

  protected readonly unidadesFiltradas = computed(() => {
    const termo = this.normalizar(this.busca());
    const situacao = this.filtroSituacao();
    return this.unidades()
      .filter((u) => situacao === 'TODAS' || (situacao === 'ATIVAS') === u.ativo)
      .filter(
        (u) =>
          !termo ||
          this.normalizar(u.nome).includes(termo) ||
          this.normalizar(u.codigo).includes(termo),
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
    codigo: [
      '',
      [Validators.required, Validators.maxLength(20), Validators.pattern(/^[A-Za-z0-9-]+$/)],
    ],
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
    this.unidadeService
      .listar()
      .pipe(finalize(() => this.carregando.set(false)))
      .subscribe({
        next: (lista) => this.unidades.set(lista),
        error: () => this.erroCarregamento.set(true),
      });
  }

  protected alterarBusca(evento: Event): void {
    this.busca.set((evento.target as HTMLInputElement).value);
  }

  protected alterarFiltro(evento: Event): void {
    this.filtroSituacao.set((evento.target as HTMLSelectElement).value as FiltroSituacao);
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
    this.form.reset({ nome: unidade.nome, codigo: unidade.codigo });
    this.errosForm.set([]);
    this.modalFormAberto.set(true);
  }

  protected fecharForm(): void {
    if (!this.salvando()) this.modalFormAberto.set(false);
  }

  protected invalido(campo: 'nome' | 'codigo'): boolean {
    const control = this.form.controls[campo];
    return control.invalid && (control.touched || control.dirty);
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
      codigo: valor.codigo.trim().toUpperCase(),
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

  protected confirmarDesativacao(unidade: Unidade): void {
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

  private normalizar(texto: string): string {
    return texto
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .trim();
  }
}
