import { Injectable, signal } from '@angular/core';
import {
  MotivoEspera,
  PrioridadeChamado,
  SlaChamado,
  StatusChamado,
} from '../../shared/models/chamado.model';

export interface DadosCalculoSla {
  aberto_em: string;
  status: StatusChamado;
  prioridade?: PrioridadeChamado | null;
  motivo_espera?: MotivoEspera | null;
  resolvido_em?: string | null;
  /** Instante explícito de finalização/cancelamento (ex.: fechado_em ou evento CHAMADO_CANCELADO). */
  encerrado_em?: string | null;
  /** Último evento registrado no chamado (fallback congelado para auditoria, nunca Date.now()). */
  atualizado_em?: string | null;
}

@Injectable({ providedIn: 'root' })
export class SlaService {
  /**
   * Signal de timestamp atualizado periodicamente para reatividade contínua
   * apenas dos chamados ativos (em andamento).
   */
  readonly agora = signal<number>(Date.now());

  constructor() {
    // Atualiza a cada 30 segundos para manter os cronômetros e barras dos chamados ativos sincronizados
    if (typeof window !== 'undefined') {
      setInterval(() => {
        this.agora.set(Date.now());
      }, 30000);
    }
  }

  /**
   * Converte strings de data (ISO-8601 ou formato local DD/MM/YYYY HH:mm) para timestamp numérico seguro.
   */
  parseData(valor?: string | null): number {
    if (!valor) return NaN;
    const str = valor.trim();
    if (/^\d{4}-\d{2}-\d{2}/.test(str)) {
      return new Date(str).getTime();
    }
    const brMatch = str.match(/^(\d{2})\/(\d{2})\/(\d{4})(?:\s+(\d{2}):(\d{2}))?/);
    if (brMatch) {
      const dia = parseInt(brMatch[1], 10);
      const mes = parseInt(brMatch[2], 10) - 1;
      const ano = parseInt(brMatch[3], 10);
      const hora = brMatch[4] ? parseInt(brMatch[4], 10) : 0;
      const min = brMatch[5] ? parseInt(brMatch[5], 10) : 0;
      return new Date(ano, mes, dia, hora, min).getTime();
    }
    return new Date(str).getTime();
  }

