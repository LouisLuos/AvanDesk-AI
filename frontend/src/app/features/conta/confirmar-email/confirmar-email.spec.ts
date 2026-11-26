import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { ConfirmarEmail } from './confirmar-email';

describe('ConfirmarEmail', () => {
  let http: HttpTestingController;

  function abrir(token: string | null) {
    TestBed.configureTestingModule({
      imports: [ConfirmarEmail],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(token ? { token } : {}) } },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(ConfirmarEmail);
    fixture.detectChanges();
    return { fixture, component: fixture.componentInstance, navegar };
  }

  function recusar(codigo: string, mensagem: string) {
    http.expectOne('/api/v1/auth/verificar-email').flush(
      { status_code: 422, message: mensagem, data: null, errors: [codigo] },
      { status: 422, statusText: 'Unprocessable Content' },
    );
  }

  afterEach(() => http.verify());

  it('deve confirmar o token, tirar o token da URL e mostrar os chamados vinculados', () => {
    const { fixture, navegar } = abrir('abc_123');

    expect(navegar).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: {}, replaceUrl: true }));
    const requisicao = http.expectOne('/api/v1/auth/verificar-email');
    expect(requisicao.request.body).toEqual({ token: 'abc_123' });
    requisicao.flush({
      status_code: 200,
      message: 'E-mail confirmado',
      data: { email: 'mariana.alves@gmail.com', chamados_vinculados: 2 },
      errors: null,
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#confirmacao-sucesso')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('#chamados-vinculados').textContent).toContain('2');
  });

  it('deve tratar o link expirado oferecendo o reenvio', () => {
    const { fixture, component } = abrir('vencido');

    recusar('TOKEN_EXPIRADO', 'Este link expirou. Peça um novo link de verificação.');
    fixture.detectChanges();

    expect(component['motivo']()).toBe('TOKEN_EXPIRADO');
    expect(fixture.nativeElement.querySelector('#mensagem-recusa').textContent).toContain('Este link expirou');
    expect(fixture.nativeElement.querySelector('#btn-reenviar-verificacao')).not.toBeNull();
  });

  it('deve levar ao login quando o link já foi usado', () => {
    const { fixture } = abrir('usado');

    recusar('TOKEN_UTILIZADO', 'Este link já foi usado e a sua conta já está confirmada. É só entrar.');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#confirmacao-ja-feita')).not.toBeNull();
  });

  it('deve recusar sem chamar a API quando o link chega sem token', () => {
    const { component } = abrir(null);

    expect(component['situacao']()).toBe('recusado');
    expect(component['motivo']()).toBe('TOKEN_INVALIDO');
  });
});
