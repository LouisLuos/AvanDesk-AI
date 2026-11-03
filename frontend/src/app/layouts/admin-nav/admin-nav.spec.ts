import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AdminNav } from './admin-nav';

@Component({ template: '' })
class Vazio {}

describe('AdminNav', () => {
  function renderizar() {
    TestBed.configureTestingModule({
      imports: [AdminNav],
      providers: [provideRouter([{ path: 'admin/unidades', component: Vazio }])],
    });
    const fixture = TestBed.createComponent(AdminNav);
    fixture.detectChanges();
    return fixture;
  }

  it('liga só a aba de Unidades e marca as demais como "Em breve"', () => {
    const elemento = renderizar().nativeElement as HTMLElement;

    const unidades = elemento.querySelector('#aba-unidades')!;
    expect(unidades.tagName).toBe('A');
    expect(unidades.getAttribute('href')).toBe('/admin/unidades');

    for (const id of ['aba-painel', 'aba-analistas', 'aba-fornecedores', 'aba-categorias']) {
      const aba = elemento.querySelector(`#${id}`)!;
      expect(aba.tagName).toBe('SPAN');
      expect(aba.getAttribute('aria-disabled')).toBe('true');
      expect(aba.textContent).toContain('Em breve');
    }
  });

  it('marca a aba da rota atual com aria-current', async () => {
    const fixture = renderizar();
    await TestBed.inject(Router).navigateByUrl('/admin/unidades');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#aba-unidades').getAttribute('aria-current')).toBe('page');
  });
});
