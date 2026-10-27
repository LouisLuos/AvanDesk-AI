import { HttpErrorResponse } from '@angular/common/http';
import { ApiResponse } from '../models/api-response.model';

/**
 * Extrai mensagens legíveis de um erro HTTP no formato ApiResponseError.
 */
export function extrairMensagensErro(erro: unknown): string[] {
  if (erro instanceof HttpErrorResponse) {
    const corpo = erro.error as Partial<ApiResponse<unknown>> | null;
    if (corpo?.errors?.length) {
      return corpo.errors;
    }
    if (corpo?.message) {
      return [corpo.message];
    }
    if (erro.status === 0) {
      return ['Não foi possível conectar ao servidor. Verifique sua conexão.'];
    }
  }
  return ['Ocorreu um erro inesperado. Tente novamente.'];
}
