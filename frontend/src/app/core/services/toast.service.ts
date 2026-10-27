import { Injectable, signal } from '@angular/core';

export type TipoToast = 'sucesso' | 'erro' | 'info';

export interface Toast {
  id: number;
  tipo: TipoToast;
  mensagem: string;
}

/** Notificações temporárias exibidas no canto da tela. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private sequencia = 0;
  readonly toasts = signal<Toast[]>([]);

  sucesso(mensagem: string): void {
    this.exibir('sucesso', mensagem);
  }

  erro(mensagem: string): void {
    this.exibir('erro', mensagem);
  }

  info(mensagem: string): void {
    this.exibir('info', mensagem);
  }

  fechar(id: number): void {
    this.toasts.update((lista) => lista.filter((t) => t.id !== id));
  }

  private exibir(tipo: TipoToast, mensagem: string, duracaoMs = 4000): void {
    const id = ++this.sequencia;
    this.toasts.update((lista) => [...lista, { id, tipo, mensagem }]);
    setTimeout(() => this.fechar(id), duracaoMs);
  }
}
