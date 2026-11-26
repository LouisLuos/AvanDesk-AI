import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { ContaSolicitanteService } from '../../../core/services/conta-solicitante.service';
import { ToastService } from '../../../core/services/toast.service';
import { extrairMensagensErro } from '../../../shared/utils/api-error';
import { emailValido, normalizarEmail } from '../../../shared/utils/contato';

export const ESPERA_REENVIO_SEGUNDOS = 60;

@Component({
  selector: 'app-reenvio-verificacao',
  templateUrl: './reenvio-verificacao.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReenvioVerificacao implements OnInit {
  private readonly contaService = inject(ContaSolicitanteService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly emailConhecido = input<string | null>(null);
  readonly comecarEsperando = input(false);

  protected readonly email = signal('');
  protected readonly segundosRestantes = signal(0);
  protected readonly enviando = signal(false);
  protected readonly aviso = signal<string | null>(null);
  protected readonly erro = signal<string | null>(null);

  protected readonly emailParaEnvio = computed(() => this.emailConhecido() ?? this.email());
  protected readonly podeReenviar = computed(
    () => !this.enviando() && this.segundosRestantes() === 0 && emailValido(this.emailParaEnvio()),
  );

  private temporizador: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.destroyRef.onDestroy(() => this.pararContagem());
    if (this.comecarEsperando()) {
      this.iniciarContagem();
    }
  }

  protected atualizarEmail(valor: string): void {
    this.email.set(valor);
    this.erro.set(null);
  }

  protected reenviar(): void {
    if (!this.podeReenviar()) return;
    const email = normalizarEmail(this.emailParaEnvio());
    this.enviando.set(true);
    this.erro.set(null);
    this.contaService
      .reenviarVerificacao(email)
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: (mensagem) => {
          this.contaService.guardarEmailPendente(email);
          this.aviso.set(mensagem);
          this.toast.info(mensagem);
          this.iniciarContagem();
        },
        error: (erro) => this.erro.set(extrairMensagensErro(erro)[0]),
      });
  }

  private iniciarContagem(): void {
    this.pararContagem();
    this.segundosRestantes.set(ESPERA_REENVIO_SEGUNDOS);
    this.temporizador = setInterval(() => {
      const restante = this.segundosRestantes() - 1;
      this.segundosRestantes.set(Math.max(restante, 0));
      if (restante <= 0) {
        this.pararContagem();
      }
    }, 1000);
  }

  private pararContagem(): void {
    if (this.temporizador !== null) {
      clearInterval(this.temporizador);
      this.temporizador = null;
    }
  }
}
