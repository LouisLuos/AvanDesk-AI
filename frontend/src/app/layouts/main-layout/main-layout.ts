import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MODO_SIMULADO } from '../../core/config/modo-simulado';
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

  protected readonly usuario = USUARIO_ATUAL;
  protected readonly modoSimulado = inject(MODO_SIMULADO);

  protected readonly menu = [
    { rotulo: 'Fila de Chamados', rota: '/chamados', id: 'nav-fila-chamados' },
    { rotulo: 'Administração', rota: '/admin', id: 'nav-administracao' },
  ];

  protected get iniciais(): string {
    return this.usuario.nome
      .split(' ')
      .map((p) => p[0])
      .slice(0, 2)
      .join('');
  }

  protected buscar(evento: Event, campo: HTMLInputElement): void {
    const teclado = evento as KeyboardEvent;
    if (teclado.isComposing || teclado.keyCode === 229) return;
    const termo = campo.value.trim();
    this.router.navigate(['/chamados'], { queryParams: { busca: termo || null } });
  }
}
