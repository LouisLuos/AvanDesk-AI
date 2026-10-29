import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ChamadoService } from '../../../core/services/chamado.service';
import { ToastService } from '../../../core/services/toast.service';
import {
  ChamadoDetalhe,
  ChamadoDetalheApiResponse,
  STATUS_CHAMADO,
  StatusChamado,
} from '../../../shared/models/chamado.model';
import { formatarDataHoraLocal, mapearChamadoDetalhe } from '../../../shared/utils/chamado-detalhe.mapper';
import { DetalheChamado } from './detalhe-chamado';

describe('DetalheChamado Component & Integração HU02 / HU05', () => {
  let component: DetalheChamado;
  let fixture: ComponentFixture<DetalheChamado>;
  let chamadoService: ChamadoService;
  let toastService: ToastService;
  let httpMock: HttpTestingController;

  const mockDetalhe: ChamadoDetalhe = {
    id: 'chm-127',
    codigo: 'LUM-2026-000127',
    titulo: 'Impressora de etiquetas parada na coleta',
    descricao: 'Impressora de etiquetas da coleta parou de imprimir.',
    unidade: { id: 'u-1', nome: 'U.03 Boa Viagem' },
    solicitante: {
      nome: 'Mariana Alves',
      contato: '+5581988771234',
      telefoneMascarado: '+55 81 9****-1234',
    },
    canal_origem: 'LIGACAO',
    status: 'EM_ATENDIMENTO',
    prioridade: 'ALTA',
    categoria: 'Equipamento de apoio',
    sistema: 'Impressora de etiquetas',
    responsavel: { id: 'r-1', nome: 'Ana Souza' },
    aberto_em: '07/10/2026 · 10:02',
    primeira_resposta_em: '10:04',
    evidencias: [{ id: 'ev-1', nome: 'erro.png', enviadoAs: '10:03' }],
    historico: [
      {
        id: 'h-1',
        tipo: 'CHAMADO_ABERTO',
        titulo: 'Chamado aberto via Ligação',
        hora: '10:02',
        dataCompleta: '07/10/2026 10:02',
        estiloDot: 'bg-[#94a3b8]',
      },
    ],
    slaItens: [
      {
        etapa: 'Triagem',
        meta: '15 min',
        situacaoTexto: 'Cumprida em 3 min',
        progresso: 100,
        estiloBarra: 'bg-[#22c55e]',
      },
    ],
    pausasMinutos: 0,
    tempoTerceirosHoras: 0,
    relatosVinculados: [],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DetalheChamado],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => (key === 'id' ? 'chm-127' : null),
              },
            },
          },
        },
      ],
    }).compileComponents();

    chamadoService = TestBed.inject(ChamadoService);
    toastService = TestBed.inject(ToastService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve carregar e exibir os dados do chamado', () => {
    vi.spyOn(chamadoService, 'buscarPorId').mockReturnValue(of(mockDetalhe));

    fixture = TestBed.createComponent(DetalheChamado);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(chamadoService.buscarPorId).toHaveBeenCalledWith('chm-127');
    expect(component['chamado']()?.codigo).toBe('LUM-2026-000127');
    expect(component['carregando']()).toBe(false);

    const elemento = fixture.nativeElement as HTMLElement;
    expect(elemento.textContent).toContain('LUM-2026-000127');
    expect(elemento.textContent).toContain('U.03 Boa Viagem');
    expect(elemento.textContent).toContain('+55 81 9****-1234');
    expect(elemento.textContent).toContain('Ana Souza');
  });

  it('deve exibir "Não atribuído" quando responsavel for nulo sem gerar exceções na UI (HU05)', () => {
    const chamadoSemResponsavel: ChamadoDetalhe = {
      ...mockDetalhe,
      responsavel: null,
      status: 'AGUARDANDO',
      motivo_espera: 'FORNECEDOR',
    };
    vi.spyOn(chamadoService, 'buscarPorId').mockReturnValue(of(chamadoSemResponsavel));

    fixture = TestBed.createComponent(DetalheChamado);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const elemento = fixture.nativeElement as HTMLElement;
    const ddResponsavel = elemento.querySelector('#valor-responsavel-chamado');
    expect(ddResponsavel?.textContent?.trim()).toBe('Não atribuído');
    expect(elemento.querySelector('#badge-motivo-espera')?.textContent).toContain(
      'Fornecedor externo',
    );
  });

  it('deve validar rótulos, cores e badges para todos os 9 status possíveis do domínio (HU05)', () => {
    vi.spyOn(chamadoService, 'buscarPorId').mockReturnValue(of(mockDetalhe));

    fixture = TestBed.createComponent(DetalheChamado);
    component = fixture.componentInstance;
    fixture.detectChanges();

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

    expect(STATUS_CHAMADO).toHaveLength(9);

    for (const status of todosOs9Status) {
      const rotulo = component['rotuloStatus'](status);
      const estilo = component['estiloStatus'](status);
      expect(rotulo).toBeTruthy();
      expect(rotulo).not.toBe(status);
      expect(estilo).toContain('bg-');
      expect(estilo).toContain('text-');
    }
  });

  it('deve conectar ChamadoService.buscarPorId() à API real, ordenar linha do tempo e juntar contatos (HU02)', () => {
    const idReal = '123e4567-e89b-12d3-a456-426614174000';
    const payloadBackend: ChamadoDetalheApiResponse = {
      id: idReal,
      codigo: 'LUM-2026-000001',
      status: 'ABERTO',
      motivo_espera: null,
      descricao: 'Tomógrafo da sala 2 sem comunicação com o PACS.\nExames urgentes na fila.',
      canal_origem: 'WHATSAPP',
      unidade: { id: 'u-1', nome: 'U.03 Boa Viagem' },
      categoria: { id: 'cat-1', nome: 'Sistema hospitalar' },
      sistema_equipamento: { id: 'sis-1', nome: 'Portal de resultados' },
      solicitante: { nome: 'Dr. Roberto Mendes', contato: '+5581999887766' },
      responsavel: null,
      prioridade: null,
      aberto_em: '2026-10-29T12:00:00Z',
      primeira_resposta_em: null,
      origem_dado: 'TESTE',
      registros_contato: [
        {
          id: 'cnt-2',
          canal: 'LIGACAO',
          direcao: 'RECEBIDO',
          conteudo: 'Solicitante ligou reforçando urgência na sala 2.',
          ocorrido_em: '2026-10-29T12:15:00Z',
          solicitante: { nome: 'Dr. Roberto Mendes', contato: '+5581999887766' },
          registrado_por: 'Analista Dev Um',
        },
        {
          id: 'cnt-1',
          canal: 'WHATSAPP',
          direcao: 'RECEBIDO',
          conteudo: 'Tomógrafo da sala 2 sem comunicação com o PACS.',
          ocorrido_em: '2026-10-29T12:00:00Z',
          solicitante: { nome: 'Dr. Roberto Mendes', contato: '+5581999887766' },
          registrado_por: 'Analista Dev Um',
        },
      ],
      historico: [
        // Enviado propositalmente fora de ordem para testar a ordenação cronológica estrita
        {
          id_evento: 'ev-2',
          tipo: 'REGISTRO_DE_CONTATO_RECEBIDO',
          data: '2026-10-29T12:15:00Z',
          autor: 'Analista Dev Um',
          detalhes: { registro_contato_id: 'cnt-2' },
        },
        {
          id_evento: 'ev-1',
          tipo: 'CHAMADO_ABERTO',
          data: '2026-10-29T12:00:00Z',
          autor: 'Analista Dev Um',
          detalhes: { canal: 'WHATSAPP' },
        },
      ],
    };

    let resultado: ChamadoDetalhe | undefined;
    chamadoService.buscarPorId(idReal).subscribe((res) => (resultado = res));

    const req = httpMock.expectOne((r) => r.url.endsWith(`/chamados/${idReal}`) && r.method === 'GET');
    req.flush({
      status_code: 200,
      message: 'Detalhes do chamado recuperados',
      data: payloadBackend,
      errors: null,
    });

    expect(resultado).toBeDefined();
    expect(resultado!.codigo).toBe('LUM-2026-000001');
    expect(resultado!.titulo).toBe('Tomógrafo da sala 2 sem comunicação com o PACS.');
    expect(resultado!.categoria).toBe('Sistema hospitalar');
    expect(resultado!.sistema).toBe('Portal de resultados');
    expect(resultado!.solicitante.telefoneMascarado).toBe('+55 81 9****-7766');
    expect(resultado!.responsavel).toBeNull();

    // Verifica ordenação cronológica estrita (ev-1 às 12:00 UTC antes de ev-2 às 12:15 UTC)
    expect(resultado!.historico).toHaveLength(2);
    expect(resultado!.historico[0].id).toBe('ev-1');
    expect(resultado!.historico[0].titulo).toBe('Chamado aberto via WhatsApp');
    expect(resultado!.historico[1].id).toBe('ev-2');
    expect(resultado!.historico[1].titulo).toContain('Contato registrado via Ligação');
    expect(resultado!.historico[1].descricao).toContain(
      'Solicitante ligou reforçando urgência na sala 2.',
    );

    // Verifica ordenação e mapeamento dos registros de contato associados
    expect(resultado!.registrosContato).toHaveLength(2);
    expect(resultado!.registrosContato![0].id).toBe('cnt-1');
    expect(resultado!.registrosContato![1].id).toBe('cnt-2');

    // Verifica conversão de timestamp UTC para o fuso local do navegador
    const dataLocalEsperada = formatarDataHoraLocal('2026-10-29T12:00:00Z');
    expect(resultado!.aberto_em).toBe(dataLocalEsperada);
    expect(resultado!.historico[0].dataCompleta).toBe(dataLocalEsperada);
  });

  it('deve exibir mensagem de erro ao falhar na busca', () => {
    vi.spyOn(chamadoService, 'buscarPorId').mockReturnValue(
      throwError(() => new Error('Erro 404')),
    );

    fixture = TestBed.createComponent(DetalheChamado);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component['erro']()).toBe('Não foi possível carregar os detalhes do chamado.');
    const elemento = fixture.nativeElement as HTMLElement;
    expect(elemento.textContent).toContain('Não foi possível carregar os detalhes do chamado.');
  });

  it('deve disparar toast informativo ao clicar em ações de levas futuras', () => {
    vi.spyOn(chamadoService, 'buscarPorId').mockReturnValue(of(mockDetalhe));
    const toastSpy = vi.spyOn(toastService, 'info');

    fixture = TestBed.createComponent(DetalheChamado);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component['acaoNaoDisponivel']('Avaliar prioridade', 'Leva 2 (HU04)');
    expect(toastSpy).toHaveBeenCalledWith(
      'Ação "Avaliar prioridade" planejada para a Leva 2 (HU04).',
    );
  });
});
