import { HttpRequest, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { ToastService } from '../../../core/services/toast.service';
import { ChamadoRequest, ChamadoResponse, ItemLista } from '../../../shared/models/chamado.model';
import { Unidade } from '../../../shared/models/unidade.model';
import { NovoChamado } from './novo-chamado';

const ABERTURA_DA_TELA = new Date('2026-10-22T13:00:00.000Z');

const UNIDADES_ATIVAS: Unidade[] = [
  {
    id: 'u-old',
    nome: 'Unidade Olinda',
    identificacao: 'LUM-OLD',
    atendimento_24h: false,
    ativo: true,
  },
  {
    id: 'u-bvg',
    nome: 'Unidade Boa Viagem',
    identificacao: 'LUM-BVG',
    atendimento_24h: false,
    ativo: true,
  },
  {
    id: 'u-csf',
    nome: 'Unidade Casa Forte',
    identificacao: 'LUM-CSF',
    atendimento_24h: true,
    ativo: true,
  },
];

const CATEGORIAS: ItemLista[] = [
  { id: 'cat-acesso', nome: 'Acesso e senhas' },
  { id: 'cat-equip', nome: 'Equipamento de apoio' },
];

const SISTEMAS: ItemLista[] = [
  { id: 'sis-agenda', nome: 'Sistema de agendamento' },
  { id: 'sis-etiquetas', nome: 'Impressora de etiquetas' },
];

const CHAMADO_CRIADO: ChamadoResponse = {
  id: 'c-1',
  codigo: 'LUM-2026-000124',
  status: 'ABERTO',
  data_criacao: '2026-10-22T13:00:45.000Z',
  origem_dado: 'SIMULADO',
};

function envelope<T>(data: T) {
  return { status_code: 200, message: 'ok', data, errors: null };
}

describe('NovoChamado', () => {
  let fixture: ComponentFixture<NovoChamado>;
  let component: NovoChamado;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NovoChamado],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => vi.useRealTimers());

  /**
   * Abre a tela. As requisições de carga (unidades, categorias, sistemas e fila) ficam pendentes
   * até serem respondidas.
   */
  function abrirTela(): void {
    fixture = TestBed.createComponent(NovoChamado);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function pedidoGet(url: string): ReturnType<HttpTestingController['expectOne']> {
    return http.expectOne((req) => req.method === 'GET' && req.url === url);
  }

  function responderCarga(unidades: Unidade[] = UNIDADES_ATIVAS): void {
    pedidoGet('/api/v1/unidades').flush(envelope(unidades));
    pedidoGet('/api/v1/categorias').flush(envelope(CATEGORIAS));
    pedidoGet('/api/v1/sistemas-equipamentos').flush(envelope(SISTEMAS));
    pedidoGet('/api/v1/chamados').flush(envelope([]));
    fixture.detectChanges();
  }

  function preencher(
    valores: Partial<{
      unidadeId: string;
      contato: string;
      nome: string;
      categoriaId: string;
      sistemaId: string;
      descricao: string;
    }>,
  ): void {
    component['form'].patchValue({
      contato: '81988771234',
      categoriaId: 'cat-equip',
      descricao: 'Impressora de etiquetas da coleta parou de imprimir.',
      ...valores,
    });
  }

  function opcoes(seletor: string): string[] {
    const select = fixture.nativeElement.querySelector(seletor) as HTMLSelectElement;
    return Array.from(select.options).map((opcao) => opcao.textContent!.trim());
  }

  function registrar(): void {
    component['registrar']();
    fixture.detectChanges();
  }

  function requisicaoDeRegistro(): ReturnType<HttpTestingController['expectOne']> {
    return http.expectOne((req) => req.method === 'POST' && req.url === '/api/v1/chamados');
  }

  function nenhumRegistroEnviado(): void {
    http.expectNone(
      (req: HttpRequest<unknown>) => req.method === 'POST' && req.url === '/api/v1/chamados',
    );
  }

  // ---------- estado inicial (mockup) ----------

  describe('estado inicial', () => {
    beforeEach(() => abrirTela());

    it('deve inicializar o formulário com valores padrão alinhados ao mockup', () => {
      const valor = component['form'].getRawValue();
      expect(valor.canalOrigem).toBe('LIGACAO');
      expect(valor.prioridade).toBe('TRIAGEM');
      expect(valor.atribuirAMim).toBe(true);
    });

    it('deve começar sem categoria nem sistema escolhidos, para o analista escolher', () => {
      const valor = component['form'].getRawValue();
      expect(valor.categoriaId).toBe('');
      expect(valor.sistemaId).toBe('');
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

    it('deve exibir o aviso de não registrar dados clínicos (LGPD)', () => {
      const avisos = Array.from(
        fixture.nativeElement.querySelectorAll('[role="note"]'),
      ) as HTMLElement[];
      expect(
        avisos.some((aviso) => aviso.textContent?.includes('Não registre dados clínicos')),
      ).toBe(true);
    });
  });

  // ---------- unidades ----------

  it('deve pedir só unidades ativas e listá-las em ordem alfabética', () => {
    abrirTela();
    const pedido = http.expectOne((req) => req.method === 'GET' && req.url === '/api/v1/unidades');
    expect(pedido.request.params.get('ativo')).toBe('true');
    pedido.flush(envelope(UNIDADES_ATIVAS));
    pedidoGet('/api/v1/categorias').flush(envelope(CATEGORIAS));
    pedidoGet('/api/v1/sistemas-equipamentos').flush(envelope(SISTEMAS));
    pedidoGet('/api/v1/chamados').flush(envelope([]));

    expect(component['unidades']().map((u) => u.nome)).toEqual([
      'Unidade Boa Viagem',
      'Unidade Casa Forte',
      'Unidade Olinda',
    ]);
    expect(component['form'].controls.unidadeId.value).toBe('u-bvg');
  });

  it('deve avisar quando as unidades não carregam e permitir tentar de novo', () => {
    abrirTela();
    pedidoGet('/api/v1/unidades').flush(null, { status: 500, statusText: 'Erro' });
    pedidoGet('/api/v1/categorias').flush(envelope(CATEGORIAS));
    pedidoGet('/api/v1/sistemas-equipamentos').flush(envelope(SISTEMAS));
    pedidoGet('/api/v1/chamados').flush(envelope([]));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar as unidades.');

    component['carregarUnidades']();
    pedidoGet('/api/v1/unidades').flush(envelope(UNIDADES_ATIVAS));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Não foi possível carregar as unidades.');
    expect(component['form'].controls.unidadeId.value).toBe('u-bvg');
  });

  // ---------- categorias e sistemas (listas da API) ----------

  it('deve montar os selects de categoria e sistema com as listas da API', () => {
    abrirTela();
    responderCarga();

    expect(opcoes('#categoria')).toEqual([
      'Selecionar categoria...',
      'Acesso e senhas',
      'Equipamento de apoio',
    ]);
    expect(opcoes('#sistema')).toEqual([
      'Não informado',
      'Sistema de agendamento',
      'Impressora de etiquetas',
    ]);
  });

  it('deve avisar quando as listas não carregam e permitir tentar de novo', () => {
    abrirTela();
    pedidoGet('/api/v1/unidades').flush(envelope(UNIDADES_ATIVAS));
    const sistemasPrimeiraTentativa = pedidoGet('/api/v1/sistemas-equipamentos');
    pedidoGet('/api/v1/categorias').flush(null, { status: 500, statusText: 'Erro' });
    pedidoGet('/api/v1/chamados').flush(envelope([]));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar as listas.');
    expect(sistemasPrimeiraTentativa.cancelled).toBe(true);

    component['carregarListas']();
    pedidoGet('/api/v1/categorias').flush(envelope(CATEGORIAS));
    pedidoGet('/api/v1/sistemas-equipamentos').flush(envelope(SISTEMAS));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Não foi possível carregar as listas.');
    expect(component['categorias']()).toEqual(CATEGORIAS);
  });

  it('deve enviar o id da categoria e o do sistema escolhidos', () => {
    abrirTela();
    responderCarga();
    preencher({ categoriaId: 'cat-acesso', sistemaId: 'sis-agenda' });

    registrar();

    const body = requisicaoDeRegistro().request.body as ChamadoRequest;
    expect(body.categoria_id).toBe('cat-acesso');
    expect(body.sistema_equipamento_id).toBe('sis-agenda');
    expect(body).not.toHaveProperty('categoria');
    expect(body).not.toHaveProperty('sistema');
  });

  it('deve omitir o sistema quando ele não é informado', () => {
    abrirTela();
    responderCarga();
    preencher({ sistemaId: '' });

    registrar();

    const body = requisicaoDeRegistro().request.body as ChamadoRequest;
    expect(body.categoria_id).toBe('cat-equip');
    expect(body).not.toHaveProperty('sistema_equipamento_id');
  });

  // ---------- validação ----------

  it.each([
    ['unidade', { unidadeId: '' }],
    ['contato', { contato: '' }],
    ['categoria', { categoriaId: '' }],
    ['descrição', { descricao: '' }],
  ])('deve bloquear o envio sem %s', (_campo, valores) => {
    abrirTela();
    responderCarga();
    preencher(valores);

    registrar();

    nenhumRegistroEnviado();
    expect(component['form'].invalid).toBe(true);
  });

  it('deve bloquear o envio com descrição de menos de 10 caracteres', () => {
    abrirTela();
    responderCarga();
    preencher({ descricao: 'Quebrou' });

    registrar();

    nenhumRegistroEnviado();
    expect(component['form'].controls.descricao.hasError('minlength')).toBe(true);
  });

  // ---------- payload ----------

  it('deve omitir o nome vazio e enviar o telefone em E.164', () => {
    abrirTela();
    responderCarga();
    preencher({ nome: '   ', contato: '(81) 98877-1234' });

    registrar();

    const body = requisicaoDeRegistro().request.body as ChamadoRequest;
    expect(body.unidade_id).toBe('u-bvg');
    expect(body.solicitante).toEqual({ contato: '+5581988771234' });
    expect(body.descricao).toBe('Impressora de etiquetas da coleta parou de imprimir.');
  });

  it('deve enviar o e-mail do solicitante em minúsculas, junto com o nome', () => {
    abrirTela();
    responderCarga();
    preencher({ nome: 'Recepção Boa Viagem', contato: 'Recepcao.BVG@Lumen.Local' });

    registrar();

    const body = requisicaoDeRegistro().request.body as ChamadoRequest;
    expect(body.solicitante).toEqual({
      nome: 'Recepção Boa Viagem',
      contato: 'recepcao.bvg@lumen.local',
    });
  });

  // ---------- telemetria M11 (FE-01) ----------

  it('deve enviar como registro_iniciado_em o instante em que a tela abriu, não o do envio', () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(ABERTURA_DA_TELA);
    abrirTela();
    responderCarga();
    preencher({});

    vi.setSystemTime(new Date(ABERTURA_DA_TELA.getTime() + 45_000)); // o analista leva 45 s
    registrar();

    const body = requisicaoDeRegistro().request.body as ChamadoRequest;
    expect(body.registro_iniciado_em).toBe('2026-10-22T13:00:00.000Z');
  });

  it('deve começar uma nova medição a cada vez que a tela é aberta', () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(ABERTURA_DA_TELA);
    abrirTela();
    responderCarga();
    preencher({});
    registrar();
    const primeiro = (requisicaoDeRegistro().request.body as ChamadoRequest).registro_iniciado_em!;

    vi.setSystemTime(new Date(ABERTURA_DA_TELA.getTime() + 5 * 60_000));
    abrirTela();
    responderCarga();
    preencher({});
    registrar();
    const segundo = (requisicaoDeRegistro().request.body as ChamadoRequest).registro_iniciado_em!;

    expect(segundo).toBe('2026-10-22T13:05:00.000Z');
    expect(Date.parse(segundo)).toBeGreaterThan(Date.parse(primeiro));
  });

  it('não deve mostrar a telemetria na tela', () => {
    abrirTela();
    responderCarga();

    expect(fixture.nativeElement.textContent).not.toContain('registro_iniciado_em');
  });

  // ---------- resposta da API ----------

  it('deve avisar o código e a origem do dado e voltar para a fila após o sucesso', () => {
    const router = TestBed.inject(Router);
    const navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    abrirTela();
    responderCarga();
    preencher({});

    registrar();
    requisicaoDeRegistro().flush({ ...envelope(CHAMADO_CRIADO), status_code: 201 });

    const mensagens = TestBed.inject(ToastService)
      .toasts()
      .map((t) => t.mensagem);
    expect(mensagens).toContain('Chamado LUM-2026-000124 registrado com sucesso (dado SIMULADO).');
    expect(navegar).toHaveBeenCalledWith(['/chamados']);
  });

  it('não deve marcar a origem quando o dado é REAL', () => {
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    abrirTela();
    responderCarga();
    preencher({});

    registrar();
    requisicaoDeRegistro().flush(envelope({ ...CHAMADO_CRIADO, origem_dado: 'REAL' }));

    const mensagens = TestBed.inject(ToastService)
      .toasts()
      .map((t) => t.mensagem);
    expect(mensagens).toContain('Chamado LUM-2026-000124 registrado com sucesso.');
  });

  it('deve mostrar as mensagens de erro da API quando ela recusa o chamado', () => {
    abrirTela();
    responderCarga();
    preencher({});

    registrar();
    requisicaoDeRegistro().flush(
      {
        status_code: 400,
        message: 'Erro de validação',
        data: null,
        errors: [
          "Campo 'categoria_id' é obrigatório.",
          'Unidade inativa não pode receber novos chamados.',
        ],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    fixture.detectChanges();

    const caixa = fixture.nativeElement.querySelector('#erros-api') as HTMLElement;
    expect(caixa.getAttribute('role')).toBe('alert');
    expect(caixa.textContent).toContain("Campo 'categoria_id' é obrigatório.");
    expect(caixa.textContent).toContain('Unidade inativa não pode receber novos chamados.');
  });
});
