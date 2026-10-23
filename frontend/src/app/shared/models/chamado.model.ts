/**
 * Chamado (HU01) — conforme docs/openapi-mvp.yaml (POST /chamados).
 */
export type CanalOrigem = 'LIGACAO' | 'PRESENCIAL' | 'WHATSAPP' | 'EMAIL';

export const CANAIS_ORIGEM: ReadonlyArray<{ valor: CanalOrigem; rotulo: string }> = [
  { valor: 'LIGACAO', rotulo: 'Ligação' },
  { valor: 'WHATSAPP', rotulo: 'WhatsApp' },
  { valor: 'EMAIL', rotulo: 'E-mail' },
  { valor: 'PRESENCIAL', rotulo: 'Presencial' },
];

export interface Solicitante {
  /** Opcional no lofi ("se informado") — omitido do payload quando vazio. */
  nome?: string;
  /** Telefone em E.164 (+5581999999999) ou e-mail. */
  contato: string;
}

/** Item das listas do formulário: GET /categorias e GET /sistemas-equipamentos. */
export interface ItemLista {
  id: string;
  nome: string;
}

export interface ChamadoRequest {
  unidade_id: string;
  canal_origem: CanalOrigem;
  solicitante: Solicitante;
  descricao: string;
  /** Id de uma categoria ativa (GET /categorias). */
  categoria_id: string;
  /** Opcional. Id de um sistema/equipamento ativo (GET /sistemas-equipamentos). */
  sistema_equipamento_id?: string;
  /** Ainda ignorados pelo backend (HU04, HU05 e HU03); a tela já envia. */
  prioridade?: PrioridadeChamado;
  atribuir_a_mim?: boolean;
  chamado_relacionado_id?: string;
  /** Quando a tela Novo Chamado foi aberta, em ISO-8601 UTC (métrica M11, seção 9.5). */
  registro_iniciado_em?: string;
}

/** Marca do dado gravado (seção 9.1): o servidor define, o cliente nunca envia. */
export type OrigemDado = 'REAL' | 'TESTE' | 'SIMULADO';

export interface ChamadoResponse {
  id: string;
  codigo: string;
  status: string;
  data_criacao: string;
  origem_dado?: OrigemDado;
}

/**
 * Evento na Linha do Tempo (Timeline) do chamado (HU02 / Seção 6).
 */
export interface EventoTimeline {
  id: string;
  tipo: string;
  titulo: string;
  descricao?: string;
  autor?: string;
  hora: string;
  dataCompleta: string;
  estiloDot?: string; // Classe de cor do dot (Tailwind)
}

export interface EvidenciaAnexo {
  id: string;
  nome: string;
  enviadoAs: string;
  tamanho?: string;
  tipo?: string;
}

export interface RelatoVinculadoItem {
  id: string;
  codigo: string;
  status: StatusChamado;
  descricao: string;
  vinculadoEm: string;
  vinculadoPor: string;
}

export interface ItemSlaDetalhe {
  etapa: string;
  meta: string;
  situacaoTexto: string;
  progresso: number;
  estiloBarra: string;
}

export interface ChamadoDetalhe {
  id: string;
  codigo: string;
  titulo: string;
  descricao: string;
  unidade: { id: string; nome: string };
  solicitante: {
    nome?: string;
    contato: string;
    telefoneMascarado?: string;
  };
  canal_origem: CanalOrigem;
  status: StatusChamado;
  prioridade: PrioridadeChamado | null;
  categoria: string;
  sistema: string;
  responsavel: ResponsavelChamado | null;
  aberto_em: string;
  primeira_resposta_em?: string | null;
  evidencias: EvidenciaAnexo[];
  historico: EventoTimeline[];
  slaItens: ItemSlaDetalhe[];
  pausasMinutos?: number;
  tempoTerceirosHoras?: number;
  relatosVinculados: RelatoVinculadoItem[];
  origem_dado?: 'REAL' | 'TESTE' | 'SIMULADO';
}


/**
 * Fila de chamados — contrato provisório (GET /chamados).
 * Somente dados técnicos/administrativos: nenhum dado clínico ou de paciente (LGPD / RN01).
 */
export type StatusChamado =
  | 'ABERTO'
  | 'EM_TRIAGEM'
  | 'EM_ATENDIMENTO'
  | 'AGUARDANDO'
  | 'REABERTO'
  | 'RESOLVIDO'
  | 'MESCLADO';

export const STATUS_CHAMADO: ReadonlyArray<{ valor: StatusChamado; rotulo: string }> = [
  { valor: 'ABERTO', rotulo: 'Aberto' },
  { valor: 'EM_TRIAGEM', rotulo: 'Em Triagem' },
  { valor: 'EM_ATENDIMENTO', rotulo: 'Em Atendimento' },
  { valor: 'AGUARDANDO', rotulo: 'Aguardando' },
  { valor: 'REABERTO', rotulo: 'Reaberto' },
  { valor: 'RESOLVIDO', rotulo: 'Resolvido' },
  { valor: 'MESCLADO', rotulo: 'Mesclado' },
];

export type PrioridadeChamado = 'ALTA' | 'MEDIA' | 'BAIXA';

export const PRIORIDADES_CHAMADO: ReadonlyArray<{ valor: PrioridadeChamado; rotulo: string }> = [
  { valor: 'ALTA', rotulo: 'Alta' },
  { valor: 'MEDIA', rotulo: 'Média' },
  { valor: 'BAIXA', rotulo: 'Baixa' },
];

export type SituacaoSla =
  | 'TRIAGEM'
  | 'DENTRO_DA_META'
  | 'EM_RISCO'
  | 'ESTOURADO'
  | 'PAUSADO'
  | 'CUMPRIDO'
  | 'INCORPORADO';

export interface SlaChamado {
  situacao: SituacaoSla;
  /** Texto pronto para exibição, ex.: "Em risco · 12 min". */
  rotulo: string;
  /** Percentual do prazo já consumido (0–100). */
  progresso: number;
}

export interface ResponsavelChamado {
  id: string;
  nome: string;
}

export interface ChamadoResumo {
  id: string;
  codigo: string;
  titulo: string;
  unidade: { id: string; nome: string };
  canal_origem: CanalOrigem;
  status: StatusChamado;
  prioridade: PrioridadeChamado | null;
  sla: SlaChamado;
  responsavel: ResponsavelChamado | null;
  atualizado_em: string;
}
