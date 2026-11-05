import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MODO_SIMULADO } from '../../core/config/modo-simulado';
import { PERFIS_ADMINISTRACAO } from '../../core/config/perfis';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { ToastContainer } from '../../shared/components/toast-container/toast-container';

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

  protected readonly modoSimulado = inject(MODO_SIMULADO);
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
