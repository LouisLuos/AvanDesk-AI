import { InjectionToken } from '@angular/core';
import { environment } from '../../../environments/environment';

/**
 * Indica se a API simulada (mock) está ligada. Vem do environment; os testes podem trocar o valor.
 * Seção 9.1: dado simulado nunca pode ser confundido com dado real.
 */
export const MODO_SIMULADO = new InjectionToken<boolean>('MODO_SIMULADO', {
  providedIn: 'root',
  factory: () => environment.useMocks,
});
