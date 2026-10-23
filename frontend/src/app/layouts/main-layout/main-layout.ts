import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/services/auth.service';
import { ToastContainer } from '../../shared/components/toast-container/toast-container';
import { USUARIO_ATUAL } from '../../shared/utils/usuario-atual';

@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ToastContainer],
  templateUrl: './main-layout.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainLayout {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  protected readonly useMocks = environment.useMocks;
  protected readonly usuario = computed(
    () =>
      this.authService.usuario() ?? {
        id: USUARIO_ATUAL.id,
        nome: USUARIO_ATUAL.nome,
        email: 'ana.souza@lumendiagnosticos.com.br',
        perfil: USUARIO_ATUAL.perfil,
      },
  );

  protected readonly menu = [
    { rotulo: 'Fila de Chamados', rota: '/chamados', id: 'nav-fila-chamados' },
    { rotulo: 'Administração', rota: '/admin/unidades', id: 'nav-administracao' },
  ];

  protected get iniciais(): string {
    const nome = this.usuario().nome;
    return nome
      .split(' ')
      .map((p) => p[0])
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
  }
}

