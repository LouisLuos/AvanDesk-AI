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
import { ChamadoService } from '../../../core/services/chamado.service';
import { ToastService } from '../../../core/services/toast.service';
import {
  CanalOrigem,
  ChamadoDetalhe,
  PrioridadeChamado,
  StatusChamado,
} from '../../../shared/models/chamado.model';

@Component({
  selector: 'app-detalhe-chamado',
  imports: [RouterLink],
  templateUrl: './detalhe-chamado.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DetalheChamado implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly chamadoService = inject(ChamadoService);
  private readonly toast = inject(ToastService);

  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);
  protected readonly chamado = signal<ChamadoDetalhe | null>(null);

  protected readonly idChamado = computed(() => this.route.snapshot.paramMap.get('id') ?? '');

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
        error: () => this.erro.set('Não foi possível carregar os detalhes do chamado.'),
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
      REABERTO: 'Reaberto',
      RESOLVIDO: 'Resolvido',
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
      REABERTO: 'bg-[#e5484d]/10 text-[#e5484d] border-[#e5484d]/20',
      RESOLVIDO: 'bg-[#22c55e]/10 text-[#22c55e] border-[#22c55e]/20',
      MESCLADO: 'bg-[#8b5cf6]/10 text-[#8b5cf6] border-[#8b5cf6]/20',
    };
    return estilos[status] ?? 'bg-[#e2e5ec] text-[#363b47] border-transparent';
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

  protected iniciais(nome: string): string {
    return nome
      .split(' ')
      .map((p) => p[0])
      .slice(0, 2)
      .join('')
      .toUpperCase();
  }
}
