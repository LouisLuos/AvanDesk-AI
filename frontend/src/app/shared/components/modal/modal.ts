import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

/**
 * Modal genérico com conteúdo projetado, no padrão visual do modal de status do detalhe.
 * Uso:
 *   <app-modal [aberto]="x()" titulo="..." (fechar)="...">
 *     <span icone>...</span>          (opcional, à esquerda do título)
 *     conteúdo
 *     <ng-container rodape>...</ng-container>   (opcional, botões de ação)
 *   </app-modal>
 */
@Component({
  selector: 'app-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'aberto() && fechar.emit()' },
  template: `
    @if (aberto()) {
      <div class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div
          class="fixed inset-0 bg-slate-950/60 backdrop-blur-xs"
          (click)="fechar.emit()"
          aria-hidden="true"
        ></div>
        <section
          class="animate-fade-in-up relative w-full rounded-2xl border border-[#e2e5ec] bg-white p-6 shadow-2xl"
          [class]="larguraMaxima()"
          role="dialog"
          aria-modal="true"
          [attr.aria-labelledby]="idTitulo"
        >
          <header class="flex items-start justify-between gap-4">
            <div class="flex items-start gap-3">
              <ng-content select="[icone]" />
              <div>
                <h2 [id]="idTitulo" class="text-base font-bold text-[#14161b]">{{ titulo() }}</h2>
                @if (subtitulo()) {
                  <p class="mt-0.5 text-xs text-[#6b7280]">{{ subtitulo() }}</p>
                }
              </div>
            </div>
            <button
              type="button"
              class="rounded-lg p-1 text-[#9aa2b1] transition hover:bg-[#f1f3f8] hover:text-[#14161b] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#2d5bff]"
              aria-label="Fechar"
              (click)="fechar.emit()"
            >
              <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M18 6 6 18" />
                <path d="m6 6 12 12" />
              </svg>
            </button>
          </header>

          <div class="mt-5">
            <ng-content />
          </div>

          <footer class="mt-6 flex items-center justify-end gap-2.5 border-t border-[#f1f3f8] pt-4 empty:hidden">
            <ng-content select="[rodape]" />
          </footer>
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
  /** `md` para confirmações curtas; `lg` para formulários. */
  readonly tamanho = input<'md' | 'lg'>('lg');
  readonly fechar = output<void>();

  protected readonly idTitulo = `modal-titulo-${++Modal.contador}`;
  protected readonly larguraMaxima = computed(() => (this.tamanho() === 'md' ? 'max-w-md' : 'max-w-lg'));
}
