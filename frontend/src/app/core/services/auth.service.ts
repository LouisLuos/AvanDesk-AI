import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { EMPTY, Observable, catchError, finalize, map, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import { TokenStorageService } from './token-storage.service';

export interface UsuarioAutenticado {
  id: string;
  nome: string;
  email: string;
  perfil: string;
}

export interface LoginRequest {
  usuario: string;
  senha: string;
  manterConectado?: boolean;
}

export interface LoginResponse {
  access_token?: string;
  usuario: UsuarioAutenticado;
}

const STORAGE_KEY_USER = 'avandesk.user';
const LEGACY_KEY_USER = 'avandesk_user';
const LEGACY_KEY_TOKEN = 'avandesk_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  private readonly baseUrl = environment.apiUrl;

  private readonly usuarioSignal = signal<UsuarioAutenticado | null>(null);

  readonly usuario = computed(() => this.usuarioSignal());
  readonly estaAutenticado = computed(() => this.usuarioSignal() !== null);

  /** Define o perfil autenticado apenas em memória (usado na hidratação e em testes). */
  definirUsuario(usuario: UsuarioAutenticado | null): void {
    this.usuarioSignal.set(usuario);
  }

  emailDeLogin(usuario: string): string {
    return usuario.includes('@') ? usuario.toLowerCase() : `${usuario}@lumen.local`;
  }

  /**
   * Realiza login na plataforma consumindo POST /api/v1/auth/login (HU09 / HU26).
   * O JWT é gerenciado pelo servidor via cookie HttpOnly; apenas o perfil fica em memória.
   */
  login(dados: LoginRequest): Observable<LoginResponse> {
    const payload = {
      email: this.emailDeLogin(dados.usuario),
      senha: dados.senha,
      manter_conectado: Boolean(dados.manterConectado),
    };

    return this.http
      .post<ApiResponse<LoginResponse>>(`${this.baseUrl}/auth/login`, payload, {
        withCredentials: true,
      })
      .pipe(
        map((res) => res.data as LoginResponse),
        tap((resposta) => {
          this.limparStorageLegado();
          this.usuarioSignal.set(resposta.usuario);
        }),
      );
  }

  /**
   * Consulta GET /api/v1/auth/me para hidratar o perfil autenticado em memória via cookie HttpOnly.
   */
  carregarSessao(): Observable<UsuarioAutenticado | null> {
    return this.http
      .get<ApiResponse<UsuarioAutenticado>>(`${this.baseUrl}/auth/me`, {
        withCredentials: true,
      })
      .pipe(
        map((res) => res.data ?? null),
        tap((usuario) => this.usuarioSignal.set(usuario)),
        catchError(() => {
          this.usuarioSignal.set(null);
          return of(null);
        }),
      );
  }

  /**
   * Encerra a sessão ativa invalidando o cookie HttpOnly no backend e limpando o estado em memória.
   */
  logout(): void {
    this.limparSessaoLocal();
    this.http
      .post<ApiResponse<void>>(`${this.baseUrl}/auth/logout`, {}, { withCredentials: true })
      .pipe(
        catchError(() => EMPTY),
        finalize(() => this.router.navigate(['/login'])),
      )
      .subscribe();
  }

  /** Limpa o estado em memória e quaisquer resíduos legados no storage sem chamar o servidor. */
  limparSessaoLocal(): void {
    this.limparStorageLegado();
    this.usuarioSignal.set(null);
  }

  private limparStorageLegado(): void {
    this.tokenStorage.limpar();
    if (this.isBrowser) {
      globalThis.localStorage?.removeItem(STORAGE_KEY_USER);
      globalThis.sessionStorage?.removeItem(STORAGE_KEY_USER);
      globalThis.localStorage?.removeItem(LEGACY_KEY_USER);
      globalThis.sessionStorage?.removeItem(LEGACY_KEY_USER);
      globalThis.localStorage?.removeItem(LEGACY_KEY_TOKEN);
      globalThis.sessionStorage?.removeItem(LEGACY_KEY_TOKEN);
    }
  }
}
