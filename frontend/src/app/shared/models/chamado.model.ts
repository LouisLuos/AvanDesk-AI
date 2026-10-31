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

/** Uma linha da fila (GET /chamados, HU14). */
export interface ChamadoResumo {
  id: string;
  codigo: string;
  /** Primeira linha da descrição, com até 80 caracteres. */
  titulo: string;
  unidade: { id: string; nome: string };
  categoria: ItemLista | null;
  canal_origem: CanalOrigem;
  status: StatusChamado;
  /** Prioridade final; nula enquanto não houver triagem. */
  prioridade: PrioridadeChamado | null;
  /** Ainda não vem da API (o cálculo de SLA é de outra HU); só o modo simulado preenche. */
  sla?: SlaChamado | null;
  /** Nulo quando o chamado não tem responsável ("Não atribuído"). */
  responsavel: ResponsavelChamado | null;
  aberto_em: string;
  /** Último evento do histórico; sem eventos, a data de abertura. */
  atualizado_em: string;
}

/** Itens por página da fila (HU14). */
export const TAMANHO_PAGINA_FILA = 20;

/** Filtros da fila (GET /chamados). Todos opcionais; os informados se somam. */
export interface FiltrosFila {
  status?: StatusChamado[];
  unidade_id?: string;
  prioridade?: PrioridadeChamado;
  sem_prioridade?: boolean;
  canal_origem?: CanalOrigem;
  responsavel_id?: string;
  sem_responsavel?: boolean;
  /** O servidor identifica o usuário pelo token. */
  meus?: boolean;
  /** Código exato (LUM-AAAA-NNNNNN), sem diferenciar maiúsculas. */
  codigo?: string;
}

/** Página da fila, do mais recente para o mais antigo (GET /chamados). */
export interface FilaChamados {
  itens: ChamadoResumo[];
  /** Número da página, começando em 0. */
  pagina: number;
  tamanho: number;
  total_itens: number;
  total_paginas: number;
  /** Contagem com os mesmos filtros, menos o de status. Sempre traz os 9 status. */
  totais_por_status: Record<StatusChamado, number>;
}

/** Payload para atribuir/assumir responsável (HU12). */
export interface ResponsavelRequest {
  responsavel_id: string;
  responsavelId?: string;
}

/** Payload para atualizar status do chamado (HU12). */
export interface StatusRequest {
  status: StatusChamado;
  motivo_espera?: MotivoEspera | null;
  motivo_cancelamento?: string | null;
  motivoEspera?: MotivoEspera | null;
  motivoCancelamento?: string | null;
}
