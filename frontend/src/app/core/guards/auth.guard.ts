import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { MODO_SIMULADO } from '../config/modo-simulado';
import { AuthService } from '../services/auth.service';

/**
 * Guarda de rotas autenticadas (HU09 / RF-021).
 * Impede acesso às telas internas sem login ativo e após o logout.
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const modoSimulado = inject(MODO_SIMULADO);
  const router = inject(Router);

  if (authService.estaAutenticado() || modoSimulado) {
    return true;
  }

  return router.createUrlTree(['/login']);
};

