import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Entrar na plataforma | Nexo',
    loadComponent: () =>
      import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./layouts/main-layout/main-layout').then((m) => m.MainLayout),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'chamados' },
      {
        path: 'chamados',
        pathMatch: 'full',
        title: 'Fila de Chamados | Nexo',
        loadComponent: () =>
          import('./features/chamados/fila-chamados/fila-chamados').then((m) => m.FilaChamados),
      },
      {
        path: 'chamados/novo',
        title: 'Registrar chamado | Nexo',
        loadComponent: () =>
          import('./features/chamados/novo-chamado/novo-chamado').then((m) => m.NovoChamado),
      },
      {
        path: 'chamados/:id',
        title: 'Detalhes do Chamado | Nexo',
        loadComponent: () =>
          import('./features/chamados/detalhe-chamado/detalhe-chamado').then((m) => m.DetalheChamado),
      },
      {
        // HU11 — restrito à Liderança de TI quando a HU09 (perfis) estiver pronta.
        path: 'admin/unidades',
        title: 'Unidades | Nexo',
        loadComponent: () =>
          import('./features/admin/unidades/unidades').then((m) => m.Unidades),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
