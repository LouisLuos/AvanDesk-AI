import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MODO_SIMULADO } from '../../core/config/modo-simulado';
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
});
