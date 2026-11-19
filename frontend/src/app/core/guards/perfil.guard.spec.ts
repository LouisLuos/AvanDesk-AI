import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { MODO_SIMULADO } from '../config/modo-simulado';
import { AuthService } from '../services/auth.service';
import { perfilGuard } from './perfil.guard';

describe('perfilGuard', () => {
  function executar(perfilLogado: string | null, modoSimulado = false) {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MODO_SIMULADO, useValue: modoSimulado },
      ],
    });
    if (perfilLogado) {
      TestBed.inject(AuthService).definirUsuario({
        id: 'u-1',
        nome: 'Usuário de Teste',
        email: 'teste@lumen.local',
        perfil: perfilLogado,
      });
    }
    return TestBed.runInInjectionContext(() =>
      perfilGuard(['LIDERANCA_TI'])({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
  }

  afterEach(() => globalThis.sessionStorage?.clear());

  it('libera a Liderança de TI', () => {
    expect(executar('LIDERANCA_TI')).toBe(true);
  });

  it('manda o analista de volta para a fila', () => {
    const resultado = executar('ANALISTA_SUPORTE');

    expect(resultado).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(resultado as UrlTree)).toBe('/chamados');
  });

  it('manda para a fila quando não há usuário carregado', () => {
    expect(executar(null)).toBeInstanceOf(UrlTree);
  });

  it('libera tudo no modo simulado, como o authGuard', () => {
    expect(executar(null, true)).toBe(true);
  });
});
