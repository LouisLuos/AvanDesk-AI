import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { MODO_SIMULADO } from '../config/modo-simulado';
import { paginaInicialDoPerfil } from '../config/perfis';
import { AuthService } from '../services/auth.service';

export function perfilGuard(perfis: readonly string[]): CanActivateFn {
  return () => {
    const perfil = inject(AuthService).usuario()?.perfil;
    const modoSimulado = inject(MODO_SIMULADO);
    const router = inject(Router);

    if (modoSimulado || (perfil && perfis.includes(perfil))) {
      return true;
    }
    return router.createUrlTree([paginaInicialDoPerfil(perfil)]);
  };
}
