import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor (HU26 — Cookies HttpOnly)', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;
  let router: Router;

  beforeEach(() => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve enviar requisições com withCredentials=true e sem header Authorization Bearer', () => {
    http.get('/api/v1/chamados').subscribe();

    const req = httpMock.expectOne('/api/v1/chamados');
    expect(req.request.withCredentials).toBe(true);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({ status_code: 200, message: 'OK', data: null, errors: null });
  });

  it('deve limpar a sessão em memória e redirecionar para /login ao receber 401 em rota protegida', () => {
    const navegarSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    authService.definirUsuario({
      id: 'u-1',
      nome: 'Analista Um',
      email: 'analista1@lumen.local',
      perfil: 'ANALISTA_SUPORTE',
    });

    http.get('/api/v1/chamados').subscribe({ error: () => undefined });

    const req = httpMock.expectOne('/api/v1/chamados');
    req.flush(
      { status_code: 401, message: 'Não autorizado', data: null, errors: null },
      { status: 401, statusText: 'Unauthorized' },
    );

    expect(authService.usuario()).toBeNull();
    expect(authService.estaAutenticado()).toBe(false);
    expect(navegarSpy).toHaveBeenCalledWith(['/login']);
  });

  it('não deve redirecionar para /login ao receber 401 em /auth/me durante checagem de sessão', () => {
    const navegarSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    http.get('/api/v1/auth/me').subscribe({ error: () => undefined });

    const req = httpMock.expectOne('/api/v1/auth/me');
    req.flush(
      { status_code: 401, message: 'Não autorizado', data: null, errors: null },
      { status: 401, statusText: 'Unauthorized' },
    );

    expect(navegarSpy).not.toHaveBeenCalled();
  });
});

