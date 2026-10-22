import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import { USUARIO_ATUAL } from '../../shared/utils/usuario-atual';

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

const STORAGE_KEY_TOKEN = 'avandesk_token';
const STORAGE_KEY_USER = 'avandesk_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly baseUrl = environment.apiUrl;

  private readonly usuarioSignal = signal<UsuarioAutenticado | null>(this.carregarUsuarioInicial());

  readonly usuario = computed(() => this.usuarioSignal());
  readonly estaAutenticado = computed(() => this.usuarioSignal() !== null);

  private carregarUsuarioInicial(): UsuarioAutenticado | null {
    try {
      const salvo = localStorage.getItem(STORAGE_KEY_USER) || sessionStorage.getItem(STORAGE_KEY_USER);
      if (salvo) return JSON.parse(salvo);
    } catch {
      // Fallback
    }
    return {
      id: USUARIO_ATUAL.id,
      nome: USUARIO_ATUAL.nome,
      email: 'ana.souza@lumendiagnosticos.com.br',
      perfil: USUARIO_ATUAL.perfil,
    };
  }

  /**
   * Realiza login na plataforma.
   * Pronto para consumir POST /auth/login da HU09 quando integrado ao backend.
   */
  login(dados: LoginRequest): Observable<LoginResponse> {
    const payload = {
      email: dados.usuario.includes('@') ? dados.usuario : `${dados.usuario}@lumendiagnosticos.com.br`,
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

  logout(): void {
    localStorage.removeItem(STORAGE_KEY_TOKEN);
    localStorage.removeItem(STORAGE_KEY_USER);
    sessionStorage.removeItem(STORAGE_KEY_TOKEN);
    sessionStorage.removeItem(STORAGE_KEY_USER);
    this.usuarioSignal.set(null);
    this.router.navigate(['/login']);
  }

  private salvarSessao(resposta: LoginResponse, manterConectado: boolean): void {
    const storage = manterConectado ? localStorage : sessionStorage;
    storage.setItem(STORAGE_KEY_TOKEN, resposta.access_token);
    storage.setItem(STORAGE_KEY_USER, JSON.stringify(resposta.usuario));
    this.usuarioSignal.set(resposta.usuario);
  }
}
