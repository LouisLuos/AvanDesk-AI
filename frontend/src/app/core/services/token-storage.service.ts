import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

const CHAVE_TOKEN = 'avandesk.access_token';

/**
 * Armazena o access_token JWT (HU09). Seguro para SSR: no servidor não há storage.
 * Usa sessionStorage por causa das estações de trabalho compartilhadas (sessão encerra ao fechar o navegador).
 */
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  obter(): string | null {
    return this.isBrowser ? sessionStorage.getItem(CHAVE_TOKEN) : null;
  }

  salvar(token: string): void {
    if (this.isBrowser) {
      sessionStorage.setItem(CHAVE_TOKEN, token);
    }
  }

  limpar(): void {
    if (this.isBrowser) {
      sessionStorage.removeItem(CHAVE_TOKEN);
    }
  }
}
