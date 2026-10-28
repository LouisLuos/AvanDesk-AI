import { HttpErrorResponse, HttpInterceptorFn, HttpStatusCode } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { TokenStorageService } from '../services/token-storage.service';

/**
 * Anexa o Bearer token JWT às requisições da API, quando houver token salvo (HU09).
 * Em caso de 401 em rotas protegidas, limpa a sessão expirada/inválida e redireciona para /login.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenStorage = inject(TokenStorageService);
  const router = inject(Router);
  const token = tokenStorage.obter();

  const requisicao = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(requisicao).pipe(
    catchError((erro: unknown) => {
      if (
        erro instanceof HttpErrorResponse &&
        erro.status === HttpStatusCode.Unauthorized &&
        !req.url.endsWith('/auth/login')
      ) {
        tokenStorage.limpar();
        router.navigate(['/login']);
      }
      return throwError(() => erro);
    }),
  );
};
