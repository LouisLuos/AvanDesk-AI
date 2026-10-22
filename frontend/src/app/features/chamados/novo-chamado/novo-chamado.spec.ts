import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NovoChamado } from './novo-chamado';

describe('NovoChamado', () => {
  let component: NovoChamado;
  let fixture: ComponentFixture<NovoChamado>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NovoChamado],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(NovoChamado);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve inicializar o formulário com valores padrão alinhados ao mockup', () => {
    const valor = component['form'].getRawValue();
    expect(valor.canalOrigem).toBe('LIGACAO');
    expect(valor.categoria).toBe('Equipamento de apoio');
    expect(valor.prioridade).toBe('TRIAGEM');
    expect(valor.atribuirAMim).toBe(true);
  });

  it('deve permitir alternar o canal de atendimento para PRESENCIAL', () => {
    component['selecionarCanal']('PRESENCIAL');
    expect(component['canalSelecionado']()).toBe('PRESENCIAL');
  });

  it('deve permitir alternar o vínculo de duplicidade', () => {
    component['alternarVinculo']('chm-118');
    expect(component['vinculadoId']()).toBe('chm-118');
    component['alternarVinculo']('chm-118');
    expect(component['vinculadoId']()).toBeNull();
  });
});
