import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import {
  ChamadoResumo,
  FilaChamados as PaginaFila,
  StatusChamado,
} from '../../../shared/models/chamado.model';
import { Unidade } from '../../../shared/models/unidade.model';
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

  it('deve renderizar os 4 dropdowns customizados de filtros no DOM', () => {
    const el = fixture.nativeElement;
    expect(el.querySelector('#filtro-unidade')).toBeTruthy();
    expect(el.querySelector('#filtro-status')).toBeTruthy();
    expect(el.querySelector('#filtro-prioridade')).toBeTruthy();
    expect(el.querySelector('#filtro-canal')).toBeTruthy();
  });

  it('deve atualizar o estado de filtro ativo e resetar página ao filtrar', () => {
    component['atualizarFiltro'](component['filtroStatus'], 'ABERTO');
    fixture.detectChanges();

    expect(component['filtroStatus']()).toBe('ABERTO');
    expect(component['temFiltroAtivo']()).toBe(true);
    expect(component['pagina']()).toBe(1);

    component['limparFiltros']();
    fixture.detectChanges();

    expect(component['filtroStatus']()).toBe('');
    expect(component['temFiltroAtivo']()).toBe(false);
  });

  it('deve possuir rótulo e estilo configurados para todos os 9 status do domínio (HU05)', () => {
    const todosOs9Status: StatusChamado[] = [
      'ABERTO',
      'EM_TRIAGEM',
      'EM_ATENDIMENTO',
      'AGUARDANDO',
      'RESOLVIDO',
      'REABERTO',
      'FECHADO',
      'CANCELADO',
      'MESCLADO',
    ];

    for (const status of todosOs9Status) {
      expect(component['rotuloStatus'](status)).toBeTruthy();
      expect(component['estiloStatus'](status)).toContain('bg-');
    }
  });
});

