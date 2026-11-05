import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToastService } from '../../../core/services/toast.service';
import { TotalChamadosPorUnidade } from '../../../shared/models/chamado.model';
import { Unidade } from '../../../shared/models/unidade.model';
import { Unidades } from './unidades';

describe('Unidades (HU11)', () => {
  let fixture: ComponentFixture<Unidades>;
  let http: HttpTestingController;
  let elemento: HTMLElement;

  const UNIDADES: Unidade[] = [
    { id: 'u-old', nome: 'U.09 Olinda', identificacao: 'LUM-OLD', localizacao: null, atendimento_24h: false, ativo: false },
    { id: 'u-sed', nome: 'Sede Administrativa', identificacao: 'LUM-SED', localizacao: 'Rua do Bom Jesus, 10', atendimento_24h: false, ativo: true },
    { id: 'u-rct', nome: 'U.07 Recife Centro', identificacao: 'LUM-RCT', localizacao: 'Av Agamenon Magalhaes, 400', atendimento_24h: false, ativo: true },
    { id: 'u-bvg', nome: 'U.03 Boa Viagem', identificacao: 'LUM-BVG', localizacao: 'Rua Boa Viagem, 123', atendimento_24h: true, ativo: true },
  ];

  const TOTAIS: TotalChamadosPorUnidade[] = [
    { unidade_id: 'u-bvg', total: 52 },
    { unidade_id: 'u-rct', total: 41 },
  ];

  function envelope<T>(data: T) {
    return { status_code: 200, message: 'ok', data, errors: null };
  }

  function configurar(): void {
    TestBed.configureTestingModule({
      imports: [Unidades],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Unidades);
    elemento = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  }

  /** Responde a contagem (endpoint ainda não implementado no backend) e a lista de unidades. */
  function responderCarga(unidades: Unidade[] = UNIDADES, totais: TotalChamadosPorUnidade[] | 'erro' = TOTAIS): void {
    const reqTotais = http.expectOne((r) => r.url === '/api/v1/chamados/totais-por-unidade');
    expect(reqTotais.request.params.get('dias')).toBe('30');
    if (totais === 'erro') {
      reqTotais.flush(null, { status: 400, statusText: 'Bad Request' });
    } else {
      reqTotais.flush(envelope(totais));
    }
    http.expectOne('/api/v1/unidades').flush(envelope(unidades));
    fixture.detectChanges();
  }

  function iniciar(unidades?: Unidade[], totais?: TotalChamadosPorUnidade[] | 'erro'): void {
    configurar();
    responderCarga(unidades, totais);
  }

  const linhas = () => Array.from(elemento.querySelectorAll<HTMLTableRowElement>('tbody tr'));
  const celulas = (linha: HTMLTableRowElement) =>
    Array.from(linha.querySelectorAll('td')).map((td) => td.textContent?.trim() ?? '');
  const nomes = () => linhas().map((linha) => celulas(linha)[0]);
  const linhaDe = (nome: string) => linhas().find((linha) => celulas(linha)[0] === nome)!;
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
    (elemento.querySelector('#form-unidade') as HTMLFormElement).dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  it('lista as ativas por nome e as inativas por último', () => {
    iniciar();

    expect(nomes()).toEqual(['Sede Administrativa', 'U.03 Boa Viagem', 'U.07 Recife Centro', 'U.09 Olinda']);
  });

  it('mostra identificação, localização, atendimento 24h, chamados em 30 dias e status em cada linha', () => {
    iniciar();

    expect(celulas(linhaDe('U.03 Boa Viagem'))).toEqual([
      'U.03 Boa Viagem',
      'LUM-BVG',
      'Rua Boa Viagem, 123',
      'Sim',
      '52',
      'Ativa',
      '',
    ]);
    expect(celulas(linhaDe('Sede Administrativa'))[4]).toBe('0');
    expect(celulas(linhaDe('U.09 Olinda')).slice(2)).toEqual(['—', 'Não', '0', 'Inativa', '']);
    expect(linhaDe('U.03 Boa Viagem').querySelector('button')?.getAttribute('aria-label')).toBe('Editar U.03 Boa Viagem');
  });

  it('monta o subtítulo e o rodapé a partir das unidades carregadas', () => {
    iniciar();

    expect(texto('#subtitulo-unidades')).toBe('4 unidades cadastradas, 3 ativas.');
    expect(texto('#rodape-unidades')).toBe('Mostrando 4 de 4 registros');
  });

  it('mantém a lista e mostra "—" nos chamados enquanto a contagem não existe no backend', () => {
    iniciar(UNIDADES, 'erro');

    expect(linhas()).toHaveLength(4);
    expect(linhas().map((linha) => celulas(linha)[4])).toEqual(['—', '—', '—', '—']);
  });

  it('filtra pela busca e pelo status, atualizando o rodapé', () => {
    iniciar();

    digitar('#busca-unidade', 'recife centro');
    expect(nomes()).toEqual(['U.07 Recife Centro']);
    expect(texto('#rodape-unidades')).toBe('Mostrando 1 de 4 registros');

    digitar('#busca-unidade', 'lum-old');
    expect(nomes()).toEqual(['U.09 Olinda']);

    digitar('#busca-unidade', 'bom jesus');
    expect(nomes()).toEqual(['Sede Administrativa']);

    digitar('#busca-unidade', '');
    clicar('#filtro-situacao-btn');
    const inativas = Array.from(elemento.querySelectorAll<HTMLButtonElement>('[role="option"]')).find(
      (opcao) => opcao.textContent?.trim() === 'Inativas',
    );
    inativas?.click();
    fixture.detectChanges();
    expect(nomes()).toEqual(['U.09 Olinda']);

    digitar('#busca-unidade', 'boa viagem');
    expect(texto('tbody')).toBe('Nenhuma unidade encontrada para os filtros aplicados.');
  });

  it('mostra o erro de carregamento e tenta de novo', () => {
    configurar();
    http.expectOne((r) => r.url === '/api/v1/chamados/totais-por-unidade').flush(envelope(TOTAIS));
    http.expectOne('/api/v1/unidades').flush(null, { status: 500, statusText: 'Erro' });
    fixture.detectChanges();

    expect(texto('[role="alert"]')).toContain('Não foi possível carregar as unidades');
    expect(elemento.querySelector('#rodape-unidades')).toBeNull();

    clicar('#btn-recarregar');
    responderCarga();
    expect(linhas()).toHaveLength(4);
  });

  it('bloqueia o cadastro com campos obrigatórios vazios', () => {
    iniciar();
    clicar('#btn-nova-unidade');

    expect(texto('[role="dialog"] h2')).toBe('Nova unidade');
    enviarFormulario();

    http.expectNone('/api/v1/unidades');
    expect(texto('#erro-unidade-nome')).toBe('Informe o nome da unidade.');
    expect(texto('#ajuda-unidade-identificacao')).toBe('Informe a identificação da unidade.');
  });

  it('cadastra a unidade com os dados normalizados e inclui na lista', () => {
    iniciar();
    clicar('#btn-nova-unidade');

    digitar('#unidade-nome', '  U.20 Caruaru ');
    digitar('#unidade-identificacao', 'lum-cru');
    digitar('#unidade-localizacao', ' Av. Agamenon, 50 ');
    clicar('#unidade-24h');
    enviarFormulario();

    const req = http.expectOne('/api/v1/unidades');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      nome: 'U.20 Caruaru',
      identificacao: 'LUM-CRU',
      localizacao: 'Av. Agamenon, 50',
      atendimento_24h: true,
    });
    req.flush(
      envelope({ id: 'u-cru', nome: 'U.20 Caruaru', identificacao: 'LUM-CRU', localizacao: 'Av. Agamenon, 50', atendimento_24h: true, ativo: true }),
    );
    fixture.detectChanges();

    expect(dialogos()).toHaveLength(0);
    expect(nomes()).toContain('U.20 Caruaru');
    expect(toasts()).toContain('Unidade "U.20 Caruaru" cadastrada.');
  });

  it('mantém o modal aberto e mostra o erro devolvido pela API', () => {
    iniciar();
    clicar('#btn-nova-unidade');
    digitar('#unidade-nome', 'U.03 Boa Viagem');
    digitar('#unidade-identificacao', 'LUM-BVG');
    enviarFormulario();

    http
      .expectOne('/api/v1/unidades')
      .flush(
        { status_code: 400, message: 'Erro de validação', data: null, errors: ["Já existe uma unidade com a identificação 'LUM-BVG'."] },
        { status: 400, statusText: 'Bad Request' },
      );
    fixture.detectChanges();

    expect(dialogos()).toHaveLength(1);
    expect(texto('#erros-form-unidade')).toBe("Já existe uma unidade com a identificação 'LUM-BVG'.");
  });

  it('abre a edição com os dados da unidade e salva com PUT', () => {
    iniciar();
    clicar('#btn-editar-LUM-BVG');

    expect(texto('[role="dialog"] h2')).toBe('Editar unidade');
    expect(campo('#unidade-nome').value).toBe('U.03 Boa Viagem');
    expect(campo('#unidade-identificacao').value).toBe('LUM-BVG');
    expect(campo('#unidade-localizacao').value).toBe('Rua Boa Viagem, 123');
    expect(campo('#unidade-24h').checked).toBe(true);

    digitar('#unidade-localizacao', 'Rua Boa Viagem, 500');
    enviarFormulario();

    const req = http.expectOne('/api/v1/unidades/u-bvg');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.localizacao).toBe('Rua Boa Viagem, 500');
    req.flush(envelope({ ...UNIDADES[3], localizacao: 'Rua Boa Viagem, 500' }));
    fixture.detectChanges();

    expect(celulas(linhaDe('U.03 Boa Viagem'))[2]).toBe('Rua Boa Viagem, 500');
    expect(toasts()).toContain('Unidade "U.03 Boa Viagem" atualizada.');
  });

  it('desativa pela edição, confirmando com a contagem de chamados', () => {
    iniciar();
    clicar('#btn-editar-LUM-BVG');
    clicar('#btn-solicitar-desativacao');

    expect(dialogos()).toHaveLength(1);
    expect(texto('[role="dialog"] h2')).toBe('Desativar U.03 Boa Viagem?');
    expect(texto('#aviso-vinculo-chamados')).toContain('Os 52 chamados dos últimos 30 dias continuam vinculados');

    clicar('#btn-confirmar-desativacao');
    const req = http.expectOne('/api/v1/unidades/u-bvg/desativar');
    expect(req.request.method).toBe('PATCH');
    req.flush(envelope({ ...UNIDADES[3], ativo: false }));
    fixture.detectChanges();

    expect(dialogos()).toHaveLength(0);
    expect(celulas(linhaDe('U.03 Boa Viagem'))[5]).toBe('Inativa');
    expect(nomes().slice(-2)).toEqual(['U.03 Boa Viagem', 'U.09 Olinda']);
  });

  it('usa um texto genérico na confirmação quando a contagem não está disponível', () => {
    iniciar(UNIDADES, 'erro');
    clicar('#btn-editar-LUM-BVG');
    clicar('#btn-solicitar-desativacao');

    expect(texto('#aviso-vinculo-chamados')).toBe(
      'Os chamados já registrados continuam vinculados a esta unidade e o histórico é preservado.',
    );
  });

  it('avisa que a unidade está inativa e não oferece desativar de novo', () => {
    iniciar();
    clicar('#btn-editar-LUM-OLD');

    expect(elemento.querySelector('#aviso-unidade-inativa')).not.toBeNull();
    expect(elemento.querySelector('#btn-solicitar-desativacao')).toBeNull();
  });
});
