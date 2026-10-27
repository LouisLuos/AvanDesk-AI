import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CustomDropdown, OpcaoDropdown } from './custom-dropdown';

@Component({
  imports: [CustomDropdown],
  template: `
    <div id="fora">Fora do dropdown</div>
    <app-custom-dropdown
      id="filtro-teste"
      rotulo="Status"
      [opcaoPadrao]="{ valor: '', rotulo: 'Todos' }"
      [opcoes]="opcoes"
      [(valor)]="valorSelecionado"
    />
  `,
})
class TestHostComponent {
  valorSelecionado = '';
  opcoes: OpcaoDropdown[] = [
    { valor: 'ABERTO', rotulo: 'Aberto' },
    { valor: 'EM_TRIAGEM', rotulo: 'Em Triagem' },
    { valor: 'RESOLVIDO', rotulo: 'Resolvido' },
  ];
}

describe('CustomDropdown', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let host: TestHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  const obterBotaoGatilho = (): HTMLButtonElement => {
    return fixture.nativeElement.querySelector('[data-testid="dropdown-trigger"]');
  };

  it('deve inicializar com o rótulo padrão exibido no botão', () => {
    const botao = obterBotaoGatilho();
    expect(botao).toBeTruthy();
    expect(botao.textContent).toContain('Status:');
    expect(botao.textContent).toContain('Todos');
  });

  it('deve abrir o menu ao clicar no botão de gatilho', () => {
    const botao = obterBotaoGatilho();
    botao.click();
    fixture.detectChanges();

    const menu = fixture.nativeElement.querySelector('[role="listbox"]');
    expect(menu).toBeTruthy();
    const itens = fixture.nativeElement.querySelectorAll('[role="option"]');
    expect(itens.length).toBe(4); // 1 padrão + 3 opções
  });

  it('deve selecionar uma opção, atualizar o modelo e fechar o menu', () => {
    const botao = obterBotaoGatilho();
    botao.click();
    fixture.detectChanges();

    const opcoes = fixture.nativeElement.querySelectorAll('[role="option"]');
    const opcaoAberto = Array.from(opcoes).find(
      (el) => (el as HTMLElement).textContent?.includes('Aberto'),
    ) as HTMLButtonElement;

    expect(opcaoAberto).toBeTruthy();
    opcaoAberto.click();
    fixture.detectChanges();

    expect(host.valorSelecionado).toBe('ABERTO');
    expect(fixture.nativeElement.querySelector('[role="listbox"]')).toBeNull();
    expect(botao.textContent).toContain('Aberto');
  });

  it('deve fechar o menu ao pressionar Escape', () => {
    const botao = obterBotaoGatilho();
    botao.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="listbox"]')).toBeTruthy();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="listbox"]')).toBeNull();
  });

  it('deve fechar o menu ao clicar fora', () => {
    const botao = obterBotaoGatilho();
    botao.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="listbox"]')).toBeTruthy();

    const fora: HTMLElement = fixture.nativeElement.querySelector('#fora');
    fora.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="listbox"]')).toBeNull();
  });
});
