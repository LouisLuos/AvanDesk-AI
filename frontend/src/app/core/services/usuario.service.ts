import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import { UsuarioAutenticado } from './auth.service';

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/usuarios`;

  /**
   * GET /usuarios/analistas — lista analistas de suporte ativos para atribuição (HU12).
   */
  listarAnalistas(): Observable<UsuarioAutenticado[]> {
    return this.http
      .get<ApiResponse<UsuarioAutenticado[]>>(`${this.baseUrl}/analistas`)
      .pipe(map((res) => res.data as UsuarioAutenticado[]));
  }
}
