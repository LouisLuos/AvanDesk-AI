import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

interface AbaAdmin {
  rotulo: string;
  id: string;
  /** Sem rota, a aba aparece desabilitada até a funcionalidade existir. */
  rota?: string;
}

/**
 * Abas da área de administração. Renderizada no outlet `subnav` do MainLayout
 * para ocupar a largura toda, logo abaixo do cabeçalho.
 */
@Component({
  selector: 'app-admin-nav',
  imports: [RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <!-- A linha inferior é uma sombra interna: com border + -mb-px o overflow-x-auto ganharia rolagem vertical. -->
    <nav class="bg-white shadow-[inset_0_-1px_0_#e2e5ec]" aria-label="Administração">
      <div class="mx-auto flex max-w-7xl gap-1 overflow-x-auto px-4 sm:px-6 lg:px-8">
        @for (aba of abas; track aba.id) {
          @if (aba.rota) {
            <a
              [id]="aba.id"
              [routerLink]="aba.rota"
              routerLinkActive
              ariaCurrentWhenActive="page"
              class="border-b-2 border-transparent px-4 py-3.5 text-sm font-medium whitespace-nowrap text-[#6b7280] transition hover:text-[#14161b] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#2d5bff] aria-[current=page]:border-[#2d5bff] aria-[current=page]:font-semibold aria-[current=page]:text-[#2d5bff]"
            >
              {{ aba.rotulo }}
            </a>
          } @else {
            <span
              [id]="aba.id"
              aria-disabled="true"
              title="Disponível em breve"
              class="inline-flex cursor-not-allowed items-center gap-2 border-b-2 border-transparent px-4 py-3.5 text-sm font-medium whitespace-nowrap text-[#9aa2b1]"
            >
              {{ aba.rotulo }}
              <span class="rounded bg-[#f1f3f8] px-1.5 py-0.5 text-[10px] font-semibold text-[#9aa2b1]">Em breve</span>
            </span>
          }
        }
      </div>
    </nav>
  `,
})
export class AdminNav {
  protected readonly abas: ReadonlyArray<AbaAdmin> = [
    { rotulo: 'Painel da Administração', id: 'aba-painel' },
    { rotulo: 'Unidades', id: 'aba-unidades', rota: '/admin/unidades' },
    { rotulo: 'Analistas', id: 'aba-analistas', rota: '/admin/analistas' },
    { rotulo: 'Fornecedores Externos', id: 'aba-fornecedores' },
    { rotulo: 'Categorias e Sistemas', id: 'aba-categorias' },
  ];
}
