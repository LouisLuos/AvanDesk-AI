import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast-container',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      class="pointer-events-none fixed right-4 bottom-4 z-[60] flex w-full max-w-sm flex-col gap-2"
      aria-live="polite"
    >
      @for (toast of toastService.toasts(); track toast.id) {
        <div
          class="animate-fade-in-up pointer-events-auto flex items-start gap-3 rounded-xl border px-4 py-3 text-sm shadow-xl backdrop-blur"
          [class]="estilos[toast.tipo]"
          role="status"
        >
          <span class="mt-0.5 h-2 w-2 shrink-0 rounded-full" [class]="pontos[toast.tipo]"></span>
          <p class="flex-1">{{ toast.mensagem }}</p>
          <button
            type="button"
            class="text-current opacity-60 transition hover:opacity-100"
            aria-label="Fechar notificação"
            (click)="toastService.fechar(toast.id)"
          >
            ✕
          </button>
        </div>
      }
    </div>
  `,
})
export class ToastContainer {
  protected readonly toastService = inject(ToastService);

  protected readonly estilos = {
    sucesso: 'border-emerald-500/30 bg-emerald-950/80 text-emerald-200',
    erro: 'border-rose-500/30 bg-rose-950/80 text-rose-200',
    info: 'border-indigo-500/30 bg-indigo-950/80 text-indigo-200',
  };

  protected readonly pontos = {
    sucesso: 'bg-emerald-400',
    erro: 'bg-rose-400',
    info: 'bg-indigo-400',
  };
}
