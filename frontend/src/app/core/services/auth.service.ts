import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';
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
  access_token: string;
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

  private readonly usuarioSignal = signal<UsuarioAutenticado | null>(this.carregarUsuarioInicial());

  readonly usuario = computed(() => this.usuarioSignal());
  readonly estaAutenticado = computed(
    () => this.usuarioSignal() !== null && this.tokenStorage.obter() !== null,
  );

  private carregarUsuarioInicial(): UsuarioAutenticado | null {
    if (!this.isBrowser) {
      return null;
    }
    try {
      const salvo =
        sessionStorage.getItem(STORAGE_KEY_USER) ??
        localStorage.getItem(STORAGE_KEY_USER) ??
        sessionStorage.getItem(LEGACY_KEY_USER) ??
        localStorage.getItem(LEGACY_KEY_USER);
      if (salvo) {
        return JSON.parse(salvo) as UsuarioAutenticado;
      }
    } catch {
      // Ignora JSON corrompido no storage
    }
    return null;
  }

  /**
   * Realiza login na plataforma consumindo POST /api/v1/auth/login (HU09).
   */
  login(dados: LoginRequest): Observable<LoginResponse> {
    const email = dados.usuario.includes('@')
      ? dados.usuario
      : `${dados.usuario}@lumen.local`;

    const payload = {
      email,
      senha: dados.senha,
    };

    return this.http
      .post<ApiResponse<LoginResponse>>(`${this.baseUrl}/auth/login`, payload)
      .pipe(
        map((res) => res.data as LoginResponse),
        tap((resposta) => {
          this.salvarSessao(resposta, dados.manterConectado ?? false);
        }),
      );
  }

  /**
   * Encerra a sessão ativa limpando o token JWT e os dados do usuário no storage (HU09 / RF-021).
   */
  logout(): void {
    this.tokenStorage.limpar();
    if (this.isBrowser) {
      localStorage.removeItem(STORAGE_KEY_USER);
      sessionStorage.removeItem(STORAGE_KEY_USER);
      localStorage.removeItem(LEGACY_KEY_USER);
      sessionStorage.removeItem(LEGACY_KEY_USER);
      localStorage.removeItem(LEGACY_KEY_TOKEN);
      sessionStorage.removeItem(LEGACY_KEY_TOKEN);
    }
    this.usuarioSignal.set(null);
    this.router.navigate(['/login']);
  }

  private salvarSessao(resposta: LoginResponse, manterConectado: boolean): void {
    this.tokenStorage.salvar(resposta.access_token, manterConectado);
    if (this.isBrowser) {
      const storage = manterConectado ? localStorage : sessionStorage;
      const outroStorage = manterConectado ? sessionStorage : localStorage;
      outroStorage.removeItem(STORAGE_KEY_USER);
      storage.setItem(STORAGE_KEY_USER, JSON.stringify(resposta.usuario));
    }
    this.usuarioSignal.set(resposta.usuario);
  }
}
