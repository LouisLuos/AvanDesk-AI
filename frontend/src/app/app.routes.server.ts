import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  {
    // Telas autenticadas e dependentes da API: renderizadas no cliente
    // (evita chamadas HTTP durante o prerender do build).
    path: '**',
    renderMode: RenderMode.Client
  }
];
