/**
 * Unidade da Rede Lúmen (HU11).
 *
 * Contrato provisório — ainda não consta em docs/openapi-mvp.yaml.
 * Endpoints esperados:
 *   GET   /unidades?ativo=true|false
 *   POST  /unidades
 *   PUT   /unidades/{id}
 *   PATCH /unidades/{id}/desativar
 */
export interface Unidade {
  id: string;
  nome: string;
  codigo: string;
  ativo: boolean;
}

export interface UnidadeRequest {
  nome: string;
  codigo: string;
}
