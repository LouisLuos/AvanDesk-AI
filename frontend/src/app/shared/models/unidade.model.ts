/**
 * Unidade da Rede Lúmen (HU11).
 *
 * Contrato provisório — ainda não consta em docs/openapi-mvp.yaml.
 * Endpoints:
 *   GET   /unidades?ativo=true|false
 *   POST  /unidades
 *   PUT   /unidades/{id}
 *   PATCH /unidades/{id}/desativar
 */
export interface Unidade {
  id: string;
  nome: string;
  identificacao: string;
  localizacao?: string | null;
  atendimento_24h: boolean;
  ativo: boolean;
}

export interface UnidadeRequest {
  nome: string;
  identificacao: string;
  localizacao?: string;
  atendimento_24h: boolean;
}
