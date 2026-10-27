import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/**
 * Modal genérico com conteúdo projetado.
 * Uso: <app-modal [aberto]="x()" titulo="..." (fechar)="..."> conteúdo </app-modal>
 */
@Component({
  selector: 'app-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'aberto() && fechar.emit()' },
  template: `
    @if (aberto()) {
      <div class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div
          class="absolute inset-0 bg-slate-950/70 backdrop-blur-sm"
          (click)="fechar.emit()"
          aria-hidden="true"
        ></div>
        <section
          class="card animate-fade-in-up relative w-full max-w-lg p-6"
          role="dialog"
          aria-modal="true"
          [attr.aria-labelledby]="idTitulo"
        >
          <header class="mb-5 flex items-start justify-between gap-4">
            <div>
              <h2 [id]="idTitulo" class="text-lg font-semibold text-white">{{ titulo() }}</h2>
              @if (subtitulo()) {
                <p class="mt-1 text-sm text-slate-400">{{ subtitulo() }}</p>
              }
            </div>
            <button
              type="button"
              class="btn btn-ghost -mt-1 -mr-2"
              aria-label="Fechar"
              (click)="fechar.emit()"
            >
              ✕
            </button>
          </header>
          <ng-content />
        </section>
      </div>
    }
  `,
})
export class Modal {
  private static contador = 0;

  readonly aberto = input.required<boolean>();
  readonly titulo = input.required<string>();
  readonly subtitulo = input<string>();
  readonly fechar = output<void>();

  protected readonly idTitulo = `modal-titulo-${++Modal.contador}`;
}
