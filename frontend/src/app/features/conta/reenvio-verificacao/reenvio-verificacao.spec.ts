import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ESPERA_REENVIO_SEGUNDOS, ReenvioVerificacao } from './reenvio-verificacao';

describe('ReenvioVerificacao', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      imports: [ReenvioVerificacao],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  function criar(email: string | null, comecarEsperando: boolean) {
    const fixture = TestBed.createComponent(ReenvioVerificacao);
    fixture.componentRef.setInput('emailConhecido', email);
    fixture.componentRef.setInput('comecarEsperando', comecarEsperando);
    fixture.detectChanges();
    return fixture;
  }

  function botao(fixture: ReturnType<typeof criar>): HTMLButtonElement {
    return fixture.nativeElement.querySelector('#btn-reenviar-verificacao');
  }

  it('deve começar esperando 60 s quando o link acabou de ser enviado', () => {
    const fixture = criar('mariana.alves@gmail.com', true);

    expect(botao(fixture).disabled).toBe(true);
    expect(botao(fixture).textContent).toContain(`Reenviar link em ${ESPERA_REENVIO_SEGUNDOS} s`);

    vi.advanceTimersByTime(59_000);
    fixture.detectChanges();
    expect(botao(fixture).textContent).toContain('Reenviar link em 1 s');

    vi.advanceTimersByTime(1_000);
    fixture.detectChanges();
    expect(botao(fixture).disabled).toBe(false);
  });

  it('deve reenviar e voltar a esperar 60 s', () => {
    const fixture = criar('mariana.alves@gmail.com', false);

    botao(fixture).click();
    const requisicao = http.expectOne('/api/v1/auth/reenviar-verificacao');
    expect(requisicao.request.body).toEqual({ email: 'mariana.alves@gmail.com' });
    requisicao.flush({
      status_code: 202,
      message: 'Se houver uma conta pendente com este e-mail, enviamos um novo link de confirmação.',
      data: null,
      errors: null,
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#aviso-reenvio').textContent).toContain('enviamos um novo link');
    expect(botao(fixture).disabled).toBe(true);
    botao(fixture).click();
    http.expectNone('/api/v1/auth/reenviar-verificacao');
  });

  it('deve pedir o e-mail quando ele não é conhecido e só liberar com e-mail válido', () => {
    const fixture = criar(null, false);
    const campo = fixture.nativeElement.querySelector('#email-reenvio') as HTMLInputElement;
    expect(botao(fixture).disabled).toBe(true);

    campo.value = 'mariana@gmail';
    campo.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(botao(fixture).disabled).toBe(true);

    campo.value = 'Mariana.Alves@Gmail.com';
    campo.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    botao(fixture).click();

    expect(http.expectOne('/api/v1/auth/reenviar-verificacao').request.body).toEqual({
      email: 'mariana.alves@gmail.com',
    });
  });
});
