import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';

export interface UnidadeCadastro {
  id: string;
  nome: string;
}

export interface CadastroSolicitanteRequest {
  nome: string;
  email: string;
  unidade_id: string;
  senha: string;
}

export interface CadastroSolicitanteResponse {
  email: string;
  status_conta: 'PENDENTE_VERIFICACAO' | 'ATIVA';
  verificacao_expira_em: string;
}

export interface VerificacaoEmailResponse {
  email: string;
  chamados_vinculados: number;
}

export type MotivoTokenRecusado = 'TOKEN_INVALIDO' | 'TOKEN_UTILIZADO' | 'TOKEN_SUBSTITUIDO' | 'TOKEN_EXPIRADO';

const CHAVE_EMAIL_PENDENTE = 'nexo.email-pendente';

@Injectable({ providedIn: 'root' })
export class ContaSolicitanteService {
  private readonly http = inject(HttpClient);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  private readonly baseUrl = `${environment.apiUrl}/auth`;
  private emailEmMemoria: string | null = null;

  unidades(): Observable<UnidadeCadastro[]> {
    return this.http
      .get<ApiResponse<UnidadeCadastro[]>>(`${this.baseUrl}/unidades`)
      .pipe(map((res) => res.data ?? []));
  }

  registrar(dados: CadastroSolicitanteRequest): Observable<CadastroSolicitanteResponse> {
    return this.http
      .post<ApiResponse<CadastroSolicitanteResponse>>(`${this.baseUrl}/registrar`, dados)
      .pipe(map((res) => res.data as CadastroSolicitanteResponse));
  }

  verificarEmail(token: string): Observable<VerificacaoEmailResponse> {
    return this.http
      .post<ApiResponse<VerificacaoEmailResponse>>(`${this.baseUrl}/verificar-email`, { token })
      .pipe(map((res) => res.data as VerificacaoEmailResponse));
  }

  reenviarVerificacao(email: string): Observable<string> {
    return this.http
      .post<ApiResponse<void>>(`${this.baseUrl}/reenviar-verificacao`, { email })
      .pipe(map((res) => res.message));
  }

  guardarEmailPendente(email: string): void {
    this.emailEmMemoria = email;
    if (!this.isBrowser) return;
    try {
      globalThis.sessionStorage?.setItem(CHAVE_EMAIL_PENDENTE, email);
    } catch {
      return;
    }
  }

  emailPendente(): string | null {
    if (this.emailEmMemoria) return this.emailEmMemoria;
    if (!this.isBrowser) return null;
    try {
      return globalThis.sessionStorage?.getItem(CHAVE_EMAIL_PENDENTE) ?? null;
    } catch {
      return null;
    }
  }

  esquecerEmailPendente(): void {
    this.emailEmMemoria = null;
    if (!this.isBrowser) return;
    try {
      globalThis.sessionStorage?.removeItem(CHAVE_EMAIL_PENDENTE);
    } catch {
      return;
    }
  }
}
