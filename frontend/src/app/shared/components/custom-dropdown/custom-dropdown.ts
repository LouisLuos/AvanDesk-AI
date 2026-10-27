import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  computed,
  inject,
  input,
  model,
  signal,
} from '@angular/core';

export interface OpcaoDropdown {
  valor: string;
  rotulo: string;
}

@Component({
  selector: 'app-custom-dropdown',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="relative inline-block w-full lg:w-auto">
      <button
        type="button"
        [id]="id() + '-btn'"
        data-testid="dropdown-trigger"
        [attr.aria-haspopup]="'listbox'"
        [attr.aria-expanded]="aberto()"
        (click)="alternar($event)"
        class="group inline-flex h-9 w-full items-center justify-between gap-2 rounded-md border border-[#e2e5ec] bg-white px-3 text-xs text-[#363b47] shadow-2xs transition-all hover:border-[#b0b8c6] hover:bg-[#f8f9fc] focus:border-[#2d5bff] focus:outline-none focus:ring-2 focus:ring-[#2d5bff]/20 lg:w-auto"
        [class.border-[#2d5bff]]="aberto()"
        [class.ring-2]="aberto()"
        [class.ring-[#2d5bff]/20]="aberto()"
      >
        <span class="flex items-center gap-1.5 truncate">
          <span class="text-[#6b7280] font-normal">{{ rotulo() }}:</span>
          <span class="font-semibold text-[#14161b]">{{ rotuloSelecionado() }}</span>
        </span>
        <svg
          class="h-3.5 w-3.5 shrink-0 text-[#9aa2b1] transition-transform duration-200 group-hover:text-[#6b7280]"
          [class.rotate-180]="aberto()"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="m6 9 6 6 6-6" />
        </svg>
      </button>

      @if (aberto()) {
        <div
          role="listbox"
          [attr.aria-labelledby]="id() + '-btn'"
          class="absolute left-0 top-full z-40 mt-1 max-h-64 min-w-[200px] w-full overflow-y-auto rounded-lg border border-[#e2e5ec] bg-white p-1 shadow-lg ring-1 ring-black/5 animate-in fade-in-50 zoom-in-95 duration-100 lg:w-auto"
        >
          @for (opcao of listaOpcoes(); track opcao.valor) {
            <button
              type="button"
              role="option"
              [attr.aria-selected]="opcao.valor === valor()"
              (click)="selecionar(opcao.valor)"
              class="flex w-full items-center justify-between gap-3 rounded-md px-2.5 py-2 text-left text-xs transition-colors"
              [class]="
                opcao.valor === valor()
                  ? 'bg-[#e8edff] font-semibold text-[#1e3fcc]'
                  : 'text-[#363b47] hover:bg-[#f4f6fa] hover:text-[#14161b]'
              "
            >
              <span class="truncate">{{ opcao.rotulo }}</span>
              @if (opcao.valor === valor()) {
                <svg
                  class="h-3.5 w-3.5 shrink-0 text-[#2d5bff]"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  aria-hidden="true"
                >
                  <path d="M20 6 9 17l-5-5" />
                </svg>
              }
            </button>
          }
        </div>
      }
    </div>
  `,
})
export class CustomDropdown {
  private readonly elementRef = inject(ElementRef);

  readonly id = input<string>('custom-dropdown');
  readonly rotulo = input.required<string>();
  readonly opcaoPadrao = input<OpcaoDropdown | null>(null);
  readonly opcoes = input<ReadonlyArray<OpcaoDropdown>>([]);
  readonly valor = model<string>('');

  protected readonly aberto = signal(false);

  protected readonly listaOpcoes = computed<OpcaoDropdown[]>(() => {
    const padrao = this.opcaoPadrao();
    const lista = [...this.opcoes()];
    return padrao ? [padrao, ...lista] : lista;
  });

  protected readonly rotuloSelecionado = computed<string>(() => {
    const val = this.valor();
    const encontrada = this.listaOpcoes().find((o) => o.valor === val);
    if (encontrada) return encontrada.rotulo;
    return this.opcaoPadrao()?.rotulo ?? 'Todos';
  });

  protected alternar(event?: MouseEvent): void {
    event?.stopPropagation();
    this.aberto.update((v) => !v);
  }

  protected selecionar(novoValor: string): void {
    this.valor.set(novoValor);
    this.aberto.set(false);
  }

  @HostListener('document:click', ['$event'])
  onClickOutside(event: MouseEvent): void {
    if (this.aberto() && !this.elementRef.nativeElement.contains(event.target)) {
      this.aberto.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.aberto.set(false);
  }
}
