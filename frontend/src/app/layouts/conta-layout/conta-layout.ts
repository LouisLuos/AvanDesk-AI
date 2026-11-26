import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-conta-layout',
  template: `
    <div class="grid min-h-screen w-full lg:grid-cols-2">
      <aside
        class="relative hidden flex-col justify-between overflow-hidden p-8 sm:p-12 lg:flex lg:p-16"
        style="background: radial-gradient(circle at 10% 40%, rgba(255, 106, 61, 0.95) 0%, rgba(255, 106, 61, 0.4) 30%, transparent 60%), radial-gradient(circle at 85% 15%, rgba(37, 99, 235, 0.95) 0%, rgba(30, 63, 204, 0.5) 35%, transparent 65%), linear-gradient(145deg, #18223c 0%, #0a0d14 100%);"
        aria-label="Apresentação da plataforma Nexo"
      >
        <div>
          <img src="/nexo.svg" alt="Nexo" class="h-8 w-auto brightness-0 invert" />
        </div>
        <div class="my-auto max-w-lg py-12">
          <h1 class="text-4xl font-extrabold leading-[1.15] tracking-tight text-white sm:text-5xl">
            Seus chamados,<br />sempre à mão.
          </h1>
          <p class="mt-5 text-sm leading-relaxed text-white/85 sm:text-base">
            Crie a sua conta para acompanhar os chamados de TI da sua unidade na Rede Lúmen.
          </p>
        </div>
        <div>
          <p class="text-xs font-medium text-white/60">Rede Lúmen Diagnósticos · Suporte técnico</p>
        </div>
      </aside>

      <main class="flex flex-col justify-center bg-white px-6 py-12 sm:px-12 lg:px-16 xl:px-24">
        <div class="mx-auto w-full max-w-sm">
          <div class="mb-8 lg:hidden">
            <img src="/nexo.svg" alt="Nexo" class="h-7 w-auto" />
          </div>
          <ng-content />
        </div>
      </main>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContaLayout {}
