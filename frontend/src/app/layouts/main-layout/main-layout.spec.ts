import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { MODO_SIMULADO } from '../../core/config/modo-simulado';
import { AuthService, UsuarioAutenticado } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { TokenStorageService } from '../../core/services/token-storage.service';
import { MainLayout } from './main-layout';

describe('MainLayout', () => {
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

  beforeEach(() => {
    if (!globalThis.localStorage) {
      vi.stubGlobal('localStorage', criarStorageEmMemoria());
    }
  });

  function renderizar(modoSimulado: boolean, usuario: UsuarioAutenticado | null = null): HTMLElement {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [MainLayout],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MODO_SIMULADO, useValue: modoSimulado },
      ],
    });
    TestBed.inject(AuthService).definirUsuario(usuario);
    const fixture = TestBed.createComponent(MainLayout);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  afterEach(() => {
    globalThis.sessionStorage?.clear();
    globalThis.localStorage?.clear();
    vi.unstubAllGlobals();
  });

  it('deve mostrar o selo de modo simulado quando o mock está ligado', () => {
    const selo = renderizar(true).querySelector('#badge-modo-simulado');

    expect(selo).not.toBeNull();
    expect(selo?.getAttribute('role')).toBe('status');
    expect(selo?.getAttribute('aria-label')).toContain('Modo simulado');
    expect(selo?.textContent).toContain('Simulado');
  });

  it('não deve mostrar o selo com a API real (integração e produção)', () => {
    expect(renderizar(false).querySelector('#badge-modo-simulado')).toBeNull();
  });

  it('deve mostrar a Administração só para a Liderança de TI', () => {
    const elemento = renderizar(false, {
      id: 'u-3',
      nome: 'Lideranca Dev',
      email: 'lideranca@lumen.local',
      perfil: 'LIDERANCA_TI',
    });

    expect(elemento.querySelector('#nav-administracao')).not.toBeNull();
  });

  it('deve esconder a Administração do analista de suporte', () => {
    const elemento = renderizar(false, {
      id: 'u-1',
      nome: 'Analista Dev Um',
      email: 'analista1@lumen.local',
      perfil: 'ANALISTA_SUPORTE',
    });

    expect(elemento.querySelector('#nav-fila-chamados')).not.toBeNull();
    expect(elemento.querySelector('#nav-administracao')).toBeNull();
  });

  it('não deve exibir dados de usuário nem botão de logout quando o usuário não estiver autenticado', () => {
    const elemento = renderizar(false);
    expect(elemento.querySelector('#btn-logout')).toBeNull();
  });

  it('deve encerrar a sessão no servidor e em memória e navegar para /login ao clicar em sair', () => {
    const elemento = renderizar(false, {
      id: 'u-1',
      nome: 'Analista Dev Um',
      email: 'analista1@lumen.local',
      perfil: 'ANALISTA_SUPORTE',
    });
    const tokenStorage = TestBed.inject(TokenStorageService);
    const authService = TestBed.inject(AuthService);
    const httpMock = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    const toast = TestBed.inject(ToastService);
    const navegarSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    const botaoLogout = elemento.querySelector('#btn-logout') as HTMLButtonElement;
    expect(botaoLogout).not.toBeNull();
    botaoLogout.click();

    const req = httpMock.expectOne('/api/v1/auth/logout');
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBe(true);
    req.flush({ status_code: 200, message: 'Logout realizado com sucesso', data: null, errors: null });
    httpMock.verify();

    expect(tokenStorage.obter()).toBeNull();
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
    expect(authService.usuario()).toBeNull();
    expect(authService.estaAutenticado()).toBe(false);
    expect(navegarSpy).toHaveBeenCalledWith(['/login']);
    expect(toast.toasts().map((t) => t.mensagem)).toContain('Sessão encerrada com sucesso.');
  });
});
