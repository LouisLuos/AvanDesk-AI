/**
 * Envelope padrão de todas as respostas da API (docs/06-documentacao-api-mvp.md).
 * O backend serializa os campos em snake_case.
 */
export interface ApiResponse<T> {
  status_code: number;
  message: string;
  data: T | null;
  errors: string[] | null;
}
