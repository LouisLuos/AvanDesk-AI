import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  ContaSolicitanteService,
  MotivoTokenRecusado,
} from '../../../core/services/conta-solicitante.service';
import { ContaLayout } from '../../../layouts/conta-layout/conta-layout';
import { ApiResponse } from '../../../shared/models/api-response.model';
import { ReenvioVerificacao } from '../reenvio-verificacao/reenvio-verificacao';

type Situacao = 'verificando' | 'confirmado' | 'ja-confirmado' | 'recusado';

const MOTIVOS: readonly MotivoTokenRecusado[] = ['TOKEN_INVALIDO', 'TOKEN_UTILIZADO', 'TOKEN_SUBSTITUIDO', 'TOKEN_EXPIRADO'];

@Component({
  selector: 'app-confirmar-email',
  imports: [RouterLink, ContaLayout, ReenvioVerificacao],
  templateUrl: './confirmar-email.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmarEmail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly contaService = inject(ContaSolicitanteService);

  protected readonly situacao = signal<Situacao>('verificando');
  protected readonly motivo = signal<MotivoTokenRecusado | null>(null);
  protected readonly mensagem = signal('');
  protected readonly chamadosVinculados = signal(0);

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token')?.trim() ?? '';
    this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
    if (!token) {
      this.recusar('TOKEN_INVALIDO', 'O link está incompleto. Abra o endereço completo enviado por e-mail.');
      return;
    }
    this.contaService.verificarEmail(token).subscribe({
      next: (resposta) => {
        this.contaService.esquecerEmailPendente();
        this.chamadosVinculados.set(resposta.chamados_vinculados);
        this.situacao.set('confirmado');
      },
      error: (erro: unknown) => this.tratarErro(erro),
    });
  }

  private tratarErro(erro: unknown): void {
    const corpo = erro instanceof HttpErrorResponse ? (erro.error as Partial<ApiResponse<unknown>> | null) : null;
    const codigo = corpo?.errors?.[0] as MotivoTokenRecusado | undefined;
    const motivo = codigo && MOTIVOS.includes(codigo) ? codigo : 'TOKEN_INVALIDO';
    const mensagem = corpo?.message && codigo ? corpo.message : 'Não foi possível confirmar o e-mail agora. Tente novamente.';
    if (motivo === 'TOKEN_UTILIZADO') {
      this.mensagem.set(mensagem);
      this.situacao.set('ja-confirmado');
      return;
    }
    this.recusar(motivo, mensagem);
  }

  private recusar(motivo: MotivoTokenRecusado, mensagem: string): void {
    this.motivo.set(motivo);
    this.mensagem.set(mensagem);
    this.situacao.set('recusado');
  }
}
