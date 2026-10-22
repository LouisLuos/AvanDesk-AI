import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { FilaChamados } from './fila-chamados';

describe('FilaChamados', () => {
  let component: FilaChamados;
  let fixture: ComponentFixture<FilaChamados>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FilaChamados],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(FilaChamados);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve inicializar o componente da fila de chamados', () => {
    expect(component).toBeTruthy();
  });

  it('deve inicializar com a página 1 e filtros vazios', () => {
    expect(component['pagina']()).toBe(1);
    expect(component['temFiltroAtivo']()).toBe(false);
  });
});
