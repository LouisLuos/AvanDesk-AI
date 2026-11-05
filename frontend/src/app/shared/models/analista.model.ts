/**
 * Analista de suporte da Rede Lúmen (HU10). O perfil é sempre ANALISTA_SUPORTE.
 * Endpoints:
 *   GET   /analistas?ativo=true|false   (qualquer usuário autenticado)
 *   POST  /analistas                    (Liderança de TI; devolve a senha temporária)
 *   PUT   /analistas/{id}
 *   PATCH /analistas/{id}/desativar
 *   PATCH /analistas/{id}/reativar
 */
export interface Analista {
  id: string;
  nome: string;
  email: string;
  ativo: boolean;
  criado_em: string;
}

export interface AnalistaRequest {
  nome: string;
  email: string;
}

/** Resposta do cadastro: a senha temporária só aparece aqui. */
export interface AnalistaCadastrado {
  analista: Analista;
  senha_temporaria: string;
}
