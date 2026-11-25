import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { InfoTooltip } from './info-tooltip';

@Component({
  imports: [InfoTooltip],
  template: `<app-info-tooltip titulo="TR (Líquido)" texto="Desconta pausas aguardando solicitante." />`,
})
class HostTooltipTest {}

describe('InfoTooltip', () => {
  let fixture: ComponentFixture<HostTooltipTest>;
  let elemento: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HostTooltipTest],
    });
    fixture = TestBed.createComponent(HostTooltipTest);
    elemento = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  it('mantem o tooltip oculto inicialmente e exibe ao passar o mouse ou focar', () => {
    const trigger = elemento.querySelector('[data-testid="info-tooltip-trigger"]') as HTMLButtonElement;
    expect(trigger).not.toBeNull();
    expect(elemento.querySelector('[role="tooltip"]')).toBeNull();

    trigger.dispatchEvent(new MouseEvent('mouseenter'));
    fixture.detectChanges();

    const tooltip = elemento.querySelector('[role="tooltip"]');
    expect(tooltip).not.toBeNull();
    expect(tooltip?.textContent).toContain('TR (Líquido)');
    expect(tooltip?.textContent).toContain('Desconta pausas aguardando solicitante.');

    trigger.dispatchEvent(new MouseEvent('mouseleave'));
    fixture.detectChanges();
    expect(elemento.querySelector('[role="tooltip"]')).toBeNull();
  });

  it('permite fixar ao clicar e fechar com Escape', () => {
    const trigger = elemento.querySelector('[data-testid="info-tooltip-trigger"]') as HTMLButtonElement;

    trigger.click();
    fixture.detectChanges();
    expect(elemento.querySelector('[role="tooltip"]')).not.toBeNull();

    trigger.dispatchEvent(new MouseEvent('mouseleave'));
    fixture.detectChanges();
    expect(elemento.querySelector('[role="tooltip"]')).not.toBeNull();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    expect(elemento.querySelector('[role="tooltip"]')).toBeNull();
  });
});
