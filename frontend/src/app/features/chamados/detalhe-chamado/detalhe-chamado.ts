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
  CANAIS_ORIGEM,
  CanalOrigem,
  ChamadoDetalhe,
  DirecaoContato,
  EscalonamentoRequest,
  MotivoEspera,
  PrioridadeChamado,
  ResolucaoRequest,
  StatusChamado,
  RegistroContatoRequest,
  RetornoFornecedorRequest,
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

  // Estados do Modal de Resolução (HU06)
  protected readonly modalResolucaoAberto = signal(false);
  protected readonly resolucaoTexto = signal('');
  protected readonly causaRaizTexto = signal('');
  protected readonly possivelRecorrenciaCheck = signal(false);
  protected readonly resolucaoTocou = signal(false);

  protected readonly sugestoesCausaRaiz = [
    'Falha de hardware',
    'Falha de software / bug',
    'Configuração incorreta',
    'Instabilidade de rede / conectividade',
    'Erro operacional / usuário',
    'Problema com fornecedor externo',
  ];

  /** Enquanto espera o fornecedor (HU07), só o retorno tira o chamado de AGUARDANDO. */
  protected readonly aguardandoFornecedor = computed(() => {
    const c = this.chamado();
    return c?.status === 'AGUARDANDO' && c.motivo_espera === 'FORNECEDOR';
  });

  protected readonly podeResolver = computed(() => {
    const s = this.chamado()?.status;
    return (s === 'EM_ATENDIMENTO' || s === 'AGUARDANDO') && !this.aguardandoFornecedor();
  });

  protected readonly resolucaoValida = computed(() => {
    const t = this.resolucaoTexto().trim();
    return t.length >= 10 && t.length <= 2000;
  });

  protected readonly erroResolucao = computed(() => {
    if (!this.resolucaoTocou()) return null;
    const t = this.resolucaoTexto().trim();
    if (!t) return 'A descrição da solução é obrigatória.';
    if (t.length < 10) return 'A solução deve conter no mínimo 10 caracteres.';
    if (t.length > 2000) return 'A solução deve conter no máximo 2000 caracteres.';
    return null;
  });
  protected readonly modalEscalarAberto = signal(false);
  protected readonly fornecedorTexto = signal('');
  protected readonly protocoloExternoTexto = signal('');
  protected readonly observacaoFornecedorTexto = signal('');
  protected readonly escalarTocou = signal(false);

  protected readonly escalonamentoAberto = computed(
    () => this.chamado()?.escalonamentos?.find((e) => e.aberto) ?? null,
  );

  protected readonly podeEscalar = computed(() => {
    const c = this.chamado();
    return !!c && c.status === 'EM_ATENDIMENTO' && !!c.responsavel && !this.escalonamentoAberto();
  });

  protected readonly erroFornecedor = computed(() => {
    if (!this.escalarTocou()) return null;
    const t = this.fornecedorTexto().trim();
    if (!t) return 'Informe o fornecedor externo.';
    if (t.length > 120) return 'O fornecedor deve ter no máximo 120 caracteres.';
    return null;
  });

  protected readonly erroObservacaoFornecedor = computed(() => {
    if (!this.escalarTocou()) return null;
    const t = this.observacaoFornecedorTexto().trim();
    if (!t) return 'Descreva o problema para o fornecedor.';
    if (t.length > 1000) return 'A descrição deve ter no máximo 1000 caracteres.';
    return null;
  });

  protected readonly escalonamentoValido = computed(() => {
    const fornecedor = this.fornecedorTexto().trim();
    const observacao = this.observacaoFornecedorTexto().trim();
    return (
      fornecedor.length > 0 &&
      fornecedor.length <= 120 &&
      this.protocoloExternoTexto().trim().length <= 100 &&
      observacao.length > 0 &&
      observacao.length <= 1000
    );
  });

  protected readonly modalRetornoAberto = signal(false);
  protected readonly retornoTexto = signal('');
  protected readonly retornoTocou = signal(false);

  protected readonly retornoValido = computed(() => {
    const t = this.retornoTexto().trim();
    return t.length > 0 && t.length <= 2000;
  });

  protected readonly erroRetorno = computed(() => {
    if (!this.retornoTocou()) return null;
    const t = this.retornoTexto().trim();
    if (!t) return 'Descreva o retorno do fornecedor.';
    if (t.length > 2000) return 'O retorno deve ter no máximo 2000 caracteres.';
    return null;
  });

  // Estados do Registro de Novo Contato / Mensagem (RN16 / HU13)
  protected readonly canaisDisponiveis = CANAIS_ORIGEM;
  protected readonly novoContatoCanal = signal<CanalOrigem>('WHATSAPP');
  protected readonly novoContatoDirecao = signal<DirecaoContato>('RECEBIDO');
  protected readonly novoContatoConteudo = signal('');
  protected readonly novoContatoOutroSolicitante = signal(false);
  protected readonly novoContatoNome = signal('');
  protected readonly novoContatoIdentificador = signal('');
  protected readonly salvandoContato = signal(false);
  protected readonly erroContato = signal<string | null>(null);

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
        proximos = this.aguardandoFornecedor() ? [] : ['EM_ATENDIMENTO'];
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

  /**
   * Abre o modal com foco direto no cancelamento do chamado (HU12).
   */
  protected abrirModalCancelamento(): void {
    if (this.chamadoEncerrado()) {
      this.toast.info('Este chamado já se encontra encerrado.');
      return;
    }
    const statusAtual = this.chamado()?.status;
    if (statusAtual !== 'ABERTO' && statusAtual !== 'EM_TRIAGEM') {
      this.toast.info(
        `O cancelamento direto não está disponível para chamados com status "${this.rotuloStatus(statusAtual!)}".`,
      );
      return;
    }
    this.statusSelecionado.set('CANCELADO');
    this.motivoCancelamentoTexto.set('');
    this.modalStatusAberto.set(true);
  }

  // Métodos de Registro de Novo Contato / Mensagem (RN16 / HU13)
  protected selecionarCanalContato(canal: CanalOrigem): void {
    this.novoContatoCanal.set(canal);
  }

  protected selecionarDirecaoContato(direcao: DirecaoContato): void {
    this.novoContatoDirecao.set(direcao);
  }

  protected alternarOutroSolicitante(): void {
    this.novoContatoOutroSolicitante.update((v) => !v);
  }

  protected atualizarConteudoContato(texto: string): void {
    this.novoContatoConteudo.set(texto);
    if (this.erroContato()) {
      this.erroContato.set(null);
    }
  }

  protected atualizarNomeContato(texto: string): void {
    this.novoContatoNome.set(texto);
  }

  protected atualizarIdentificadorContato(texto: string): void {
    this.novoContatoIdentificador.set(texto);
  }

  protected limparFormularioContato(): void {
    this.novoContatoCanal.set('WHATSAPP');
    this.novoContatoDirecao.set('RECEBIDO');
    this.novoContatoConteudo.set('');
    this.novoContatoOutroSolicitante.set(false);
    this.novoContatoNome.set('');
    this.novoContatoIdentificador.set('');
    this.erroContato.set(null);
  }

  protected registrarNovoContato(): void {
    if (this.chamadoEncerrado()) {
      this.toast.info('Não é possível registrar contatos em um chamado encerrado.');
      return;
    }

    const id = this.idChamado();
    if (!id) return;

    const conteudoLimpo = this.novoContatoConteudo().trim();
    if (!conteudoLimpo) {
      this.erroContato.set('Informe o conteúdo da mensagem ou relato do contato.');
      this.toast.erro('Informe o conteúdo da mensagem ou relato do contato.');
      return;
    }

    if (conteudoLimpo.length > 2000) {
      this.erroContato.set('O relato deve ter no máximo 2000 caracteres.');
      this.toast.erro('O relato deve ter no máximo 2000 caracteres.');
      return;
    }

    const payload: RegistroContatoRequest = {
      canal: this.novoContatoCanal(),
      direcao: this.novoContatoDirecao(),
      conteudo: conteudoLimpo,
    };

    if (this.novoContatoOutroSolicitante()) {
      const identificador = this.novoContatoIdentificador().trim();
      if (!identificador) {
        this.erroContato.set('Informe o telefone ou e-mail do interlocutor.');
        this.toast.erro('Informe o telefone ou e-mail do interlocutor.');
        return;
      }
      payload.solicitante = {
        nome: this.novoContatoNome().trim() || undefined,
        contato: identificador,
      };
    }

    this.salvandoContato.set(true);
    this.erroContato.set(null);

    this.chamadoService
      .registrarContato(id, payload)
      .pipe(finalize(() => this.salvandoContato.set(false)))
      .subscribe({
        next: (dadosAtualizados) => {
          this.chamado.set(dadosAtualizados);
          this.toast.sucesso('Mensagem registrada com sucesso na timeline do chamado!');
          this.limparFormularioContato();
        },
        error: (err) => {
          const msgApi = extrairMensagensErro(err)[0] ?? 'Erro ao registrar contato.';
          this.erroContato.set(msgApi);
          this.toast.erro(msgApi);
        },
      });
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

  /**
   * Abre o modal de resolução do chamado (HU06).
   */
  protected abrirModalResolucao(): void {
    if (!this.podeResolver()) {
      this.toast.info('Para resolver o chamado, ele deve estar Em Atendimento ou Aguardando.');
      return;
    }
    this.resolucaoTexto.set('');
    this.causaRaizTexto.set('');
    this.possivelRecorrenciaCheck.set(false);
    this.resolucaoTocou.set(false);
    this.modalResolucaoAberto.set(true);
  }

  protected fecharModalResolucao(): void {
    this.modalResolucaoAberto.set(false);
  }

  protected atualizarResolucaoTexto(valor: string): void {
    this.resolucaoTexto.set(valor);
    this.resolucaoTocou.set(true);
  }

  protected atualizarCausaRaizTexto(valor: string): void {
    this.causaRaizTexto.set(valor);
  }

  protected selecionarSugestaoCausaRaiz(sugestao: string): void {
    this.causaRaizTexto.set(sugestao);
  }

  protected alternarPossivelRecorrencia(checked: boolean): void {
    this.possivelRecorrenciaCheck.set(checked);
  }

  /**
   * Confirma a resolução do chamado via PATCH /chamados/{id}/resolucao (HU06).
   */
  protected confirmarResolucao(): void {
    this.resolucaoTocou.set(true);
    if (!this.resolucaoValida()) {
      this.toast.erro('Preencha a descrição da solução detalhadamente (mínimo 10 caracteres).');
      return;
    }

    const id = this.idChamado();
    if (!id) return;

    const payload: ResolucaoRequest = {
      resolucao: this.resolucaoTexto().trim(),
      causa_raiz: this.causaRaizTexto().trim() || null,
      causaRaiz: this.causaRaizTexto().trim() || null,
      possivel_recorrencia: this.possivelRecorrenciaCheck(),
      possivelRecorrencia: this.possivelRecorrenciaCheck(),
    };

    this.salvando.set(true);
    this.chamadoService
      .resolver(id, payload)
      .pipe(finalize(() => this.salvando.set(false)))
      .subscribe({
        next: (dadosAtualizados) => {
          this.chamado.set(dadosAtualizados);
          this.toast.sucesso('Chamado resolvido com sucesso!');
          this.fecharModalResolucao();
        },
        error: (err) => {
          const msgApi = extrairMensagensErro(err)[0] ?? 'Erro ao resolver chamado.';
          this.toast.erro(msgApi);
        },
      });
  }

  protected abrirModalEscalar(): void {
    if (!this.podeEscalar()) {
      this.toast.info('Só é possível escalar um chamado Em Atendimento, com responsável e sem escalonamento em aberto.');
      return;
    }
    this.fornecedorTexto.set('');
    this.protocoloExternoTexto.set('');
    this.observacaoFornecedorTexto.set('');
    this.escalarTocou.set(false);
    this.modalEscalarAberto.set(true);
  }

  protected fecharModalEscalar(): void {
    this.modalEscalarAberto.set(false);
  }

  protected atualizarFornecedor(valor: string): void {
    this.fornecedorTexto.set(valor);
  }

  protected atualizarProtocoloExterno(valor: string): void {
    this.protocoloExternoTexto.set(valor);
  }

  protected atualizarObservacaoFornecedor(valor: string): void {
    this.observacaoFornecedorTexto.set(valor);
  }

  protected confirmarEscalonamento(): void {
    this.escalarTocou.set(true);
    if (!this.escalonamentoValido()) {
      this.toast.erro('Preencha o fornecedor e a descrição para o fornecedor.');
      return;
    }

    const id = this.idChamado();
    if (!id) return;

    const payload: EscalonamentoRequest = {
      fornecedor: this.fornecedorTexto().trim(),
      observacao: this.observacaoFornecedorTexto().trim(),
    };
    const protocolo = this.protocoloExternoTexto().trim();
    if (protocolo) {
      payload.protocolo_externo = protocolo;
    }

    this.salvando.set(true);
    this.chamadoService
      .escalar(id, payload)
      .pipe(finalize(() => this.salvando.set(false)))
      .subscribe({
        next: (dadosAtualizados) => {
          this.chamado.set(dadosAtualizados);
          this.toast.sucesso(`Chamado escalado para ${payload.fornecedor}.`);
          this.fecharModalEscalar();
        },
        error: (err) => {
          const msgApi = extrairMensagensErro(err)[0] ?? 'Erro ao escalar o chamado.';
          this.toast.erro(msgApi);
        },
      });
  }

  protected abrirModalRetorno(): void {
    if (!this.escalonamentoAberto()) {
      this.toast.info('Este chamado não tem escalonamento aguardando retorno.');
      return;
    }
    this.retornoTexto.set('');
    this.retornoTocou.set(false);
    this.modalRetornoAberto.set(true);
  }

  protected fecharModalRetorno(): void {
    this.modalRetornoAberto.set(false);
  }

  protected atualizarRetornoTexto(valor: string): void {
    this.retornoTexto.set(valor);
    this.retornoTocou.set(true);
  }

  protected confirmarRetorno(): void {
    this.retornoTocou.set(true);
    if (!this.retornoValido()) {
      this.toast.erro('Descreva o retorno do fornecedor.');
      return;
    }

    const id = this.idChamado();
    const escalonamento = this.escalonamentoAberto();
    if (!id || !escalonamento) return;

    const payload: RetornoFornecedorRequest = { retorno: this.retornoTexto().trim() };

    this.salvando.set(true);
    this.chamadoService
      .registrarRetornoFornecedor(id, escalonamento.id, payload)
      .pipe(finalize(() => this.salvando.set(false)))
      .subscribe({
        next: (dadosAtualizados) => {
          this.chamado.set(dadosAtualizados);
          this.toast.sucesso('Retorno do fornecedor registrado. O chamado voltou para Em Atendimento.');
          this.fecharModalRetorno();
        },
        error: (err) => {
          const msgApi = extrairMensagensErro(err)[0] ?? 'Erro ao registrar o retorno do fornecedor.';
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
