import { isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID, inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { MODO_SIMULADO } from '../config/modo-simulado';
import { AuthService } from '../services/auth.service';

/**
 * Guarda de rotas autenticadas (HU09 / HU26 / RF-021).
 * Impede acesso às telas internas sem login ativo e, em recarregamento de página,
 * hidrata a sessão em memória via GET /api/v1/auth/me (cookie HttpOnly) antes de redirecionar.
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const modoSimulado = inject(MODO_SIMULADO);
  const router = inject(Router);
  const isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  if (authService.estaAutenticado() || modoSimulado) {
    return true;
  }

  if (!isBrowser) {
    return router.createUrlTree(['/login']);
  }

  return authService
    .carregarSessao()
    .pipe(map((usuario) => (usuario ? true : router.createUrlTree(['/login']))));
};