describe('FilaChamados integrada ao GET /chamados (HU14)', () => {
  let component: FilaChamados;
  let fixture: ComponentFixture<FilaChamados>;
  let http: HttpTestingController;

  const UNIDADES: Unidade[] = [
    {
      id: 'u-old',
      nome: 'U.09 Olinda',
      identificacao: 'LUM-OLD',
      atendimento_24h: false,
      ativo: false,
    },
    {
      id: 'u-bvg',
      nome: 'U.03 Boa Viagem',
      identificacao: 'LUM-BVG',
      atendimento_24h: false,
      ativo: true,
    },
  ];

  function chamado(numero: number, extra: Partial<ChamadoResumo> = {}): ChamadoResumo {
    return {
      id: `id-${numero}`,
      codigo: `LUM-2026-${String(numero).padStart(6, '0')}`,
      titulo: `Chamado ${numero}`,
      unidade: { id: 'u-bvg', nome: 'U.03 Boa Viagem' },
      categoria: { id: 'cat-1', nome: 'Equipamento de apoio' },
      canal_origem: 'LIGACAO',
      status: 'ABERTO',
      prioridade: null,
      responsavel: { id: 'usr-1', nome: 'Ana Souza' },
      aberto_em: new Date().toISOString(),
      atualizado_em: new Date().toISOString(),
      ...extra,
    };
  }

  function pagina(extra: Partial<PaginaFila> = {}): PaginaFila {
    return {
      itens: [chamado(127)],
      pagina: 0,
      tamanho: 20,
      total_itens: 1,
      total_paginas: 1,
      totais_por_status: {
        ABERTO: 1,
        EM_TRIAGEM: 0,
        EM_ATENDIMENTO: 0,
        AGUARDANDO: 0,
        RESOLVIDO: 0,
        REABERTO: 0,
        FECHADO: 0,
        CANCELADO: 0,
        MESCLADO: 0,
      },
      ...extra,
    };
  }

  function envelope<T>(data: T) {
    return { status_code: 200, message: 'ok', data, errors: null };
  }

  function configurar(busca?: string): void {
    TestBed.configureTestingModule({
      imports: [FilaChamados],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        ...(busca
          ? [
              {
                provide: ActivatedRoute,
                useValue: { queryParamMap: of(convertToParamMap({ busca })) },
              },
            ]
          : []),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(FilaChamados);
    component = fixture.componentInstance;
    fixture.detectChanges();
    http.expectOne((req) => req.url === '/api/v1/unidades').flush(envelope(UNIDADES));
  }

  function pedidoDaFila(): TestRequest {
    return http.expectOne((req) => req.method === 'GET' && req.url === '/api/v1/chamados');
  }

  function responder(fila: PaginaFila = pagina()): TestRequest {
    const pedido = pedidoDaFila();
    pedido.flush(envelope(fila));
    fixture.detectChanges();
    return pedido;
  }

  /** Parâmetros enviados, sem o page e o size. */
  function filtrosEnviados(pedido: TestRequest): Record<string, string[]> {
    const params = pedido.request.params;
    return Object.fromEntries(
      params
        .keys()
        .filter((nome) => nome !== 'page' && nome !== 'size')
        .map((nome) => [nome, params.getAll(nome) ?? []]),
    );
  }

  function texto(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  afterEach(() => http.verify());

  // ---------- carga inicial ----------

  it('deve pedir a primeira página com 20 itens e sem filtros', () => {
    configurar();

    const pedido = responder();

    expect(pedido.request.params.get('page')).toBe('0');
    expect(pedido.request.params.get('size')).toBe('20');
    expect(filtrosEnviados(pedido)).toEqual({});
  });

  it('deve montar o filtro de unidades com todas as unidades, em ordem alfabética', () => {
    configurar();
    responder();

    expect(component['unidadesOpcoes']()).toEqual([
      { valor: 'u-bvg', rotulo: 'U.03 Boa Viagem' },
      { valor: 'u-old', rotulo: 'U.09 Olinda' },
    ]);
  });

  it('deve mostrar a faixa e as páginas informadas pelo servidor', () => {
    configurar();
    responder(
      pagina({
        itens: Array.from({ length: 20 }, (_, i) => chamado(200 - i)),
        total_itens: 45,
        total_paginas: 3,
      }),
    );

    expect(texto()).toContain('Mostrando 1 a 20 de 45 chamados');
    expect(component['paginas']()).toEqual([1, 2, 3]);
  });

  it('deve mostrar no máximo 5 botões de página ao redor da atual', () => {
    configurar();
    responder(pagina({ total_itens: 400, total_paginas: 20 }));

    component['irParaPagina'](10);
    responder(pagina({ pagina: 9, total_itens: 400, total_paginas: 20 }));

    expect(component['paginas']()).toEqual([8, 9, 10, 11, 12]);
  });

  // ---------- filtros e paginação no servidor ----------

  it('deve enviar status, unidade, prioridade e canal e voltar para a página 1 ao filtrar', () => {
    configurar();
    responder(pagina({ total_itens: 60, total_paginas: 3 }));
    component['irParaPagina'](3);
    responder(pagina({ pagina: 2, total_itens: 60, total_paginas: 3 }));

    component['atualizarFiltro'](component['filtroUnidade'], 'u-bvg');
    responder();
    component['atualizarFiltro'](component['filtroPrioridade'], 'ALTA');
    responder();
    component['atualizarFiltro'](component['filtroCanal'], 'WHATSAPP');
    responder();
    component['atualizarFiltro'](component['filtroStatus'], 'EM_ATENDIMENTO');
    const pedido = responder();

    expect(pedido.request.params.get('page')).toBe('0');
    expect(filtrosEnviados(pedido)).toEqual({
      status: ['EM_ATENDIMENTO'],
      unidade_id: ['u-bvg'],
      prioridade: ['ALTA'],
      canal_origem: ['WHATSAPP'],
    });
  });

  it('deve manter os filtros ao trocar de página', () => {
    configurar();
    responder();
    component['atualizarFiltro'](component['filtroUnidade'], 'u-bvg');
    responder(pagina({ total_itens: 45, total_paginas: 3 }));

    component['irParaPagina'](2);
    const pedido = responder(pagina({ pagina: 1, total_itens: 45, total_paginas: 3 }));

    expect(pedido.request.params.get('page')).toBe('1');
    expect(filtrosEnviados(pedido)).toEqual({ unidade_id: ['u-bvg'] });
  });

  it('deve enviar sem_prioridade quando a opção "Sem prioridade" é escolhida', () => {
    configurar();
    responder();

    component['atualizarFiltro'](component['filtroPrioridade'], component['semPrioridade']);

    expect(filtrosEnviados(responder())).toEqual({ sem_prioridade: ['true'] });
  });

  it('deve tratar "meus" e "sem responsável" como opções que se excluem', () => {
    configurar();
    responder();

    component['alternarMeus'](true);
    expect(filtrosEnviados(responder())).toEqual({ meus: ['true'] });

    component['alternarSemResponsavel'](true);
    expect(filtrosEnviados(responder())).toEqual({ sem_responsavel: ['true'] });
    expect(component['somenteMeus']()).toBe(false);

    component['alternarMeus'](true);
    expect(filtrosEnviados(responder())).toEqual({ meus: ['true'] });
    expect(component['somenteSemResponsavel']()).toBe(false);
  });

  it('deve limpar todos os filtros e recarregar sem parâmetros', () => {
    configurar();
    responder();
    component['alternarSemResponsavel'](true);
    responder();

    component['limparFiltros']();

    expect(filtrosEnviados(responder())).toEqual({});
    expect(component['temFiltroAtivo']()).toBe(false);
  });

  // ---------- busca pelo código ----------

  it('deve buscar pelo código só ao apertar Enter e voltar à lista ao apagar o campo', () => {
    configurar();
    responder();

    component['digitarBusca']('lum-2026-000127');
    http.expectNone((req) => req.url === '/api/v1/chamados');

    component['aplicarBusca']();
    expect(filtrosEnviados(responder())).toEqual({ codigo: ['lum-2026-000127'] });

    component['digitarBusca']('');
    expect(filtrosEnviados(responder())).toEqual({});
  });

  it('deve usar a busca do cabeçalho (?busca=) já na primeira carga', () => {
    configurar('LUM-2026-000127');

    expect(filtrosEnviados(responder())).toEqual({ codigo: ['LUM-2026-000127'] });
    expect(component['busca']()).toBe('LUM-2026-000127');
  });

  // ---------- conteúdo da tela ----------

  it('deve montar os cartões a partir dos totais do servidor, com Estourados sem dado', () => {
    configurar();
    const totais = {
      ...pagina().totais_por_status,
      ABERTO: 12,
      REABERTO: 2,
      EM_TRIAGEM: 5,
      EM_ATENDIMENTO: 9,
      AGUARDANDO: 4,
    };
    responder(pagina({ totais_por_status: totais }));

    const valores = Object.fromEntries(component['indicadores']().map((i) => [i.id, i.valor]));
    expect(valores).toEqual({
      abertos: 14,
      triagem: 5,
      atendimento: 9,
      aguardando: 4,
      estourados: null,
    });
    expect(fixture.nativeElement.querySelector('#indicador-estourados').textContent).toContain('—');
  });

  it('deve destacar chamado sem responsável e mostrar "—" no prazo quando não há SLA', () => {
    configurar();
    responder(pagina({ itens: [chamado(126, { responsavel: null })] }));

    const linha = (fixture.nativeElement as HTMLElement).querySelector('tbody tr') as HTMLElement;
    const naoAtribuido = Array.from(linha.querySelectorAll('span')).find((s) =>
      s.textContent?.includes('Não atribuído'),
    );
    expect(naoAtribuido?.className).toContain('text-[#dc2626]');
    expect(linha.textContent).toContain('—');
    expect(linha.querySelector('a')?.getAttribute('href')).toBe('/chamados/id-126');
  });

  it('deve avisar quando a fila não carrega e permitir tentar de novo', () => {
    configurar();
    pedidoDaFila().flush(
      { status_code: 500, message: 'Erro interno', data: null, errors: null },
      { status: 500, statusText: 'Erro' },
    );
    fixture.detectChanges();

    expect(texto()).toContain('Não foi possível carregar a fila');

    component['carregar']();
    responder();
    expect(texto()).not.toContain('Não foi possível carregar a fila');
    expect(texto()).toContain('LUM-2026-000127');
  });

  it('deve ignorar a resposta antiga quando o filtro muda antes dela chegar', () => {
    configurar();
    const primeiro = pedidoDaFila();

    component['atualizarFiltro'](component['filtroStatus'], 'ABERTO');

    expect(primeiro.cancelled).toBe(true);
    expect(filtrosEnviados(responder())).toEqual({ status: ['ABERTO'] });
  });
});
