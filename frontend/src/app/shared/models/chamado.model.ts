/**
 * Chamado (HU01, HU02, HU05) — conforme docs/openapi-mvp.yaml.
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

export type MotivoEspera = 'FORNECEDOR' | 'SOLICITANTE' | 'PECA_EQUIPAMENTO';

export type DirecaoContato = 'RECEBIDO' | 'ENVIADO';

export interface ChamadoResponse {
  id: string;
  codigo: string;
  status: StatusChamado;
  data_criacao: string;
  origem_dado?: OrigemDado;
}

/**
 * Tipos de evento da tabela eventos_historico (TipoEvento no backend).
 */
export type TipoEventoHistorico =
  | 'CHAMADO_ABERTO'
  | 'REGISTRO_DE_CONTATO_RECEBIDO'
  | 'TRIAGEM_INICIADA'
  | 'PRIORIDADE_DEFINIDA'
  | 'CHAMADO_TRIADO'
  | 'CHAMADO_ATRIBUIDO'
  | 'CHAMADO_AGUARDANDO'
  | 'RETORNO_RECEBIDO'
  | 'CHAMADO_ESCALADO'
  | 'CHAMADO_RESOLVIDO'
  | 'CHAMADO_REABERTO'
  | 'CHAMADO_FECHADO'
  | 'CHAMADO_CANCELADO'
  | 'RELATO_VINCULADO'
  | 'RELATO_DESVINCULADO'
  | 'CHAMADO_MESCLADO'
  | 'NOTIFICACAO_ENVIADA'
  | 'ANEXO_VISUALIZADO'
  | 'ANOTACAO_ADICIONADA'
  | 'STATUS_ALTERADO';

/**
 * Contrato HTTP bruto retornado por GET /api/v1/chamados/{id} (ChamadoDetalheResponse no backend).
 */
export interface RegistroContatoApiResponse {
  id: string;
  canal: CanalOrigem;
  direcao: DirecaoContato;
  conteudo: string;
  ocorrido_em: string;
  solicitante: {
    nome?: string | null;
    contato: string;
  };
  registrado_por?: string | null;
}

export interface EventoHistoricoApiResponse {
  id_evento: string;
  tipo: TipoEventoHistorico | string;
  data: string;
  autor?: string | null;
  detalhes?: Record<string, unknown> | null;
}

export interface ChamadoDetalheApiResponse {
  id: string;
  codigo: string;
  status: StatusChamado;
  motivo_espera?: MotivoEspera | null;
  descricao: string;
  canal_origem: CanalOrigem;
  unidade: { id: string; nome: string };
  categoria: ItemLista | null;
  sistema_equipamento?: ItemLista | null;
  solicitante: {
    nome?: string | null;
    contato: string;
  };
  responsavel: ResponsavelChamado | null;
  prioridade: PrioridadeChamado | null;
  aberto_em: string;
  primeira_resposta_em?: string | null;
  origem_dado?: OrigemDado;
  registros_contato: RegistroContatoApiResponse[];
  historico: EventoHistoricoApiResponse[];
}

/**
 * Evento na Linha do Tempo (Timeline) do chamado (HU02 / Seção 6).
 */
export interface EventoTimeline {
  id: string;
  tipo: TipoEventoHistorico | string;
  titulo: string;
  descricao?: string;
  autor?: string;
  hora: string;
  dataCompleta: string;
  ocorridoEmIso?: string;
  estiloDot?: string; // Classe de cor do dot (Tailwind)
}

/**
 * Registro de contato formatado para exibição na tela de detalhe (HU02).
 */
export interface RegistroContatoDetalhe {
  id: string;
  canal: CanalOrigem;
  direcao: DirecaoContato;
  conteudo: string;
  dataHoraLocal: string;
  solicitanteNome: string;
  solicitanteContato: string;
  registradoPor?: string | null;
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
  motivo_espera?: MotivoEspera | null;
  prioridade: PrioridadeChamado | null;
  categoria: string;
  sistema: string;
  responsavel: ResponsavelChamado | null;
  aberto_em: string;
  primeira_resposta_em?: string | null;
  registrosContato?: RegistroContatoDetalhe[];
  evidencias: EvidenciaAnexo[];
  historico: EventoTimeline[];
  slaItens: ItemSlaDetalhe[];
  pausasMinutos?: number;
  tempoTerceirosHoras?: number;
  relatosVinculados: RelatoVinculadoItem[];
  origem_dado?: OrigemDado;
}

/**
 * Status do ciclo de vida do chamado (HU05 — 9 estados do domínio).
 * Somente dados técnicos/administrativos: nenhum dado clínico ou de paciente (LGPD / RN01).
 */
export type StatusChamado =
  | 'ABERTO'
  | 'EM_TRIAGEM'
  | 'EM_ATENDIMENTO'
  | 'AGUARDANDO'
  | 'RESOLVIDO'
  | 'REABERTO'
  | 'FECHADO'
  | 'CANCELADO'
  | 'MESCLADO';

export const STATUS_CHAMADO: ReadonlyArray<{ valor: StatusChamado; rotulo: string }> = [
  { valor: 'ABERTO', rotulo: 'Aberto' },
  { valor: 'EM_TRIAGEM', rotulo: 'Em Triagem' },
  { valor: 'EM_ATENDIMENTO', rotulo: 'Em Atendimento' },
  { valor: 'AGUARDANDO', rotulo: 'Aguardando' },
  { valor: 'RESOLVIDO', rotulo: 'Resolvido' },
  { valor: 'REABERTO', rotulo: 'Reaberto' },
  { valor: 'FECHADO', rotulo: 'Fechado' },
  { valor: 'CANCELADO', rotulo: 'Cancelado' },
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
