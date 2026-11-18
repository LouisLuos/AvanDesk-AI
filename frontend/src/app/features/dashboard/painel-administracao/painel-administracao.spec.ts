import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DashboardMetricas } from '../../../shared/models/dashboard.model';
import { Unidade } from '../../../shared/models/unidade.model';
import { PainelAdministracao } from './painel-administracao';

describe('PainelAdministracao (HU08)', () => {
  let fixture: ComponentFixture<PainelAdministracao>;
  let http: HttpTestingController;
  let elemento: HTMLElement;

  const UNIDADES: Unidade[] = [
    {
      id: 'u-bvg',
      nome: 'U.03 Boa Viagem',
      identificacao: 'LUM-BVG',
      localizacao: 'Rua Boa Viagem, 123',
      atendimento_24h: false,
      ativo: true,
    },
    {
      id: 'u-rct',
      nome: 'U.07 Recife Centro',
      identificacao: 'LUM-RCT',
      localizacao: 'Av. Agamenon, 400',
      atendimento_24h: true,
      ativo: true,
    },
  ];

  const METRICAS_PADRAO: DashboardMetricas = {
    periodo: {
      inicio: '2026-10-19T00:00:00Z',
      fim: '2026-11-18T00:00:00Z',
      inicio_periodo_anterior: '2026-09-19T00:00:00Z',
      fim_periodo_anterior: '2026-10-19T00:00:00Z',
      unidade_id_filtro: null,
    },
    kpis: {
      chamados_abertos: { total_atual: 182, total_periodo_anterior: 168, variacao_percentual: 8.33 },
      tempo_resolucao: {
        mediano_segundos: 10080,
        medio_segundos: 9600,
        diferenca_mediana_segundos_anterior: -720,
        diferenca_media_segundos_anterior: -600,
      },
      chamados_rastreaveis: {
        percentual: 91,
        meta_inicial_percentual: 90,
        dentro_da_meta: true,
        total_avaliados: 100,
        total_conformes: 91,
      },
      dentro_da_meta_sla: {
        percentual: 84,
        meta_inicial_percentual: 80,
        dentro_da_meta: true,
        total_avaliados: 100,
        total_conformes: 84,
      },
    },
    indicadores_processo: {
      tt_medio_segundos: 11400,
      tpr_medio_segundos: 540,
      tr_medio_segundos: 9600,
      m11_tempo_registro_medio_segundos: 42,
      m08_percentual_divergencia_prioridade: 15,
      m12_incidentes_privacidade: 0,
    },
    alertas: {
      alerta_divergencia_prioridade_m08: false,
      alerta_tempo_registro_m11: false,
      alerta_critico_privacidade_m12: false,
    },
    chamados_por_unidade: [
      {
        unidade_id: 'u-bvg',
        unidade_nome: 'U.03 Boa Viagem',
        unidade_identificacao: 'LUM-BVG',
        total_chamados: 52,
        chamados_resolvidos: 50,
        percentual_dentro_da_meta: 78,
        tempo_medio_atendimento_segundos: 5400,
      },
      {
        unidade_id: 'u-rct',
        unidade_nome: 'U.07 Recife Centro',
        unidade_identificacao: 'LUM-RCT',
        total_chamados: 0,
        chamados_resolvidos: 0,
        percentual_dentro_da_meta: 0,
        tempo_medio_atendimento_segundos: 0,
      },
    ],
    sla_por_prioridade: [
      {
        prioridade: 'ALTA',
        rotulo: 'Alta (P1)',
        meta_segundos: 7200,
        total_avaliados: 30,
        total_dentro_da_meta: 23,
        percentual_dentro_da_meta: 78,
        abaixo_da_meta_inicial: true,
      },
      {
        prioridade: 'MEDIA',
        rotulo: 'Média (P2)',
        meta_segundos: 28800,
        total_avaliados: 40,
        total_dentro_da_meta: 34,
        percentual_dentro_da_meta: 86,
        abaixo_da_meta_inicial: false,
      },
      {
        prioridade: 'BAIXA',
        rotulo: 'Baixa (P3)',
        meta_segundos: 86400,
        total_avaliados: 30,
        total_dentro_da_meta: 28,
        percentual_dentro_da_meta: 93,
        abaixo_da_meta_inicial: false,
      },
    ],
    problemas_recorrentes: [
      {
        problema: 'Lentidão no agendamento',
        ocorrencias: 6,
        ultima_ocorrencia: '2026-09-28T12:00:00Z',
        unidades_mais_afetadas: ['U.07 Recife Centro', 'U.03 Boa Viagem'],
      },
    ],
    tempo_em_terceiros: [
      {
        fornecedor: 'Fornecedor de Agendamento Ltda.',
        total_escalonamentos: 4,
        escalonamentos_concluidos: 4,
        tempo_medio_retorno_segundos: 183600,
      },
    ],
    volume_por_categoria: [{ id: 'cat-1', nome: 'Sistema hospitalar', total_chamados: 80 }],
    volume_por_equipamento: [{ id: 'eq-1', nome: 'Impressora de etiquetas', total_chamados: 45 }],
  };

  function envelope<T>(data: T) {
    return { status_code: 200, message: 'ok', data, errors: null };
  }

  function configurar(): void {
    TestBed.configureTestingModule({
      imports: [PainelAdministracao],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PainelAdministracao);
    elemento = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  }

  function responderCargaInicial(metricas: DashboardMetricas = METRICAS_PADRAO): void {
    http.expectOne('/api/v1/unidades?ativo=true').flush(envelope(UNIDADES));
    const req = http.expectOne((r) => r.url === '/api/v1/dashboard/metricas');
    expect(req.request.params.has('inicio')).toBe(true);
    expect(req.request.params.has('fim')).toBe(true);
    req.flush(envelope(metricas));
    fixture.detectChanges();
  }

  const texto = (seletor: string) => elemento.querySelector(seletor)?.textContent?.replace(/\s+/g, ' ').trim();

  afterEach(() => http.verify());

  it('exibe os 4 KPIs, graficos de unidades/SLA/categorias/equipamentos, recorrencias e terceiros', () => {
    configurar();
    responderCargaInicial();

    expect(texto('#kpi-chamados-abertos')).toContain('182');
    expect(texto('#kpi-chamados-abertos')).toContain('+8% em relação ao período anterior');
    expect(texto('#kpi-tempo-resolucao')).toContain('2 h 48');
    expect(texto('#kpi-tempo-resolucao')).toContain('12 min mais rápido que o período anterior');
    expect(texto('#kpi-chamados-rastreaveis')).toContain('91%');
    expect(texto('#kpi-dentro-meta-sla')).toContain('84%');

    expect(texto('#card-chamados-unidade')).toContain('U.03 Boa Viagem');
    expect(texto('#card-chamados-unidade')).toContain('78% na meta');
    expect(texto('#card-chamados-unidade')).toContain('U.07 Recife Centro');
    expect(texto('#card-chamados-unidade')).toContain('Sem resolvidos');

    expect(texto('#card-sla-prioridade')).toContain('Alta (P1)');
    expect(texto('#card-sla-prioridade')).toContain('78%');
    expect(texto('#card-sla-prioridade')).toContain('93%');

    expect(texto('#card-problemas-recorrentes')).toContain('Lentidão no agendamento');
    expect(texto('#card-problemas-recorrentes')).toContain('U.07 · U.03');

    expect(texto('#card-tempo-terceiros')).toContain('Fornecedor de Agendamento Ltda.');
    expect(texto('#card-tempo-terceiros')).toContain('2 d 3 h');

    expect(texto('#card-volume-categoria')).toContain('Sistema hospitalar');
    expect(texto('#card-volume-equipamento')).toContain('Impressora de etiquetas');

    expect(elemento.querySelector('#alerta-critico-m12')).toBeNull();
    expect(elemento.querySelector('#alerta-m08')).toBeNull();
    expect(elemento.querySelector('#alerta-m11')).toBeNull();
  });

  it('dispara os alertas visuais da Secao 9.7 quando os limiares sao ultrapassados', () => {
    configurar();
    responderCargaInicial({
      ...METRICAS_PADRAO,
      indicadores_processo: {
        ...METRICAS_PADRAO.indicadores_processo,
        m08_percentual_divergencia_prioridade: 35,
        m11_tempo_registro_medio_segundos: 75,
        m12_incidentes_privacidade: 2,
      },
      alertas: {
        alerta_divergencia_prioridade_m08: true,
        alerta_tempo_registro_m11: true,
        alerta_critico_privacidade_m12: true,
      },
    });

    expect(texto('#alerta-critico-m12')).toContain('Alerta crítico de privacidade (M12)');
    expect(texto('#alerta-m08')).toContain('Divergência de prioridade acima de 30% (M08)');
    expect(texto('#alerta-m11')).toContain('Tempo de registro acima de 60 segundos (M11)');
  });

  it('reconsulta o backend ao filtrar por unidade e por periodo', () => {
    configurar();
    responderCargaInicial();

    (elemento.querySelector('#filtro-unidade-btn') as HTMLButtonElement).click();
    fixture.detectChanges();
    const opcaoUnidade = Array.from(elemento.querySelectorAll<HTMLButtonElement>('[role="option"]')).find(
      (b) => b.textContent?.trim() === 'U.03 Boa Viagem',
    );
    opcaoUnidade?.click();
    fixture.detectChanges();

    const reqUnidade = http.expectOne((r) => r.url === '/api/v1/dashboard/metricas');
    expect(reqUnidade.request.params.get('unidade_id')).toBe('u-bvg');
    reqUnidade.flush(envelope(METRICAS_PADRAO));
    fixture.detectChanges();

    (elemento.querySelector('#filtro-periodo-btn') as HTMLButtonElement).click();
    fixture.detectChanges();
    const opcaoPeriodo = Array.from(elemento.querySelectorAll<HTMLButtonElement>('[role="option"]')).find(
      (b) => b.textContent?.trim() === 'Últimos 90 dias',
    );
    opcaoPeriodo?.click();
    fixture.detectChanges();

    const reqPeriodo = http.expectOne((r) => r.url === '/api/v1/dashboard/metricas');
    expect(reqPeriodo.request.params.get('unidade_id')).toBe('u-bvg');
    reqPeriodo.flush(envelope(METRICAS_PADRAO));
    fixture.detectChanges();

    expect(texto('#kpi-chamados-abertos')).toContain('90 dias');
  });

  it('exibe estado de erro e permite recarregar', () => {
    configurar();
    http.expectOne('/api/v1/unidades?ativo=true').flush(envelope(UNIDADES));
    http
      .expectOne((r) => r.url === '/api/v1/dashboard/metricas')
      .flush(null, { status: 500, statusText: 'Erro interno' });
    fixture.detectChanges();

    expect(texto('[role="alert"]')).toContain('Não foi possível carregar os indicadores do painel');

    (elemento.querySelector('#btn-recarregar') as HTMLButtonElement).click();
    fixture.detectChanges();
    const reqRetry = http.expectOne((r) => r.url === '/api/v1/dashboard/metricas');
    reqRetry.flush(envelope(METRICAS_PADRAO));
    fixture.detectChanges();

    expect(texto('#kpi-chamados-abertos')).toContain('182');
  });
});
