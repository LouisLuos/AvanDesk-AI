import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService, UsuarioAutenticado } from './auth.service';

describe('AuthService (HU26 — Cookies HttpOnly)', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let router: Router;

  function criarStorageEmMemoria(): Storage {
    const dados = new Map<string, string>();
    return {
      get length() {
        return dados.size;
      },
      clear: () => dados.clear(),
      getItem: (chave: string) => dados.get(chave) ?? null,
      key: (indice: number) => Array.from(dados.keys())[indice] ?? null,
      removeItem: (chave: string) => {
        dados.delete(chave);
      },
      setItem: (chave: string, valor: string) => {
        dados.set(chave, valor);
      },
    };
  }

  const usuarioExemplo: UsuarioAutenticado = {
    id: 'u-1',
    nome: 'Analista Um',
    email: 'analista1@lumen.local',
    perfil: 'ANALISTA_SUPORTE',
  };

  beforeEach(() => {
    if (!globalThis.localStorage) {
      vi.stubGlobal('localStorage', criarStorageEmMemoria());
    }
    globalThis.localStorage?.clear();
    globalThis.sessionStorage?.clear();

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpMock.verify();
    globalThis.localStorage?.clear();
    globalThis.sessionStorage?.clear();
    vi.unstubAllGlobals();
  });

  it('deve realizar login com withCredentials=true sem gravar JWT ou usuário no storage', () => {
    let respostaRecebida: UsuarioAutenticado | undefined;

    service.login({ usuario: 'analista1', senha: '123', manterConectado: true }).subscribe((res) => {
      respostaRecebida = res.usuario;
    });

    const req = httpMock.expectOne('/api/v1/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBe(true);
    expect(req.request.body).toEqual({
      email: 'analista1@lumen.local',
      senha: '123',
      manter_conectado: true,
    });

    req.flush({
      status_code: 200,
      message: 'Autenticação realizada com sucesso',
      data: { usuario: usuarioExemplo },
      errors: null,
    });

    expect(respostaRecebida).toEqual(usuarioExemplo);
    expect(service.usuario()).toEqual(usuarioExemplo);
    expect(service.estaAutenticado()).toBe(true);
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });

  it('deve hidratar a sessão em memória via GET /auth/me com withCredentials=true', () => {
    let usuarioCarregado: UsuarioAutenticado | null | undefined;

    service.carregarSessao().subscribe((u) => {
      usuarioCarregado = u;
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

    expect(usuarioCarregado).toEqual(usuarioExemplo);
    expect(service.usuario()).toEqual(usuarioExemplo);
    expect(service.estaAutenticado()).toBe(true);
  });

  it('deve limpar a sessão em memória sem lançar erro quando GET /auth/me retornar 401', () => {
    service.definirUsuario(usuarioExemplo);
    let usuarioCarregado: UsuarioAutenticado | null | undefined;

    service.carregarSessao().subscribe((u) => {
      usuarioCarregado = u;
    });

    const req = httpMock.expectOne('/api/v1/auth/me');
    req.flush(
      { status_code: 401, message: 'Não autenticado', data: null, errors: null },
      { status: 401, statusText: 'Unauthorized' },
    );

    expect(usuarioCarregado).toBeNull();
    expect(service.usuario()).toBeNull();
    expect(service.estaAutenticado()).toBe(false);
  });

  it('deve disparar POST /auth/logout com withCredentials=true, zerar memória e navegar para /login', () => {
    const navegarSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    service.definirUsuario(usuarioExemplo);

    service.logout();

    const req = httpMock.expectOne('/api/v1/auth/logout');
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBe(true);
    req.flush({ status_code: 200, message: 'Logout realizado com sucesso', data: null, errors: null });

    expect(service.usuario()).toBeNull();
    expect(service.estaAutenticado()).toBe(false);
    expect(navegarSpy).toHaveBeenCalledWith(['/login']);
  });
});

