export interface PeriodoAnalisado {
  inicio: string;
  fim: string;
  inicio_periodo_anterior: string;
  fim_periodo_anterior: string;
  unidade_id_filtro: string | null;
}

export interface KpiChamadosAbertos {
  total_atual: number;
  total_periodo_anterior: number;
  variacao_percentual: number | null;
}

export interface KpiTempoResolucao {
  mediano_segundos: number;
  medio_segundos: number;
  p90_segundos: number;
  diferenca_mediana_segundos_anterior: number | null;
  diferenca_media_segundos_anterior: number | null;
}

export interface KpiMetaPercentual {
  percentual: number;
  meta_inicial_percentual: number;
  dentro_da_meta: boolean;
  total_avaliados: number;
  total_conformes: number;
}

export interface KpisPrincipais {
  chamados_abertos: KpiChamadosAbertos;
  tempo_resolucao: KpiTempoResolucao;
  chamados_rastreaveis: KpiMetaPercentual;
  dentro_da_meta_sla: KpiMetaPercentual;
}

export interface IndicadoresProcesso {
  tt_medio_segundos: number;
  tpr_medio_segundos: number;
  tr_medio_segundos: number;
  tr_p90_segundos: number;
  m06_taxa_reabertura_percentual: number;
  m07_taxa_duplicidade_percentual: number;
  m07_total_duplicados: number;
  m11_tempo_registro_medio_segundos: number;
  m08_percentual_divergencia_prioridade: number;
  m12_incidentes_privacidade: number;
}

export interface AlertasProcesso {
  alerta_divergencia_prioridade_m08: boolean;
  alerta_tempo_registro_m11: boolean;
  alerta_critico_privacidade_m12: boolean;
}

export interface ChamadosPorUnidade {
  unidade_id: string;
  unidade_nome: string;
  unidade_identificacao: string;
  total_chamados: number;
  chamados_resolvidos: number;
  percentual_dentro_da_meta: number;
  tempo_medio_atendimento_segundos: number;
}

export interface ChamadosPorResponsavel {
  responsavel_id: string | null;
  responsavel_nome: string;
  total_em_aberto: number;
  total_alta_prioridade: number;
}

export interface SlaPorPrioridade {
  prioridade: 'ALTA' | 'MEDIA' | 'BAIXA' | string;
  rotulo: string;
  meta_segundos: number;
  total_avaliados: number;
  total_dentro_da_meta: number;
  percentual_dentro_da_meta: number;
  abaixo_da_meta_inicial: boolean;
}

export interface ProblemaRecorrente {
  problema: string;
  ocorrencias: number;
  ultima_ocorrencia: string;
  unidades_mais_afetadas: string[];
}

export interface TempoTerceirosFornecedor {
  fornecedor: string;
  total_escalonamentos: number;
  escalonamentos_concluidos: number;
  tempo_medio_retorno_segundos: number;
}

export interface VolumeItem {
  id: string;
  nome: string;
  total_chamados: number;
}

export interface DashboardMetricas {
  periodo: PeriodoAnalisado;
  kpis: KpisPrincipais;
  indicadores_processo: IndicadoresProcesso;
  alertas: AlertasProcesso;
  chamados_por_unidade: ChamadosPorUnidade[];
  chamados_por_responsavel: ChamadosPorResponsavel[];
  sla_por_prioridade: SlaPorPrioridade[];
  problemas_recorrentes: ProblemaRecorrente[];
  tempo_em_terceiros: TempoTerceirosFornecedor[];
  volume_por_categoria: VolumeItem[];
  volume_por_equipamento: VolumeItem[];
}
