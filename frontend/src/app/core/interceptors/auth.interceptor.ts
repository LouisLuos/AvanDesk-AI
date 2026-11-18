import { HttpErrorResponse, HttpInterceptorFn, HttpStatusCode } from '@angular/common/http';
import { Injector, inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Envia credenciais (cookies HttpOnly e token XSRF) em todas as requisições da API (HU26).
 * Em caso de 401 em rotas protegidas, limpa a sessão em memória e redireciona para /login.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const injector = inject(Injector);
  const router = inject(Router);

  const requisicao = req.clone({ withCredentials: true });

  return next(requisicao).pipe(
    catchError((erro: unknown) => {
      if (
        erro instanceof HttpErrorResponse &&
        erro.status === HttpStatusCode.Unauthorized &&
        !req.url.endsWith('/auth/login') &&
        !req.url.endsWith('/auth/me')
      ) {
        injector.get(AuthService).limparSessaoLocal();
        router.navigate(['/login']);
      }
      return throwError(() => erro);
    }),
  );
};
