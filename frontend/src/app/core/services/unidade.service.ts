import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import { Unidade, UnidadeRequest } from '../../shared/models/unidade.model';

@Injectable({ providedIn: 'root' })
export class UnidadeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/unidades`;

  /** Lista unidades. Quando `ativo` é informado, filtra pela situação. */
  listar(ativo?: boolean): Observable<Unidade[]> {
    let params = new HttpParams();
    if (ativo !== undefined) {
      params = params.set('ativo', ativo);
    }
    return this.http
      .get<ApiResponse<Unidade[]>>(this.baseUrl, { params })
      .pipe(map((res) => res.data ?? []));
  }

  /** Atalho para a abertura de chamados: somente unidades ativas (HU11). */
  listarAtivas(): Observable<Unidade[]> {
    return this.listar(true);
  }

  cadastrar(payload: UnidadeRequest): Observable<Unidade> {
    return this.http
      .post<ApiResponse<Unidade>>(this.baseUrl, payload)
      .pipe(map((res) => res.data as Unidade));
  }

  editar(id: string, payload: UnidadeRequest): Observable<Unidade> {
    return this.http
      .put<ApiResponse<Unidade>>(`${this.baseUrl}/${id}`, payload)
      .pipe(map((res) => res.data as Unidade));
  }

  desativar(id: string): Observable<Unidade> {
    return this.http
      .patch<ApiResponse<Unidade>>(`${this.baseUrl}/${id}/desativar`, {})
      .pipe(map((res) => res.data as Unidade));
  }
}
