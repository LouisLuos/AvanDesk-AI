import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ContaSolicitanteService } from '../../../core/services/conta-solicitante.service';
import { ContaLayout } from '../../../layouts/conta-layout/conta-layout';
import { ReenvioVerificacao } from '../reenvio-verificacao/reenvio-verificacao';

@Component({
  selector: 'app-verifique-email',
  imports: [RouterLink, ContaLayout, ReenvioVerificacao],
  template: `
    <app-conta-layout>
      <div class="mb-6 flex h-12 w-12 items-center justify-center rounded-full bg-[#eef2ff] text-[#2d5bff]" aria-hidden="true">
        <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="2" y="4" width="20" height="16" rx="2" />
          <path d="m22 7-10 6L2 7" />
        </svg>
      </div>
      <h2 class="text-2xl font-bold tracking-tight text-[#14161b]">Verifique o seu e-mail</h2>
      @if (email) {
        <p class="mt-2 text-sm text-[#363b47]">
          Enviamos um link de confirmação para <strong id="email-pendente">{{ email }}</strong>.
        </p>
      } @else {
        <p class="mt-2 text-sm text-[#363b47]">Informe o e-mail da sua conta para receber um novo link de confirmação.</p>
      }
      <ul class="mt-4 list-inside list-disc space-y-1 text-xs text-[#6b7280]">
        <li>O link vale por 24 horas e só pode ser usado uma vez.</li>
        <li>Não encontrou? Confira a caixa de spam ou lixo eletrônico.</li>
        <li>Você só consegue entrar depois de confirmar o e-mail.</li>
      </ul>

      <div class="mt-6">
        <app-reenvio-verificacao [emailConhecido]="email" [comecarEsperando]="!!email" />
      </div>

      <p class="mt-8 text-center text-xs text-[#6b7280]">
        Já confirmou? <a routerLink="/login" class="font-semibold text-[#2d5bff] hover:underline">Entrar</a>
      </p>
    </app-conta-layout>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VerifiqueEmail {
  protected readonly email = inject(ContaSolicitanteService).emailPendente();
}