  /**
   * Calcula o SLA e a situação da barra em relação à meta de tempo em saúde (HU04 / HU14).
   *
   * Regras de auditoria do relógio:
   * - RESOLVIDO / FECHADO: congela no instante da finalização (resolvido_em -> encerrado_em -> atualizado_em), nunca usa agora().
   * - CANCELADO: congela no instante do cancelamento (encerrado_em -> atualizado_em), nunca entra como estourado nem usa agora().
   * - MESCLADO: incorporado ao chamado principal, relógio encerrado.
   * - Em andamento (ABERTO, EM_TRIAGEM, EM_ATENDIMENTO, REABERTO): relógio segue avançando continuamente via agora(),
   *   inclusive quando estourado, até ser resolvido ou cancelado.
   */
  calcularSla(dados: DadosCalculoSla, timestampAtual?: number): SlaChamado {
    const status = dados.status;

    if (status === 'MESCLADO') {
      return {
        situacao: 'INCORPORADO',
        rotulo: 'Incorporado',
        progresso: 0,
      };
    }

    const dataAbertura = this.parseData(dados.aberto_em);

    if (status === 'CANCELADO') {
      const dataCancelamento = this.obterTimestampCongelado(dados.encerrado_em, dados.atualizado_em);
      if (!isNaN(dataAbertura) && !isNaN(dataCancelamento)) {
        const decorridoCancelamento = Math.max(0, Math.floor((dataCancelamento - dataAbertura) / 60000));
        return {
          situacao: 'CANCELADO',
          rotulo: `Cancelado · ${this.formatarTempo(decorridoCancelamento)}`,
          progresso: 0,
        };
      }
      return {
        situacao: 'CANCELADO',
        rotulo: 'Cancelado',
        progresso: 0,
      };
    }

    if (status === 'RESOLVIDO' || status === 'FECHADO') {
      const dataFinalizacao = this.obterTimestampCongelado(
        dados.resolvido_em,
        dados.encerrado_em,
        dados.atualizado_em,
      );
      if (!isNaN(dataAbertura) && !isNaN(dataFinalizacao)) {
        const decorridoResolucao = Math.max(0, Math.floor((dataFinalizacao - dataAbertura) / 60000));
        return {
          situacao: 'CUMPRIDO',
          rotulo: `Cumprido · ${this.formatarTempo(decorridoResolucao)}`,
          progresso: 100,
        };
      }
      return {
        situacao: 'CUMPRIDO',
        rotulo: 'Cumprido',
        progresso: 100,
      };
    }

    if (isNaN(dataAbertura)) {
      return {
        situacao: 'DENTRO_DA_META',
        rotulo: 'Dentro da meta',
        progresso: 10,
      };
    }

    if (status === 'AGUARDANDO') {
      const motivo = dados.motivo_espera;
      let rotuloMotivo = 'solicitante';
      if (motivo === 'FORNECEDOR') rotuloMotivo = 'fornecedor';
      if (motivo === 'PECA_EQUIPAMENTO') rotuloMotivo = 'peça/equipamento';

      return {
        situacao: 'PAUSADO',
        rotulo: `Pausado · ${rotuloMotivo}`,
        progresso: 40,
      };
    }

    // Somente chamados ativos consomem o signal reativo agora(), mantendo o contador crescendo até resolução/cancelamento
    const agora = timestampAtual ?? this.agora();
    const decorridoMinutos = Math.max(0, Math.floor((agora - dataAbertura) / 60000));

    // Sem prioridade ou Em Triagem inicial (Meta: 15 min de triagem)
    if (!dados.prioridade || status === 'EM_TRIAGEM') {
      const metaTriagemMin = 15;
      const progresso = Math.round((decorridoMinutos / metaTriagemMin) * 100);
      const restantes = metaTriagemMin - decorridoMinutos;

      if (decorridoMinutos > metaTriagemMin) {
        return {
          situacao: 'ESTOURADO',
          rotulo: `Estourado · +${this.formatarTempo(decorridoMinutos - metaTriagemMin)}`,
          progresso: 100,
        };
      }

      if (progresso >= 80) {
        return {
          situacao: 'EM_RISCO',
          rotulo: `Triagem · ${restantes} min`,
          progresso: Math.min(100, progresso),
        };
      }

      return {
        situacao: 'TRIAGEM',
        rotulo: `Triagem · ${restantes} min`,
        progresso: Math.max(5, progresso),
      };
    }

    // Chamados com prioridade definida (Metas de Resolução: Alta = 2h, Média = 8h, Baixa = 24h)
    const metaMinutos = this.obterMetaResolucaoMinutos(dados.prioridade);
    const progresso = Math.round((decorridoMinutos / metaMinutos) * 100);
    const restantes = metaMinutos - decorridoMinutos;

    if (decorridoMinutos > metaMinutos) {
      const excesso = decorridoMinutos - metaMinutos;
      return {
        situacao: 'ESTOURADO',
        rotulo: `Estourado · +${this.formatarTempo(excesso)}`,
        progresso: 100,
      };
    }

    if (progresso >= 80) {
      return {
        situacao: 'EM_RISCO',
        rotulo: `Em risco · ${this.formatarTempo(restantes)}`,
        progresso: Math.min(100, progresso),
      };
    }

    return {
      situacao: 'DENTRO_DA_META',
      rotulo: `Dentro da meta · ${this.formatarTempo(restantes)}`,
      progresso: Math.max(5, progresso),
    };
  }

  private obterTimestampCongelado(...candidatos: Array<string | null | undefined>): number {
    for (const candidato of candidatos) {
      const parsed = this.parseData(candidato);
      if (!isNaN(parsed)) {
        return parsed;
      }
    }
    return NaN;
  }

  private obterMetaResolucaoMinutos(prioridade: PrioridadeChamado): number {
    switch (prioridade) {
      case 'ALTA':
        return 120; // 2 horas
      case 'MEDIA':
        return 480; // 8 horas úteis
      case 'BAIXA':
        return 1440; // 24 horas úteis (3 dias úteis)
      default:
        return 480;
    }
  }

  formatarTempo(minutos: number): string {
    if (minutos < 1) return 'menos de 1 min';
    if (minutos < 60) return `${minutos} min`;
    const horas = Math.floor(minutos / 60);
    const minRestantes = minutos % 60;
    if (minRestantes === 0) return `${horas} h`;
    return `${horas} h ${minRestantes} min`;
  }
}
