import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ToastContainer } from '../../shared/components/toast-container/toast-container';

@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ToastContainer],
  templateUrl: './main-layout.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainLayout {
  // Usuário fictício até a HU09 (login) fornecer o usuário autenticado.
  protected readonly usuario = { nome: 'Ana Souza', perfil: 'Liderança de TI' };

  protected readonly menu = [
    { rotulo: 'Novo Chamado', rota: '/chamados/novo', id: 'nav-novo-chamado' },
    { rotulo: 'Unidades', rota: '/admin/unidades', id: 'nav-unidades' },
  ];

  protected get iniciais(): string {
    return this.usuario.nome
      .split(' ')
      .map((p) => p[0])
      .slice(0, 2)
      .join('');
  }
}
