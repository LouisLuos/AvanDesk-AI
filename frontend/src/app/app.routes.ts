import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./layouts/main-layout/main-layout').then((m) => m.MainLayout),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'chamados/novo' },
      {
        path: 'chamados/novo',
        title: 'Novo Chamado | AvanDesk-AI',
        loadComponent: () =>
          import('./features/chamados/novo-chamado/novo-chamado').then((m) => m.NovoChamado),
      },
      {
        // HU11 — restrito à Liderança de TI quando a HU09 (perfis) estiver pronta.
        path: 'admin/unidades',
        title: 'Unidades | AvanDesk-AI',
        loadComponent: () =>
          import('./features/admin/unidades/unidades').then((m) => m.Unidades),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
