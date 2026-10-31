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
import { AuthService } from '../../../core/services/auth.service';
import { ChamadoService } from '../../../core/services/chamado.service';
import { ToastService } from '../../../core/services/toast.service';
import {
  CanalOrigem,
  ChamadoDetalhe,
  DirecaoContato,
  MotivoEspera,
  PrioridadeChamado,
  StatusChamado,
  StatusRequest,
} from '../../../shared/models/chamado.model';
import { extrairMensagensErro } from '../../../shared/utils/api-error';

@Component({
  selector: 'app-detalhe-chamado',
  imports: [RouterLink],
  templateUrl: './detalhe-chamado.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DetalheChamado implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly chamadoService = inject(ChamadoService);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly carregando = signal(true);
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly chamado = signal<ChamadoDetalhe | null>(null);

  // Estados do Modal de Alteração de Status (HU12)
  protected readonly modalStatusAberto = signal(false);
  protected readonly statusSelecionado = signal<StatusChamado | null>(null);
  protected readonly motivoEsperaSelecionado = signal<MotivoEspera>('SOLICITANTE');
  protected readonly motivoCancelamentoTexto = signal('');

  protected readonly idChamado = computed(() => this.route.snapshot.paramMap.get('id') ?? '');
  protected readonly usuarioLogado = computed(() => this.authService.usuario());

  protected readonly ehResponsavelAtual = computed(() => {
    const resp = this.chamado()?.responsavel;
    const u = this.usuarioLogado();
    return !!(resp && u && resp.id === u.id);
  });

  protected readonly chamadoEncerrado = computed(() => {
    const s = this.chamado()?.status;
    return s === 'FECHADO' || s === 'CANCELADO' || s === 'MESCLADO';
  });

  protected readonly proximosStatusDisponiveis = computed<{ valor: StatusChamado; rotulo: string }[]>(() => {
    const s = this.chamado()?.status;
    if (!s) return [];
    let proximos: StatusChamado[] = [];
    switch (s) {
      case 'ABERTO':
        proximos = ['EM_TRIAGEM', 'CANCELADO'];
        break;
      case 'EM_TRIAGEM':
        proximos = ['EM_ATENDIMENTO', 'CANCELADO'];
        break;
      case 'EM_ATENDIMENTO':
        proximos = ['AGUARDANDO'];
        break;
      case 'AGUARDANDO':
        proximos = ['EM_ATENDIMENTO'];
        break;
      case 'REABERTO':
        proximos = ['EM_ATENDIMENTO'];
        break;
      default:
        proximos = [];
    }
    return proximos.map((st) => ({
      valor: st,
      rotulo: this.rotuloStatus(st),
    }));
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
   * Assume a responsabilidade do chamado para o analista logado (HU12).
   */
  protected assumirChamado(): void {
    const u = this.usuarioLogado();
    if (!u) {
      this.toast.erro('Você precisa estar autenticado para assumir um chamado.');
      return;
    }

    const id = this.idChamado();
    if (!id) return;

    this.salvando.set(true);
    this.chamadoService
      .atribuirResponsavel(id, u.id)
      .pipe(finalize(() => this.salvando.set(false)))
      .subscribe({
        next: (dadosAtualizados) => {
          this.chamado.set(dadosAtualizados);
          this.toast.sucesso('Você assumiu o chamado com sucesso!');
        },
        error: (err) => {
          const msgApi = extrairMensagensErro(err)[0] ?? 'Erro ao assumir o chamado.';
          this.toast.erro(msgApi);
        },
      });
  }

  /**
   * Abre o modal para transição de status (HU12).
   */
  protected abrirModalStatus(): void {
    const opcoes = this.proximosStatusDisponiveis();
    if (opcoes.length === 0) {
      this.toast.info('Não há alterações de status disponíveis para o estado atual deste chamado.');
      return;
    }

    this.statusSelecionado.set(opcoes[0].valor);
    this.motivoEsperaSelecionado.set('SOLICITANTE');
    this.motivoCancelamentoTexto.set('');
    this.modalStatusAberto.set(true);
  }

  protected fecharModalStatus(): void {
    this.modalStatusAberto.set(false);
  }

  protected selecionarStatus(status: StatusChamado): void {
    this.statusSelecionado.set(status);
  }

  protected selecionarMotivoEspera(motivo: MotivoEspera): void {
    this.motivoEsperaSelecionado.set(motivo);
  }

  protected atualizarMotivoCancelamento(texto: string): void {
    this.motivoCancelamentoTexto.set(texto);
  }

  /**
   * Confirma a transição de status via PATCH /chamados/{id}/status (HU12).
   */
  protected confirmarAlteracaoStatus(): void {
    const novoStatus = this.statusSelecionado();
    if (!novoStatus) return;

    const id = this.idChamado();
    if (!id) return;

    if (novoStatus === 'EM_ATENDIMENTO' && this.chamado()?.status === 'EM_TRIAGEM' && !this.chamado()?.responsavel) {
      this.toast.erro('Atribua um responsável antes de iniciar o atendimento.');
      return;
    }

    if (novoStatus === 'CANCELADO' && !this.motivoCancelamentoTexto().trim()) {
      this.toast.erro('Informe uma justificativa para o cancelamento.');
      return;
    }

    const motivo = novoStatus === 'AGUARDANDO' ? this.motivoEsperaSelecionado() : null;
    const cancelamento = novoStatus === 'CANCELADO' ? this.motivoCancelamentoTexto().trim() : null;

    const payload: StatusRequest = {
      status: novoStatus,
      motivo_espera: motivo,
      motivoEspera: motivo,
      motivo_cancelamento: cancelamento,
      motivoCancelamento: cancelamento,
    };

    this.salvando.set(true);
    this.chamadoService
      .alterarStatus(id, payload)
      .pipe(finalize(() => this.salvando.set(false)))
      .subscribe({
        next: (dadosAtualizados) => {
          this.chamado.set(dadosAtualizados);
          this.toast.sucesso(`Status alterado para "${this.rotuloStatus(novoStatus)}" com sucesso!`);
          this.fecharModalStatus();
        },
        error: (err) => {
          const msgApi = extrairMensagensErro(err)[0] ?? 'Erro ao atualizar status.';
          this.toast.erro(msgApi);
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
