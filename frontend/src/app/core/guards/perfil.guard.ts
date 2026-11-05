import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { MODO_SIMULADO } from '../config/modo-simulado';
import { AuthService } from '../services/auth.service';

/**
 * Libera a rota só para os perfis informados (RF-022). Os demais voltam para a fila de chamados.
 * Complementa o authGuard, que já garante o login.
 */
export function perfilGuard(perfis: readonly string[]): CanActivateFn {
  return () => {
    const perfil = inject(AuthService).usuario()?.perfil;
    const modoSimulado = inject(MODO_SIMULADO);
    const router = inject(Router);

    if (modoSimulado || (perfil && perfis.includes(perfil))) {
      return true;
    }
    return router.createUrlTree(['/chamados']);
  };
}
