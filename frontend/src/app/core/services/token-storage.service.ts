import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

const CHAVE_TOKEN = 'avandesk.access_token';
const CHAVE_TOKEN_LEGADA = 'avandesk_token';

/**
 * Mantido por compatibilidade de assinatura após a migração para cookies HttpOnly (HU26).
 * Não persiste nem expõe tokens JWT no navegador para evitar exfiltração via XSS.
 */
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  obter(): string | null {
    return null;
  }

  salvar(_token: string, _persistente = false): void {
    this.limpar();
  }

  limpar(): void {
    if (this.isBrowser) {
      globalThis.sessionStorage?.removeItem(CHAVE_TOKEN);
      globalThis.localStorage?.removeItem(CHAVE_TOKEN);
      globalThis.sessionStorage?.removeItem(CHAVE_TOKEN_LEGADA);
      globalThis.localStorage?.removeItem(CHAVE_TOKEN_LEGADA);
    }
  }
}
