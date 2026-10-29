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
          class="absolute inset-0 bg-slate-950/50 backdrop-blur-xs transition-opacity"
          (click)="fechar.emit()"
          aria-hidden="true"
        ></div>
        <section
          class="relative w-full max-w-lg rounded-xl border border-[#e2e5ec] bg-white p-6 shadow-xl transition-all"
          role="dialog"
          aria-modal="true"
          [attr.aria-labelledby]="idTitulo"
        >
          <header class="mb-5 flex items-start justify-between gap-4">
            <div>
              <h2 [id]="idTitulo" class="text-base font-bold text-[#14161b]">{{ titulo() }}</h2>
              @if (subtitulo()) {
                <p class="mt-1 text-xs text-[#6b7280]">{{ subtitulo() }}</p>
              }
            </div>
            <button
              type="button"
              class="rounded-md p-1.5 text-[#9aa2b1] hover:bg-[#f1f3f8] hover:text-[#14161b] transition"
              aria-label="Fechar"
              (click)="fechar.emit()"
            >
              <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
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
