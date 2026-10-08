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

export interface ChamadoRequest {
  unidade_id: string;
  canal_origem: CanalOrigem;
  solicitante: Solicitante;
  descricao: string;
}

export interface ChamadoResponse {
  id: string;
  codigo: string;
  status: string;
  data_criacao: string;
}
