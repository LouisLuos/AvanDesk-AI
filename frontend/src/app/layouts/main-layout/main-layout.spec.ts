import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MODO_SIMULADO } from '../../core/config/modo-simulado';
import { MainLayout } from './main-layout';

describe('MainLayout', () => {
  function renderizar(modoSimulado: boolean): HTMLElement {
    TestBed.configureTestingModule({
      imports: [MainLayout],
      providers: [provideRouter([]), { provide: MODO_SIMULADO, useValue: modoSimulado }],
    });
    const fixture = TestBed.createComponent(MainLayout);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('deve mostrar o selo de modo simulado quando o mock está ligado', () => {
    const selo = renderizar(true).querySelector('#selo-modo-simulado');

    expect(selo?.textContent?.trim()).toBe('MODO SIMULADO: dados fictícios');
    expect(selo?.getAttribute('role')).toBe('note');
  });

  it('não deve mostrar o selo com a API real (integração e produção)', () => {
    expect(renderizar(false).querySelector('#selo-modo-simulado')).toBeNull();
  });
});
