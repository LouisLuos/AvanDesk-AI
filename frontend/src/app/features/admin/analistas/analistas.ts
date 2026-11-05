import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, finalize, forkJoin, map, of } from 'rxjs';
import { AnalistaService } from '../../../core/services/analista.service';
import { ChamadoService } from '../../../core/services/chamado.service';
import { ToastService } from '../../../core/services/toast.service';
import {
  CustomDropdown,
  OpcaoDropdown,
} from '../../../shared/components/custom-dropdown/custom-dropdown';
import { Modal } from '../../../shared/components/modal/modal';
import { Analista, AnalistaRequest } from '../../../shared/models/analista.model';
import { extrairMensagensErro } from '../../../shared/utils/api-error';
import { ESTILO_SELO_NEUTRO, ESTILO_SELO_POSITIVO, classeCampo } from '../../../shared/utils/estilos';
import { iniciais } from '../../../shared/utils/iniciais';

type FiltroSituacao = '' | 'ATIVOS' | 'INATIVOS';
type CampoTexto = 'nome' | 'email';

/** Senha temporária do último cadastro, exibida uma única vez no modal. */
interface SenhaGerada {
  nome: string;
  senha: string;
}

@Component({
  selector: 'app-analistas',
  imports: [ReactiveFormsModule, Modal, CustomDropdown],
  templateUrl: './analistas.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Analistas implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly analistaService = inject(AnalistaService);
  private readonly chamadoService = inject(ChamadoService);
  private readonly toast = inject(ToastService);

  protected readonly linhasEsqueleto = [1, 2, 3, 4, 5];
  protected readonly opcoesSituacao: OpcaoDropdown[] = [
    { valor: 'ATIVOS', rotulo: 'Ativos' },
    { valor: 'INATIVOS', rotulo: 'Inativos' },
  ];
  protected readonly estiloAtivo = ESTILO_SELO_POSITIVO;
  protected readonly estiloInativo = ESTILO_SELO_NEUTRO;
  protected readonly iniciais = iniciais;

  // Listagem
  protected readonly analistas = signal<Analista[]>([]);
  /** Chamados em andamento por analista; nulo quando a contagem não está disponível. */
  protected readonly chamadosPorAnalista = signal<ReadonlyMap<string, number> | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erroCarregamento = signal(false);
  protected readonly busca = signal('');
  protected readonly filtroSituacao = signal<FiltroSituacao>('');

  /** Ativos primeiro, depois por nome. */
  protected readonly analistasFiltrados = computed(() => {
    const termo = normalizar(this.busca());
    const situacao = this.filtroSituacao();
    return this.analistas()
      .filter((a) => !situacao || (situacao === 'ATIVOS') === a.ativo)
      .filter((a) => !termo || [a.nome, a.email].some((t) => normalizar(t).includes(termo)))
      .sort((a, b) => Number(b.ativo) - Number(a.ativo) || a.nome.localeCompare(b.nome, 'pt-BR'));
  });

  // Formulário (cadastro/edição)
  protected readonly modalFormAberto = signal(false);
  protected readonly analistaEmEdicao = signal<Analista | null>(null);
  protected readonly salvando = signal(false);
  protected readonly errosForm = signal<string[]>([]);
  protected readonly senhaGerada = signal<SenhaGerada | null>(null);

  protected readonly tituloModal = computed(() => {
    if (this.senhaGerada()) return 'Cadastro concluído';
    return this.analistaEmEdicao() ? 'Editar analista' : 'Novo analista';
  });

  protected readonly subtituloModal = computed(() => {
    const gerada = this.senhaGerada();
    if (gerada) return `Repasse a senha temporária de ${gerada.nome}. Ela não aparece de novo.`;
    return this.analistaEmEdicao()
      ? 'Mudar o e-mail também muda o login do analista.'
      : 'Analistas ativos podem assumir e atender chamados.';
  });

  protected readonly form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
  });

  // Desativação e reativação
  protected readonly analistaParaDesativar = signal<Analista | null>(null);
  protected readonly alterandoSituacao = signal(false);

  ngOnInit(): void {
    this.carregar();
  }

  protected carregar(): void {
    this.carregando.set(true);
    this.erroCarregamento.set(false);
    forkJoin({
      analistas: this.analistaService.listar(),
      totais: this.chamadoService.totaisPorResponsavel().pipe(
        map((totais) => new Map(totais.map((t) => [t.responsavel_id, t.total]))),
        // A contagem é complementar: se falhar, a coluna mostra "—" e a tela segue.
        catchError(() => of(null)),
      ),
    })
      .pipe(finalize(() => this.carregando.set(false)))
      .subscribe({
        next: ({ analistas, totais }) => {
          this.analistas.set(analistas);
          this.chamadosPorAnalista.set(totais);
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

  protected chamadosAtivos(analista: Analista): number | null {
    const contagem = this.chamadosPorAnalista();
    return contagem ? (contagem.get(analista.id) ?? 0) : null;
  }

  // ---------- Cadastro / edição ----------

  protected abrirCadastro(): void {
    this.analistaEmEdicao.set(null);
    this.senhaGerada.set(null);
    this.form.reset();
    this.errosForm.set([]);
    this.modalFormAberto.set(true);
  }

  protected abrirEdicao(analista: Analista): void {
    this.analistaEmEdicao.set(analista);
    this.senhaGerada.set(null);
    this.form.reset({ nome: analista.nome, email: analista.email });
    this.errosForm.set([]);
    this.modalFormAberto.set(true);
  }

  protected fecharForm(): void {
    if (this.salvando() || this.alterandoSituacao()) return;
    this.modalFormAberto.set(false);
    this.senhaGerada.set(null);
  }

  protected invalido(campo: CampoTexto): boolean {
    const control = this.form.controls[campo];
    return control.invalid && (control.touched || control.dirty);
  }

  protected classeCampo(campo: CampoTexto): string {
    return classeCampo(this.invalido(campo));
  }

  protected salvar(): void {
    this.errosForm.set([]);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valor = this.form.getRawValue();
    const payload: AnalistaRequest = {
      nome: valor.nome.trim(),
      email: valor.email.trim().toLowerCase(),
    };
    const emEdicao = this.analistaEmEdicao();
    this.salvando.set(true);

    if (emEdicao) {
      this.analistaService
        .editar(emEdicao.id, payload)
        .pipe(finalize(() => this.salvando.set(false)))
        .subscribe({
          next: (analista) => {
            this.substituirOuIncluir(analista);
            this.modalFormAberto.set(false);
            this.toast.sucesso(`Cadastro de ${analista.nome} atualizado.`);
          },
          error: (erro) => this.errosForm.set(extrairMensagensErro(erro)),
        });
      return;
    }

    this.analistaService
      .cadastrar(payload)
      .pipe(finalize(() => this.salvando.set(false)))
      .subscribe({
        next: ({ analista, senha_temporaria }) => {
          this.substituirOuIncluir(analista);
          this.senhaGerada.set({ nome: analista.nome, senha: senha_temporaria });
        },
        error: (erro) => this.errosForm.set(extrairMensagensErro(erro)),
      });
  }

  protected copiarSenha(): void {
    const senha = this.senhaGerada()?.senha;
    if (!senha) return;
    navigator.clipboard
      .writeText(senha)
      .then(() => this.toast.sucesso('Senha temporária copiada.'))
      .catch(() => this.toast.erro('Não foi possível copiar. Selecione a senha e copie manualmente.'));
  }

  // ---------- Desativação / reativação ----------

  /** Sai do formulário de edição e pede confirmação, para não empilhar dois modais. */
  protected solicitarDesativacao(): void {
    const analista = this.analistaEmEdicao();
    if (!analista) return;
    this.modalFormAberto.set(false);
    this.analistaParaDesativar.set(analista);
  }

  protected cancelarDesativacao(): void {
    if (!this.alterandoSituacao()) this.analistaParaDesativar.set(null);
  }

  protected desativar(): void {
    const analista = this.analistaParaDesativar();
    if (!analista) return;

    this.alterandoSituacao.set(true);
    this.analistaService
      .desativar(analista.id)
      .pipe(finalize(() => this.alterandoSituacao.set(false)))
      .subscribe({
        next: (atualizado) => {
          this.substituirOuIncluir(atualizado);
          this.analistaParaDesativar.set(null);
          this.toast.sucesso(`Acesso de ${atualizado.nome} desativado.`);
        },
        error: (erro) => this.toast.erro(extrairMensagensErro(erro).join(' ')),
      });
  }

  protected reativar(): void {
    const analista = this.analistaEmEdicao();
    if (!analista) return;

    this.errosForm.set([]);
    this.alterandoSituacao.set(true);
    this.analistaService
      .reativar(analista.id)
      .pipe(finalize(() => this.alterandoSituacao.set(false)))
      .subscribe({
        next: (atualizado) => {
          this.substituirOuIncluir(atualizado);
          this.modalFormAberto.set(false);
          this.toast.sucesso(`Acesso de ${atualizado.nome} reativado.`);
        },
        error: (erro) => this.errosForm.set(extrairMensagensErro(erro)),
      });
  }

  // ---------- Helpers ----------

  private substituirOuIncluir(analista: Analista): void {
    this.analistas.update((lista) => {
      const existe = lista.some((a) => a.id === analista.id);
      return existe ? lista.map((a) => (a.id === analista.id ? analista : a)) : [...lista, analista];
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
