import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { ContaLayout } from '../../../layouts/conta-layout/conta-layout';

@Component({
  selector: 'app-inicio-solicitante',
  imports: [ContaLayout],
  template: `
    <app-conta-layout>
      <h2 class="text-2xl font-bold tracking-tight text-[#14161b]">Olá, {{ usuario()?.nome }}</h2>
      <p class="mt-2 text-sm text-[#363b47]">
        A sua conta (<strong>{{ usuario()?.email }}</strong>) está ativa e ligada aos chamados abertos com este e-mail.
      </p>
      <p class="mt-4 rounded-lg bg-[#eef2ff] p-3 text-xs text-[#363b47]">
        Em breve você vai abrir e acompanhar os seus chamados por aqui. Enquanto isso, continue falando com o suporte e
        acompanhe as atualizações que chegam no seu e-mail.
      </p>
      <button
        type="button"
        id="btn-sair-solicitante"
        (click)="sair()"
        class="mt-6 inline-flex h-10 w-full items-center justify-center rounded-md border border-[#e2e5ec] bg-white px-4 text-xs font-semibold text-[#363b47] transition hover:bg-[#f8f9fc]"
      >
        Sair
      </button>
    </app-conta-layout>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InicioSolicitante {
  private readonly authService = inject(AuthService);

  protected readonly usuario = this.authService.usuario;

  protected sair(): void {
    this.authService.logout();
  }
}
