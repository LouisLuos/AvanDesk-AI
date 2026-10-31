import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

const CHAVE_TOKEN = 'avandesk.access_token';

/**
 * Armazena o access_token JWT (HU09). Seguro para SSR: no servidor não há storage.
 * Por padrão usa sessionStorage (estações compartilhadas), podendo usar localStorage
 * quando o analista marca "Manter conectado".
 */
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  obter(): string | null {
    if (!this.isBrowser) {
      return null;
    }
    return sessionStorage.getItem(CHAVE_TOKEN) ?? localStorage.getItem(CHAVE_TOKEN);
  }

  salvar(token: string, persistente = false): void {
    if (!this.isBrowser) {
      return;
    }
    if (persistente) {
      sessionStorage.removeItem(CHAVE_TOKEN);
      localStorage.setItem(CHAVE_TOKEN, token);
    } else {
      localStorage.removeItem(CHAVE_TOKEN);
      sessionStorage.setItem(CHAVE_TOKEN, token);
    }
  }

  limpar(): void {
    if (this.isBrowser) {
      sessionStorage.removeItem(CHAVE_TOKEN);
      localStorage.removeItem(CHAVE_TOKEN);
    }
  }
}
