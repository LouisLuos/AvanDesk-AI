import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Modal } from './modal';

@Component({
  imports: [Modal],
  template: `
    <app-modal [aberto]="aberto()" titulo="Nova unidade" subtitulo="Subtítulo" (fechar)="fechamentos = fechamentos + 1">
      <span icone id="icone">i</span>
      <p id="corpo">Corpo</p>
      <button rodape type="button" id="acao">Salvar</button>
    </app-modal>
  `,
})
class Hospedeiro {
  readonly aberto = signal(true);
  fechamentos = 0;
}

describe('Modal', () => {
  let fixture: ComponentFixture<Hospedeiro>;
  let elemento: HTMLElement;

  beforeEach(() => {
    fixture = TestBed.createComponent(Hospedeiro);
    elemento = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  it('projeta ícone, conteúdo e rodapé num diálogo acessível', () => {
    const dialogo = elemento.querySelector('[role="dialog"]')!;
    const titulo = dialogo.querySelector('h2')!;

    expect(dialogo.getAttribute('aria-modal')).toBe('true');
    expect(dialogo.getAttribute('aria-labelledby')).toBe(titulo.id);
    expect(titulo.textContent?.trim()).toBe('Nova unidade');
    expect(dialogo.querySelector('header #icone')).not.toBeNull();
    expect(dialogo.querySelector('#corpo')).not.toBeNull();
    expect(dialogo.querySelector('footer #acao')).not.toBeNull();
  });

  it('não renderiza nada quando fechado', () => {
    fixture.componentInstance.aberto.set(false);
    fixture.detectChanges();

    expect(elemento.querySelector('[role="dialog"]')).toBeNull();
  });

  it('pede para fechar pelo X, pelo fundo e pela tecla Esc', () => {
    (elemento.querySelector('button[aria-label="Fechar"]') as HTMLButtonElement).click();
    (elemento.querySelector('[aria-hidden="true"].fixed') as HTMLElement).click();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

    expect(fixture.componentInstance.fechamentos).toBe(3);
  });
});
