import { Routes } from '@angular/router';
import { PERFIL_SOLICITANTE, PERFIS_ADMINISTRACAO, PERFIS_EQUIPE } from './core/config/perfis';
import { authGuard } from './core/guards/auth.guard';
import { perfilGuard } from './core/guards/perfil.guard';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Entrar na plataforma | Nexo',
    loadComponent: () =>
      import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'cadastro',
    title: 'Criar conta | Nexo',
    loadComponent: () => import('./features/conta/cadastro/cadastro').then((m) => m.Cadastro),
  },
  {
    path: 'verifique-seu-email',
    title: 'Verifique o seu e-mail | Nexo',
    loadComponent: () =>
      import('./features/conta/verifique-email/verifique-email').then((m) => m.VerifiqueEmail),
  },
  {
    path: 'verificar-email',
    title: 'Confirmar e-mail | Nexo',
    loadComponent: () =>
      import('./features/conta/confirmar-email/confirmar-email').then((m) => m.ConfirmarEmail),
  },
  {
    path: 'solicitante',
    title: 'Minha conta | Nexo',
    canActivate: [authGuard, perfilGuard([PERFIL_SOLICITANTE])],
    loadComponent: () =>
      import('./features/conta/inicio-solicitante/inicio-solicitante').then((m) => m.InicioSolicitante),
  },
  {
    path: '',
    canActivate: [authGuard, perfilGuard(PERFIS_EQUIPE)],
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
        path: 'admin',
        canActivate: [perfilGuard(PERFIS_ADMINISTRACAO)],
        children: [
          {
            // Abas da administração, no outlet de largura total do MainLayout.
            path: '',
            outlet: 'subnav',
            loadComponent: () => import('./layouts/admin-nav/admin-nav').then((m) => m.AdminNav),
          },
          { path: '', pathMatch: 'full', redirectTo: 'painel' },
          {
            path: 'painel',
            title: 'Painel da Administração | Nexo',
            loadComponent: () =>
              import('./features/dashboard/painel-administracao/painel-administracao').then(
                (m) => m.PainelAdministracao,
              ),
          },
          {
            path: 'unidades',
            title: 'Unidades | Nexo',
            loadComponent: () =>
              import('./features/admin/unidades/unidades').then((m) => m.Unidades),
          },
          {
            path: 'analistas',
            title: 'Analistas | Nexo',
            loadComponent: () =>
              import('./features/admin/analistas/analistas').then((m) => m.Analistas),
          },
        ],
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
