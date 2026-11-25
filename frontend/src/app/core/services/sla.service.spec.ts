import { TestBed } from '@angular/core/testing';
import { SlaService } from './sla.service';

describe('SlaService — Regras de Auditoria do Relógio de SLA', () => {
  let service: SlaService;

  const ABERTURA = '2026-11-19T10:00:00Z';
  const TRINTA_MIN_DEPOIS = '2026-11-19T10:30:00Z';
  const DEZ_HORAS_DEPOIS_MS = new Date('2026-11-19T20:00:00Z').getTime();
  const VINTE_HORAS_DEPOIS_MS = new Date('2026-11-20T06:00:00Z').getTime();

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SlaService);
  });

  it('congela o contador em RESOLVIDO no momento da finalizacao (resolvido_em), mesmo com o tempo avancando', () => {
    service.agora.set(DEZ_HORAS_DEPOIS_MS);
    const t1 = service.calcularSla({
      aberto_em: ABERTURA,
      status: 'RESOLVIDO',
      prioridade: 'ALTA',
      resolvido_em: TRINTA_MIN_DEPOIS,
      atualizado_em: TRINTA_MIN_DEPOIS,
    });

    service.agora.set(VINTE_HORAS_DEPOIS_MS);
    const t2 = service.calcularSla({
      aberto_em: ABERTURA,
      status: 'RESOLVIDO',
      prioridade: 'ALTA',
      resolvido_em: TRINTA_MIN_DEPOIS,
      atualizado_em: TRINTA_MIN_DEPOIS,
    });

    expect(t1).toEqual({ situacao: 'CUMPRIDO', rotulo: 'Cumprido · 30 min', progresso: 100 });
    expect(t2).toEqual(t1);
  });

  it('congela FECHADO usando atualizado_em como fallback quando resolvido_em nao vier, sem nunca usar agora()', () => {
    service.agora.set(DEZ_HORAS_DEPOIS_MS);
    const t1 = service.calcularSla({
      aberto_em: ABERTURA,
      status: 'FECHADO',
      prioridade: 'MEDIA',
      resolvido_em: null,
      atualizado_em: '2026-11-19T11:15:00Z',
    });

    service.agora.set(VINTE_HORAS_DEPOIS_MS);
    const t2 = service.calcularSla({
      aberto_em: ABERTURA,
      status: 'FECHADO',
      prioridade: 'MEDIA',
      resolvido_em: null,
      atualizado_em: '2026-11-19T11:15:00Z',
    });

    expect(t1).toEqual({ situacao: 'CUMPRIDO', rotulo: 'Cumprido · 1 h 15 min', progresso: 100 });
    expect(t2).toEqual(t1);
  });

  it('congela CANCELADO no instante do cancelamento e nunca marca como estourado', () => {
    service.agora.set(DEZ_HORAS_DEPOIS_MS);
    const t1 = service.calcularSla({
      aberto_em: ABERTURA,
      status: 'CANCELADO',
      prioridade: 'ALTA',
      atualizado_em: '2026-11-19T10:45:00Z',
    });

    service.agora.set(VINTE_HORAS_DEPOIS_MS);
    const t2 = service.calcularSla({
      aberto_em: ABERTURA,
      status: 'CANCELADO',
      prioridade: 'ALTA',
      atualizado_em: '2026-11-19T10:45:00Z',
    });

    expect(t1).toEqual({ situacao: 'CANCELADO', rotulo: 'Cancelado · 45 min', progresso: 0 });
    expect(t2).toEqual(t1);
  });

  it('mantem o tempo de chamados ativos estourados aumentando continuamente ate resolucao ou cancelamento', () => {
    // Prioridade ALTA (meta 2h): 10h apos abertura -> estourado em +8h; 20h apos abertura -> estourado em +18h
    service.agora.set(DEZ_HORAS_DEPOIS_MS);
    const t1 = service.calcularSla({
      aberto_em: ABERTURA,
      status: 'EM_ATENDIMENTO',
      prioridade: 'ALTA',
    });

    service.agora.set(VINTE_HORAS_DEPOIS_MS);
    const t2 = service.calcularSla({
      aberto_em: ABERTURA,
      status: 'EM_ATENDIMENTO',
      prioridade: 'ALTA',
    });

    expect(t1).toEqual({ situacao: 'ESTOURADO', rotulo: 'Estourado · +8 h', progresso: 100 });
    expect(t2).toEqual({ situacao: 'ESTOURADO', rotulo: 'Estourado · +18 h', progresso: 100 });
  });

  it('formata excesso de triagem estourada em horas e minutos (sem +18903 min) e continua aumentando', () => {
    service.agora.set(DEZ_HORAS_DEPOIS_MS);
    const t1 = service.calcularSla({
      aberto_em: ABERTURA,
      status: 'ABERTO',
      prioridade: null,
    });

    service.agora.set(VINTE_HORAS_DEPOIS_MS);
    const t2 = service.calcularSla({
      aberto_em: ABERTURA,
      status: 'ABERTO',
      prioridade: null,
    });

    expect(t1).toEqual({ situacao: 'ESTOURADO', rotulo: 'Estourado · +9 h 45 min', progresso: 100 });
    expect(t2).toEqual({ situacao: 'ESTOURADO', rotulo: 'Estourado · +19 h 45 min', progresso: 100 });
  });
});
