import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { MODO_SIMULADO } from '../../core/config/modo-simulado';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { TokenStorageService } from '../../core/services/token-storage.service';
import { MainLayout } from './main-layout';

describe('MainLayout', () => {
  function renderizar(modoSimulado: boolean): HTMLElement {
    TestBed.configureTestingModule({
      imports: [MainLayout],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MODO_SIMULADO, useValue: modoSimulado },
      ],
    });
    const fixture = TestBed.createComponent(MainLayout);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  afterEach(() => {
    sessionStorage.clear();
    localStorage.clear();
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

  it('deve limpar o token e os dados do usuário no storage e navegar para /login ao clicar em sair', () => {
    const elemento = renderizar(false);
    const tokenStorage = TestBed.inject(TokenStorageService);
    const authService = TestBed.inject(AuthService);
    const router = TestBed.inject(Router);
    const toast = TestBed.inject(ToastService);
    const navegarSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    tokenStorage.salvar('jwt-token-ativo', false);
    sessionStorage.setItem(
      'avandesk.user',
      JSON.stringify({
        id: 'u-1',
        nome: 'Analista Dev Um',
        email: 'analista1@lumen.local',
        perfil: 'ANALISTA_SUPORTE',
      }),
    );

    const botaoLogout = elemento.querySelector('#btn-logout') as HTMLButtonElement;
    botaoLogout.click();

    expect(tokenStorage.obter()).toBeNull();
    expect(sessionStorage.getItem('avandesk.user')).toBeNull();
    expect(authService.estaAutenticado()).toBe(false);
    expect(navegarSpy).toHaveBeenCalledWith(['/login']);
    expect(toast.toasts().map((t) => t.mensagem)).toContain('Sessão encerrada com sucesso.');
  });
});
