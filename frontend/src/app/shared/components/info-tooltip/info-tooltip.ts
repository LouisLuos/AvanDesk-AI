import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  inject,
  input,
  signal,
} from '@angular/core';

let proximoIdTooltip = 0;

@Component({
  selector: 'app-info-tooltip',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="relative inline-flex items-center">
      <button
        type="button"
        data-testid="info-tooltip-trigger"
        [attr.aria-label]="rotuloAcessivel()"
        [attr.aria-describedby]="visivel() ? tooltipId : null"
        [attr.aria-expanded]="visivel()"
        (mouseenter)="abrir()"
        (mouseleave)="fecharSeNaoFixado()"
        (focus)="abrir()"
        (blur)="fechar()"
        (click)="alternarFixo($event)"
        class="inline-flex h-4 w-4 shrink-0 items-center justify-center rounded-full text-[#9aa2b1] transition-colors hover:bg-[#eef1f7] hover:text-[#2d5bff] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#2d5bff]"
      >
        <svg
          class="h-3.5 w-3.5"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <circle cx="12" cy="12" r="10" />
          <path d="M12 16v-4" />
          <path d="M12 8h.01" />
        </svg>
      </button>

      @if (visivel()) {
        <div
          [id]="tooltipId"
          role="tooltip"
          data-testid="info-tooltip-content"
          [style.top.px]="posicao().top"
          [style.left.px]="posicao().left"
          [style.transform]="posicao().acima ? 'translateY(-100%)' : 'none'"
          class="pointer-events-none fixed z-50 w-64 max-w-[calc(100vw-24px)] rounded-lg border border-white/10 bg-[#14161b] px-3 py-2 text-left text-[11px] font-normal leading-relaxed text-[#f8fafc] shadow-lg"
        >
          @if (titulo()) {
            <p class="mb-0.5 font-semibold text-white">{{ titulo() }}</p>
          }
          <p class="text-[#e2e8f0]">{{ texto() }}</p>
        </div>
      }
    </span>
  `,
})
export class InfoTooltip {
  private static readonly LARGURA_ESTIMADA = 256;
  private static readonly MARGEM_BORDA = 12;
  private static readonly ESPACO_VERTICAL = 8;
  private static readonly ALTURA_MINIMA_ACIMA = 110;

  private readonly host = inject(ElementRef<HTMLElement>);

  readonly texto = input.required<string>();
  readonly titulo = input<string>('');
  readonly ariaLabel = input<string>('');

  protected readonly tooltipId = `nexo-tooltip-${++proximoIdTooltip}`;
  protected readonly visivel = signal(false);
  protected readonly fixado = signal(false);
  protected readonly posicao = signal<{ top: number; left: number; acima: boolean }>({
    top: 0,
    left: 0,
    acima: true,
  });

  protected rotuloAcessivel(): string {
    if (this.ariaLabel()) return this.ariaLabel();
    if (this.titulo()) return `Explicação sobre ${this.titulo()}`;
    return 'Mais informações sobre este indicador';
  }

  protected abrir(): void {
    this.atualizarPosicao();
    this.visivel.set(true);
  }

  protected fecharSeNaoFixado(): void {
    if (!this.fixado()) {
      this.visivel.set(false);
    }
  }

  protected fechar(): void {
    this.fixado.set(false);
    this.visivel.set(false);
  }

  protected alternarFixo(evento: MouseEvent): void {
    evento.stopPropagation();
    if (this.fixado()) {
      this.fechar();
    } else {
      this.fixado.set(true);
      this.abrir();
    }
  }

  @HostListener('document:click', ['$event'])
  onClickFora(evento: MouseEvent): void {
    if (this.visivel() && !this.host.nativeElement.contains(evento.target as Node)) {
      this.fechar();
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.visivel()) {
      this.fechar();
    }
  }

  @HostListener('window:scroll')
  @HostListener('window:resize')
  onViewportChange(): void {
    if (this.visivel()) {
      this.atualizarPosicao();
    }
  }

  private atualizarPosicao(): void {
    if (typeof window === 'undefined') return;
    const botao = this.host.nativeElement.querySelector('button');
    if (!botao) return;

    const rect = botao.getBoundingClientRect();
    const larguraViewport = window.innerWidth || 1024;
    const largura = Math.min(InfoTooltip.LARGURA_ESTIMADA, larguraViewport - InfoTooltip.MARGEM_BORDA * 2);

    const acima = rect.top >= InfoTooltip.ALTURA_MINIMA_ACIMA;
    const top = acima
      ? rect.top - InfoTooltip.ESPACO_VERTICAL
      : rect.bottom + InfoTooltip.ESPACO_VERTICAL;

    const centroX = rect.left + rect.width / 2 - largura / 2;
    const left = Math.max(
      InfoTooltip.MARGEM_BORDA,
      Math.min(centroX, larguraViewport - largura - InfoTooltip.MARGEM_BORDA),
    );

    this.posicao.set({ top, left, acima });
  }
}
