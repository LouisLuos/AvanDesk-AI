import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToastService } from '../../../core/services/toast.service';
import { Analista } from '../../../shared/models/analista.model';
import { TotalChamadosPorResponsavel } from '../../../shared/models/chamado.model';
import { Analistas } from './analistas';

describe('Analistas (HU10)', () => {
  let fixture: ComponentFixture<Analistas>;
  let http: HttpTestingController;
  let elemento: HTMLElement;

  const ANALISTAS: Analista[] = [
    { id: 'a-pedro', nome: 'Pedro Alves', email: 'pedro.alves@lumen.local', ativo: false, criado_em: '2026-10-01T12:00:00Z' },
    { id: 'a-carlos', nome: 'Carlos Lima', email: 'carlos.lima@lumen.local', ativo: true, criado_em: '2026-10-01T12:00:00Z' },
    { id: 'a-ana', nome: 'Ana Souza', email: 'ana.souza@lumen.local', ativo: true, criado_em: '2026-10-01T12:00:00Z' },
  ];

  const TOTAIS: TotalChamadosPorResponsavel[] = [
    { responsavel_id: 'a-ana', total: 6 },
    { responsavel_id: 'a-carlos', total: 1 },
  ];

  function envelope<T>(data: T) {
    return { status_code: 200, message: 'ok', data, errors: null };
  }

  function configurar(): void {
    TestBed.configureTestingModule({
      imports: [Analistas],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Analistas);
    elemento = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  }

  function responderCarga(analistas: Analista[] = ANALISTAS, totais: TotalChamadosPorResponsavel[] | 'erro' = TOTAIS): void {
    const reqTotais = http.expectOne('/api/v1/chamados/totais-por-responsavel');
    if (totais === 'erro') {
      reqTotais.flush(null, { status: 500, statusText: 'Erro' });
    } else {
      reqTotais.flush(envelope(totais));
    }
    http.expectOne('/api/v1/analistas').flush(envelope(analistas));
    fixture.detectChanges();
  }

  function iniciar(analistas?: Analista[], totais?: TotalChamadosPorResponsavel[] | 'erro'): void {
    configurar();
    responderCarga(analistas, totais);
  }

  const linhas = () => Array.from(elemento.querySelectorAll<HTMLTableRowElement>('tbody tr'));
  const celulas = (linha: HTMLTableRowElement) =>
    Array.from(linha.querySelectorAll('td')).map((td) => td.textContent?.replace(/\s+/g, ' ').trim() ?? '');
  const nomes = () => linhas().map((linha) => linha.querySelector('td:first-child > span > span:last-child')?.textContent?.trim());
  const linhaDe = (nome: string) => linhas().find((linha) => celulas(linha)[0].endsWith(nome))!;
  const texto = (seletor: string) => elemento.querySelector(seletor)?.textContent?.replace(/\s+/g, ' ').trim();
  const dialogos = () => Array.from(elemento.querySelectorAll('[role="dialog"]'));
  const campo = (seletor: string) => elemento.querySelector(seletor) as HTMLInputElement;
  const toasts = () => TestBed.inject(ToastService).toasts().map((t) => t.mensagem);

  function clicar(seletor: string): void {
    (elemento.querySelector(seletor) as HTMLElement).click();
    fixture.detectChanges();
  }

  function digitar(seletor: string, valor: string): void {
    const input = campo(seletor);
    input.value = valor;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function enviarFormulario(): void {
    (elemento.querySelector('#form-analista') as HTMLFormElement).dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  it('lista os ativos por nome e os inativos por último', () => {
    iniciar();

    expect(nomes()).toEqual(['Ana Souza', 'Carlos Lima', 'Pedro Alves']);
  });

  it('mostra iniciais, e-mail, chamados ativos e status em cada linha', () => {
    iniciar();

    expect(celulas(linhaDe('Ana Souza'))).toEqual(['AS Ana Souza', 'ana.souza@lumen.local', '6', 'Ativo', '']);
    expect(celulas(linhaDe('Pedro Alves')).slice(2, 4)).toEqual(['0', 'Inativo']);
    expect(linhaDe('Ana Souza').querySelector('button')?.getAttribute('aria-label')).toBe('Editar Ana Souza');
    expect(texto('#rodape-analistas')).toBe('Mostrando 3 de 3 analistas');
  });

  it('mantém a lista e mostra "—" nos chamados quando a contagem falha', () => {
    iniciar(ANALISTAS, 'erro');

    expect(linhas().map((linha) => celulas(linha)[2])).toEqual(['—', '—', '—']);
  });

  it('filtra pela busca e pelo status, atualizando o rodapé', () => {
    iniciar();

    digitar('#busca-analista', 'carlos.lima');
    expect(nomes()).toEqual(['Carlos Lima']);
    expect(texto('#rodape-analistas')).toBe('Mostrando 1 de 3 analistas');

    digitar('#busca-analista', '');
    clicar('#filtro-situacao-btn');
    Array.from(elemento.querySelectorAll<HTMLButtonElement>('[role="option"]'))
      .find((opcao) => opcao.textContent?.trim() === 'Inativos')
      ?.click();
    fixture.detectChanges();
    expect(nomes()).toEqual(['Pedro Alves']);
  });

  it('mostra o erro de carregamento e tenta de novo', () => {
    configurar();
    http.expectOne('/api/v1/chamados/totais-por-responsavel').flush(envelope(TOTAIS));
    http.expectOne('/api/v1/analistas').flush(null, { status: 500, statusText: 'Erro' });
    fixture.detectChanges();

    expect(texto('[role="alert"]')).toContain('Não foi possível carregar os analistas');

    clicar('#btn-recarregar');
    responderCarga();
    expect(linhas()).toHaveLength(3);
  });

  it('bloqueia o cadastro com campos vazios ou e-mail inválido', () => {
    iniciar();
    clicar('#btn-novo-analista');
    expect(texto('[role="dialog"] h2')).toBe('Novo analista');

    enviarFormulario();
    expect(texto('#erro-analista-nome')).toBe('Informe o nome do analista.');
    expect(texto('#ajuda-analista-email')).toBe('Informe o e-mail corporativo.');

    digitar('#analista-email', 'sem-arroba');
    expect(texto('#ajuda-analista-email')).toBe('Informe um e-mail válido.');
    http.expectNone('/api/v1/analistas');
  });

  it('cadastra e mostra a senha temporária uma única vez', async () => {
    iniciar();
    clicar('#btn-novo-analista');
    digitar('#analista-nome', '  Diego Faria ');
    digitar('#analista-email', ' Diego.Faria@LUMEN.local ');
    enviarFormulario();

    const req = http.expectOne('/api/v1/analistas');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ nome: 'Diego Faria', email: 'diego.faria@lumen.local' });
    req.flush(
      envelope({
        analista: { id: 'a-diego', nome: 'Diego Faria', email: 'diego.faria@lumen.local', ativo: true, criado_em: '2026-11-03T12:00:00Z' },
        senha_temporaria: 'Kp7m-Qx4t-Rz9w',
      }),
    );
    fixture.detectChanges();

    expect(texto('[role="dialog"] h2')).toBe('Cadastro concluído');
    expect(texto('[role="dialog"] header p')).toBe('Repasse a senha temporária de Diego Faria. Ela não aparece de novo.');
    expect(texto('#senha-temporaria')).toBe('Kp7m-Qx4t-Rz9w');
    expect(nomes()).toContain('Diego Faria');

    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
    clicar('#btn-copiar-senha');
    await Promise.resolve();
    expect(writeText).toHaveBeenCalledWith('Kp7m-Qx4t-Rz9w');
    expect(toasts()).toContain('Senha temporária copiada.');

    clicar('#btn-concluir-cadastro');
    expect(dialogos()).toHaveLength(0);

    clicar('#btn-novo-analista');
    expect(elemento.querySelector('#senha-temporaria')).toBeNull();
  });

  it('mantém o formulário e mostra o erro de e-mail repetido devolvido pela API', () => {
    iniciar();
    clicar('#btn-novo-analista');
    digitar('#analista-nome', 'Ana Souza');
    digitar('#analista-email', 'ana.souza@lumen.local');
    enviarFormulario();

    http
      .expectOne('/api/v1/analistas')
      .flush(
        { status_code: 409, message: 'Conflito', data: null, errors: ["Já existe um usuário com o e-mail 'ana.souza@lumen.local'."] },
        { status: 409, statusText: 'Conflict' },
      );
    fixture.detectChanges();

    expect(texto('#erros-form-analista')).toBe("Já existe um usuário com o e-mail 'ana.souza@lumen.local'.");
    expect(elemento.querySelector('#senha-temporaria')).toBeNull();
  });

  it('edita nome e e-mail com PUT', () => {
    iniciar();
    clicar('#btn-editar-a-ana');

    expect(texto('[role="dialog"] h2')).toBe('Editar analista');
    expect(campo('#analista-email').value).toBe('ana.souza@lumen.local');

    digitar('#analista-nome', 'Ana Souza Lima');
    enviarFormulario();

    const req = http.expectOne('/api/v1/analistas/a-ana');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ nome: 'Ana Souza Lima', email: 'ana.souza@lumen.local' });
    req.flush(envelope({ ...ANALISTAS[2], nome: 'Ana Souza Lima' }));
    fixture.detectChanges();

    expect(nomes()).toContain('Ana Souza Lima');
    expect(toasts()).toContain('Cadastro de Ana Souza Lima atualizado.');
  });

  it('desativa pela edição avisando quantos chamados ativos continuam atribuídos', () => {
    iniciar();
    clicar('#btn-editar-a-ana');
    clicar('#btn-solicitar-desativacao');

    expect(dialogos()).toHaveLength(1);
    expect(texto('[role="dialog"] h2')).toBe('Desativar Ana Souza?');
    expect(texto('#aviso-chamados-ativos')).toBe('Os 6 chamados ativos continuam atribuídos até serem reatribuídos.');

    clicar('#btn-confirmar-desativacao');
    const req = http.expectOne('/api/v1/analistas/a-ana/desativar');
    expect(req.request.method).toBe('PATCH');
    req.flush(envelope({ ...ANALISTAS[2], ativo: false }));
    fixture.detectChanges();

    expect(dialogos()).toHaveLength(0);
    expect(celulas(linhaDe('Ana Souza'))[3]).toBe('Inativo');
    expect(toasts()).toContain('Acesso de Ana Souza desativado.');
  });

  it('usa o texto no singular quando resta um chamado ativo', () => {
    iniciar();
    clicar('#btn-editar-a-carlos');
    clicar('#btn-solicitar-desativacao');

    expect(texto('#aviso-chamados-ativos')).toBe('O chamado ativo continua atribuído até ser reatribuído.');
  });

  it('reativa um analista inativo pela edição', () => {
    iniciar();
    clicar('#btn-editar-a-pedro');

    expect(elemento.querySelector('#aviso-analista-inativo')).not.toBeNull();
    expect(elemento.querySelector('#btn-solicitar-desativacao')).toBeNull();

    clicar('#btn-reativar-analista');
    const req = http.expectOne('/api/v1/analistas/a-pedro/reativar');
    expect(req.request.method).toBe('PATCH');
    req.flush(envelope({ ...ANALISTAS[0], ativo: true }));
    fixture.detectChanges();

    expect(dialogos()).toHaveLength(0);
    expect(celulas(linhaDe('Pedro Alves'))[3]).toBe('Ativo');
    expect(toasts()).toContain('Acesso de Pedro Alves reativado.');
  });
});
