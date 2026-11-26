import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { MODO_SIMULADO } from '../../core/config/modo-simulado';
import { PERFIS_ADMINISTRACAO } from '../../core/config/perfis';
import { AuthService } from '../../core/services/auth.service';
import { RealtimeChamadosService } from '../../core/services/realtime-chamados.service';
import { ToastService } from '../../core/services/toast.service';
import { EventoTempoReal } from '../../shared/models/tempo-real.model';
import { ToastContainer } from '../../shared/components/toast-container/toast-container';

const CHAVE_SOM_ALERTA_P1 = 'nexo.som-alerta-p1';
export const MAXIMO_ALERTAS_P1 = 3;

const ROTULOS_PERFIL: Record<string, string> = {
  ANALISTA_SUPORTE: 'Analista de suporte',
  LIDERANCA_TI: 'Liderança de TI',
};

@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ToastContainer],
  templateUrl: './main-layout.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainLayout {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly realtime = inject(RealtimeChamadosService);

  protected readonly modoSimulado = inject(MODO_SIMULADO);

  protected readonly estadoTempoReal = this.realtime.estado;
  protected readonly alertasP1 = signal<EventoTempoReal[]>([]);
  protected readonly somAlertaLigado = signal(lerPreferenciaSom());

  constructor() {
    effect(() => {
      if (this.authService.usuario() && !this.modoSimulado) {
        this.realtime.conectar();
      } else {
        void this.realtime.desconectar();
      }
    });

    this.realtime.eventos$
      .pipe(
        filter(
          (evento) =>
            evento.tipo === 'CHAMADO_CRIADO' &&
            evento.chamado.prioridade === 'ALTA' &&
            evento.autor?.id !== this.authService.usuario()?.id,
        ),
        takeUntilDestroyed(),
      )
      .subscribe((evento) => {
        this.alertasP1.update((atuais) =>
          [evento, ...atuais.filter((a) => a.chamado.id !== evento.chamado.id)].slice(0, MAXIMO_ALERTAS_P1),
        );
        if (this.somAlertaLigado()) tocarAlerta();
      });
  }

  protected dispensarAlerta(id: string): void {
    this.alertasP1.update((atuais) => atuais.filter((a) => a.chamado.id !== id));
  }

  protected alternarSomAlerta(): void {
    const ligado = !this.somAlertaLigado();
    this.somAlertaLigado.set(ligado);
    try {
      localStorage.setItem(CHAVE_SOM_ALERTA_P1, String(ligado));
    } catch {
      return;
    }
  }
  protected readonly usuario = computed(() => {
    const autenticado = this.authService.usuario();
    if (!autenticado) {
      return null;
    }
    return {
      ...autenticado,
      perfil: ROTULOS_PERFIL[autenticado.perfil] ?? autenticado.perfil,
    };
  });

  /** A Administração só aparece para os perfis que o perfilGuard deixa entrar. */
  protected readonly menu = computed(() => {
    const perfil = this.authService.usuario()?.perfil;
    const veAdministracao = this.modoSimulado || (!!perfil && PERFIS_ADMINISTRACAO.includes(perfil));
    return [
      { rotulo: 'Fila de Chamados', rota: '/chamados', id: 'nav-fila-chamados' },
      ...(veAdministracao ? [{ rotulo: 'Administração', rota: '/admin', id: 'nav-administracao' }] : []),
    ];
  });

  protected get iniciais(): string {
    const nome = this.usuario()?.nome;
    if (!nome) return '';
    return nome
      .split(' ')
      .filter(Boolean)
      .map((p: string) => p[0])
      .slice(0, 2)
      .join('')
      .toUpperCase();
  }

  protected buscar(evento: Event, campo: HTMLInputElement): void {
    const teclado = evento as KeyboardEvent;
    if (teclado.isComposing || teclado.keyCode === 229) return;
    const termo = campo.value.trim();
    this.router.navigate(['/chamados'], { queryParams: { busca: termo || null } });
  }

  protected logout(): void {
    this.authService.logout();
    this.toast.info('Sessão encerrada com sucesso.');
  }
}

function lerPreferenciaSom(): boolean {
  try {
    return globalThis.localStorage?.getItem(CHAVE_SOM_ALERTA_P1) === 'true';
  } catch {
    return false;
  }
}

function tocarAlerta(): void {
  try {
    const Contexto = globalThis.AudioContext;
    if (!Contexto) return;
    const audio = new Contexto();
    const oscilador = audio.createOscillator();
    const volume = audio.createGain();
    oscilador.frequency.value = 880;
    volume.gain.setValueAtTime(0.15, audio.currentTime);
    volume.gain.exponentialRampToValueAtTime(0.001, audio.currentTime + 0.6);
    oscilador.connect(volume).connect(audio.destination);
    oscilador.start();
    oscilador.stop(audio.currentTime + 0.6);
    oscilador.onended = () => void audio.close();
  } catch {
    return;
  }
}
