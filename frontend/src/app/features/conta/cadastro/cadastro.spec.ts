import { HttpRequest, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { ContaSolicitanteService } from '../../../core/services/conta-solicitante.service';
import { Cadastro } from './cadastro';

describe('Cadastro', () => {
  let fixture: ComponentFixture<Cadastro>;
  let component: Cadastro;
  let http: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Cadastro],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(Cadastro);
    component = fixture.componentInstance;
    fixture.detectChanges();
    http.expectOne('/api/v1/auth/unidades').flush({
      status_code: 200,
      message: 'ok',
      data: [{ id: 'u-bvg', nome: 'U.03 Boa Viagem' }],
      errors: null,
    });
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function preencher(valores: Partial<Record<string, string>> = {}): void {
    component['form'].setValue({
      nome: 'Mariana Alves',
      email: ' Mariana.Alves@Gmail.com ',
      unidadeId: 'u-bvg',
      senha: 'Coleta#2026ok',
      confirmacaoSenha: 'Coleta#2026ok',
      ...valores,
    });
    fixture.detectChanges();
  }

  function nenhumCadastroEnviado(): void {
    http.expectNone((req: HttpRequest<unknown>) => req.url === '/api/v1/auth/registrar');
  }

  it('deve listar as unidades públicas no select', () => {
    const opcoes = Array.from(
      (fixture.nativeElement.querySelector('#unidade') as HTMLSelectElement).options,
    ).map((opcao) => opcao.textContent!.trim());
    expect(opcoes).toEqual(['Selecionar unidade...', 'U.03 Boa Viagem']);
  });

  it('deve marcar no checklist cada regra da senha atendida', () => {
    preencher({ senha: 'coleta2026', confirmacaoSenha: 'coleta2026' });

    const regras = component['regras']();
    expect(regras.filter((regra) => regra.ok).map((regra) => regra.id)).toEqual(['tamanho', 'minuscula', 'numero']);
    expect(component['form'].controls.senha.hasError('senhaFraca')).toBe(true);
  });

  it('deve bloquear o envio com senha fraca ou senhas diferentes', () => {
    preencher({ senha: 'coleta2026', confirmacaoSenha: 'coleta2026' });
    component['criarConta']();
    preencher({ confirmacaoSenha: 'Coleta#2026OK' });
    component['criarConta']();
    fixture.detectChanges();

    nenhumCadastroEnviado();
    expect(component['senhasDiferentes']()).toBe(true);
  });

  it('deve criar a conta com e-mail normalizado e ir para a tela de verificação', () => {
    const navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    preencher();

    component['criarConta']();
    const requisicao = http.expectOne('/api/v1/auth/registrar');
    expect(requisicao.request.body).toEqual({
      nome: 'Mariana Alves',
      email: 'mariana.alves@gmail.com',
      unidade_id: 'u-bvg',
      senha: 'Coleta#2026ok',
    });
    requisicao.flush({
      status_code: 201,
      message: 'Conta criada',
      data: { email: 'mariana.alves@gmail.com', status_conta: 'PENDENTE_VERIFICACAO', verificacao_expira_em: '2026-11-27T18:00:00Z' },
      errors: null,
    });

    expect(navegar).toHaveBeenCalledWith(['/verifique-seu-email']);
    expect(TestBed.inject(ContaSolicitanteService).emailPendente()).toBe('mariana.alves@gmail.com');
  });

  it('deve oferecer entrar ou pedir novo link quando o e-mail já tem conta', () => {
    preencher();

    component['criarConta']();
    http.expectOne('/api/v1/auth/registrar').flush(
      { status_code: 409, message: 'Conflito', data: null, errors: ['Já existe uma conta com este e-mail.'] },
      { status: 409, statusText: 'Conflict' },
    );
    fixture.detectChanges();

    const erros = fixture.nativeElement.querySelector('#erros-cadastro') as HTMLElement;
    expect(erros.textContent).toContain('Já existe uma conta com este e-mail.');
    expect(erros.textContent).toContain('pedir um novo link');
  });
});
