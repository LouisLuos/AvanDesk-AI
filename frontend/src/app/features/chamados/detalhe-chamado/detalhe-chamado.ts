import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService, UsuarioAutenticado } from '../../../core/services/auth.service';
import { ChamadoService } from '../../../core/services/chamado.service';
import { ToastService } from '../../../core/services/toast.service';
import { UsuarioService } from '../../../core/services/usuario.service';
import { Modal } from '../../../shared/components/modal/modal';
import {
  CanalOrigem,
  ChamadoDetalhe,
  DirecaoContato,
  MotivoEspera,
  PrioridadeChamado,
  StatusChamado,
} from '../../../shared/models/chamado.model';
import { extrairMensagensErro } from '../../../shared/utils/api-error';

@Component({
  selector: 'app-detalhe-chamado',
  imports: [RouterLink, Modal],
  templateUrl: './detalhe-chamado.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DetalheChamado implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly chamadoService = inject(ChamadoService);
  private readonly authService = inject(AuthService);
  private readonly usuarioService = inject(UsuarioService);
  private readonly toast = inject(ToastService);

  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);
  protected readonly chamado = signal<ChamadoDetalhe | null>(null);

  // Atribuição de responsável (HU12)
  protected readonly atribuindo = signal(false);
  protected readonly modalAtribuirAberto = signal(false);
  protected readonly analistas = signal<UsuarioAutenticado[]>([]);
  protected readonly carregandoAnalistas = signal(false);
  protected readonly analistaSelecionadoId = signal<string>('');

  protected readonly idChamado = computed(() => this.route.snapshot.paramMap.get('id') ?? '');
  protected readonly usuarioLogado = computed(() => this.authService.usuario());

  /** Chamados encerrados não podem receber nova atribuição (RN HU12). */
  protected readonly chamadoEncerrado = computed(() => {
    const status = this.chamado()?.status;
    return status === 'FECHADO' || status === 'CANCELADO' || status === 'MESCLADO';
  });

  /** Se o chamado já está atribuído ao usuário atualmente autenticado. */
  protected readonly ehResponsavelAtual = computed(() => {
    const resp = this.chamado()?.responsavel;
    const usr = this.usuarioLogado();
    return !!resp && !!usr && resp.id === usr.id;
  });

  /** Pode assumir o chamado com 1 clique para si mesmo. */
  protected readonly podeAssumir = computed(() => {
    const ch = this.chamado();
    const usr = this.usuarioLogado();
    if (!ch || !usr || this.chamadoEncerrado()) return false;
    return ch.responsavel?.id !== usr.id;
  });

  /** Pode atribuir ou reatribuir o chamado a qualquer analista ativo. */
  protected readonly podeAtribuir = computed(() => {
    const ch = this.chamado();
    return !!ch && !this.chamadoEncerrado();
  });

  ngOnInit(): void {
    this.carregarDetalhes();
  }

  protected carregarDetalhes(): void {
    const id = this.idChamado();
    if (!id) {
      this.erro.set('Identificador do chamado não fornecido.');
      this.carregando.set(false);
      return;
    }

    this.carregando.set(true);
    this.erro.set(null);

    this.chamadoService
      .buscarPorId(id)
      .pipe(finalize(() => this.carregando.set(false)))
      .subscribe({
        next: (dados) => this.chamado.set(dados),
        error: (err) => {
          const msgApi = extrairMensagensErro(err)[0];
          const msgPadrao = 'Não foi possível carregar os detalhes do chamado.';
          this.erro.set(
            msgApi && msgApi !== 'Ocorreu um erro inesperado. Tente novamente.'
              ? msgApi
              : msgPadrao,
          );
        },
      });
  }

  /**
   * Assume o chamado atribuindo-o ao analista atualmente autenticado (HU12).
   */
  protected assumirChamado(): void {
    const usr = this.usuarioLogado();
    const ch = this.chamado();
    if (!usr) {
      this.toast.erro('Você precisa estar autenticado para assumir este chamado.');
      return;
    }
    if (!ch || this.chamadoEncerrado()) {
      this.toast.erro('Este chamado não pode receber atribuição.');
      return;
    }

    this.atribuindo.set(true);
    this.chamadoService
      .atribuirResponsavel(ch.id, usr.id)
      .pipe(finalize(() => this.atribuindo.set(false)))
      .subscribe({
        next: (atualizado) => {
          this.chamado.set(atualizado);
          this.toast.sucesso('Chamado assumido com sucesso!');
        },
        error: (err) => {
          this.toast.erro(extrairMensagensErro(err)[0]);
        },
      });
  }

  protected abrirModalAtribuir(): void {
    this.modalAtribuirAberto.set(true);
    const atualId = this.chamado()?.responsavel?.id ?? '';
    this.analistaSelecionadoId.set(atualId);
    this.carregarAnalistas();
  }

  protected fecharModalAtribuir(): void {
    this.modalAtribuirAberto.set(false);
  }

  protected selecionarAnalista(id: string): void {
    this.analistaSelecionadoId.set(id);
  }

  protected confirmarAtribuicao(): void {
    const responsavelId = this.analistaSelecionadoId();
    const ch = this.chamado();
    if (!responsavelId) {
      this.toast.erro('Selecione um analista de suporte.');
      return;
    }
    if (!ch || this.chamadoEncerrado()) {
      this.toast.erro('Este chamado não pode receber atribuição.');
      return;
    }
    if (ch.responsavel?.id === responsavelId) {
      this.toast.info('O chamado já está atribuído a este analista.');
      this.fecharModalAtribuir();
      return;
    }

    this.atribuindo.set(true);
    this.chamadoService
      .atribuirResponsavel(ch.id, responsavelId)
      .pipe(finalize(() => this.atribuindo.set(false)))
      .subscribe({
        next: (atualizado) => {
          this.chamado.set(atualizado);
          this.fecharModalAtribuir();
          this.toast.sucesso('Responsável atribuído com sucesso!');
        },
        error: (err) => {
          this.toast.erro(extrairMensagensErro(err)[0]);
        },
      });
  }

  private carregarAnalistas(): void {
    this.carregandoAnalistas.set(true);
    this.usuarioService
      .listarAnalistas()
      .pipe(finalize(() => this.carregandoAnalistas.set(false)))
      .subscribe({
        next: (lista) => {
          this.analistas.set(lista);
          if (!this.analistaSelecionadoId() && lista.length) {
            this.analistaSelecionadoId.set(lista[0].id);
          }
        },
        error: () => {
          const fallback: UsuarioAutenticado[] = [
            { id: '00000000-0000-0000-0000-0000000000a1', nome: 'Analista Dev Um', email: 'analista1@lumen.local', perfil: 'ANALISTA_SUPORTE' },
            { id: '00000000-0000-0000-0000-0000000000a2', nome: 'Analista Dev Dois', email: 'analista2@lumen.local', perfil: 'ANALISTA_SUPORTE' },
          ];
          this.analistas.set(fallback);
          if (!this.analistaSelecionadoId() && fallback.length) {
            this.analistaSelecionadoId.set(fallback[0].id);
          }
        },
      });
  }

  protected acaoNaoDisponivel(acao: string, leva: string): void {
    this.toast.info(`Ação "${acao}" planejada para a ${leva}.`);
  }

  protected rotuloStatus(status: StatusChamado): string {
    const rotulos: Record<StatusChamado, string> = {
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
    return rotulos[status] ?? status;
  }

  protected estiloStatus(status: StatusChamado): string {
    const estilos: Record<StatusChamado, string> = {
      ABERTO: 'bg-[#ff6a3d]/10 text-[#ff6a3d] border-[#ff6a3d]/20',
      EM_TRIAGEM: 'bg-[#14b8a6]/10 text-[#14b8a6] border-[#14b8a6]/20',
      EM_ATENDIMENTO: 'bg-[#0ea5e9]/10 text-[#0ea5e9] border-[#0ea5e9]/20',
      AGUARDANDO: 'bg-[#f0a202]/10 text-[#f0a202] border-[#f0a202]/20',
      RESOLVIDO: 'bg-[#22c55e]/10 text-[#22c55e] border-[#22c55e]/20',
      REABERTO: 'bg-[#e5484d]/10 text-[#e5484d] border-[#e5484d]/20',
      FECHADO: 'bg-[#64748b]/10 text-[#64748b] border-[#64748b]/20',
      CANCELADO: 'bg-[#94a3b8]/15 text-[#64748b] border-[#94a3b8]/30',
      MESCLADO: 'bg-[#8b5cf6]/10 text-[#8b5cf6] border-[#8b5cf6]/20',
    };
    return estilos[status] ?? 'bg-[#e2e5ec] text-[#363b47] border-transparent';
  }

  protected rotuloMotivoEspera(motivo: MotivoEspera): string {
    const rotulos: Record<MotivoEspera, string> = {
      FORNECEDOR: 'Fornecedor externo',
      SOLICITANTE: 'Solicitante',
      PECA_EQUIPAMENTO: 'Peça ou equipamento',
    };
    return rotulos[motivo] ?? motivo;
  }

  protected rotuloDirecaoContato(direcao: DirecaoContato): string {
    return direcao === 'ENVIADO' ? 'Enviado pelo suporte' : 'Recebido do solicitante';
  }

  protected rotuloPrioridade(prioridade: PrioridadeChamado): string {
    const rotulos: Record<PrioridadeChamado, string> = {
      ALTA: 'Alta · P1',
      MEDIA: 'Média · P2',
      BAIXA: 'Baixa · P3',
    };
    return rotulos[prioridade] ?? prioridade;
  }

  protected estiloPrioridade(prioridade: PrioridadeChamado): string {
    const estilos: Record<PrioridadeChamado, string> = {
      ALTA: 'bg-[#dc2626] text-white',
      MEDIA: 'bg-[#f59e0b] text-white',
      BAIXA: 'bg-[#94a3b8] text-white',
    };
    return estilos[prioridade] ?? 'bg-[#64748b] text-white';
  }

  protected rotuloCanal(canal: CanalOrigem): string {
    const rotulos: Record<CanalOrigem, string> = {
      LIGACAO: 'Ligação',
      WHATSAPP: 'WhatsApp',
      EMAIL: 'E-mail',
      PRESENCIAL: 'Presencial',
    };
    return rotulos[canal] ?? canal;
  }

  protected rotuloTipoContato(contato: string): string {
    return contato?.includes('@') ? 'E-mail' : 'Telefone';
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
}
