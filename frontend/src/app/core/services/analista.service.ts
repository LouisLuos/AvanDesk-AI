import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import { Analista, AnalistaCadastrado, AnalistaRequest } from '../../shared/models/analista.model';

@Injectable({ providedIn: 'root' })
export class AnalistaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/analistas`;

  /** Lista analistas. Quando `ativo` é informado, filtra pela situação (HU10). */
  listar(ativo?: boolean): Observable<Analista[]> {
    let params = new HttpParams();
    if (ativo !== undefined) {
      params = params.set('ativo', ativo);
    }
    return this.http
      .get<ApiResponse<Analista[]>>(this.baseUrl, { params })
      .pipe(map((res) => res.data ?? []));
  }

  cadastrar(payload: AnalistaRequest): Observable<AnalistaCadastrado> {
    return this.http
      .post<ApiResponse<AnalistaCadastrado>>(this.baseUrl, payload)
      .pipe(map((res) => res.data as AnalistaCadastrado));
  }

  editar(id: string, payload: AnalistaRequest): Observable<Analista> {
    return this.http
      .put<ApiResponse<Analista>>(`${this.baseUrl}/${id}`, payload)
      .pipe(map((res) => res.data as Analista));
  }

  desativar(id: string): Observable<Analista> {
    return this.http
      .patch<ApiResponse<Analista>>(`${this.baseUrl}/${id}/desativar`, {})
      .pipe(map((res) => res.data as Analista));
  }

  reativar(id: string): Observable<Analista> {
    return this.http
      .patch<ApiResponse<Analista>>(`${this.baseUrl}/${id}/reativar`, {})
      .pipe(map((res) => res.data as Analista));
  }
}
