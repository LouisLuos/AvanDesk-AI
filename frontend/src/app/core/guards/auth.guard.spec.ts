import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  GuardResult,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { Observable, isObservable } from 'rxjs';
import { MODO_SIMULADO } from '../config/modo-simulado';
import { AuthService, UsuarioAutenticado } from '../services/auth.service';
import { authGuard } from './auth.guard';

describe('authGuard (HU09 / HU26)', () => {
  const usuarioExemplo: UsuarioAutenticado = {
    id: 'u-1',
    nome: 'Analista Um',
    email: 'analista1@lumen.local',
    perfil: 'ANALISTA_SUPORTE',
  };

  function configurar(opcoes: { modoSimulado?: boolean; platformId?: string } = {}) {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MODO_SIMULADO, useValue: opcoes.modoSimulado ?? false },
        { provide: PLATFORM_ID, useValue: opcoes.platformId ?? 'browser' },
      ],
    });
    return {
      authService: TestBed.inject(AuthService),
      httpMock: TestBed.inject(HttpTestingController),
      router: TestBed.inject(Router),
    };
  }

  function executarGuard() {
    return TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
  }

  it('libera imediatamente quando o usuário já está autenticado em memória sem chamar /auth/me', () => {
    const { authService, httpMock } = configurar();
    authService.definirUsuario(usuarioExemplo);

    const resultado = executarGuard();

    expect(resultado).toBe(true);
    httpMock.verify();
  });

  it('libera imediatamente no modo simulado sem chamar /auth/me', () => {
    const { httpMock } = configurar({ modoSimulado: true });

    const resultado = executarGuard();

    expect(resultado).toBe(true);
    httpMock.verify();
  });

  it('hidrata a sessão via GET /api/v1/auth/me ao recarregar a página e libera o acesso', () => {
    const { authService, httpMock } = configurar();
    const resultado = executarGuard();

    expect(isObservable(resultado)).toBe(true);

    let valor: GuardResult | undefined;
    (resultado as Observable<GuardResult>).subscribe((r) => {
      valor = r;
    });

    const req = httpMock.expectOne('/api/v1/auth/me');
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBe(true);
    req.flush({
      status_code: 200,
      message: 'Sessão ativa',
      data: usuarioExemplo,
      errors: null,
    });

    expect(valor).toBe(true);
    expect(authService.usuario()).toEqual(usuarioExemplo);
    expect(authService.estaAutenticado()).toBe(true);
    httpMock.verify();
  });

  it('redireciona para /login quando GET /api/v1/auth/me retornar 401', () => {
    const { authService, httpMock, router } = configurar();
    const resultado = executarGuard();

    let valor: GuardResult | undefined;
    (resultado as Observable<GuardResult>).subscribe((r) => {
      valor = r;
    });

    const req = httpMock.expectOne('/api/v1/auth/me');
    req.flush(
      { status_code: 401, message: 'Não autenticado', data: null, errors: null },
      { status: 401, statusText: 'Unauthorized' },
    );

    expect(valor).toBeInstanceOf(UrlTree);
    expect(router.serializeUrl(valor as UrlTree)).toBe('/login');
    expect(authService.estaAutenticado()).toBe(false);
    httpMock.verify();
  });

  it('redireciona para /login em ambiente SSR sem disparar chamada HTTP', () => {
    const { httpMock, router } = configurar({ platformId: 'server' });

    const resultado = executarGuard();

    expect(resultado).toBeInstanceOf(UrlTree);
    expect(router.serializeUrl(resultado as UrlTree)).toBe('/login');
    httpMock.verify();
  });
});
