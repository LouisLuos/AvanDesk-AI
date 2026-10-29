import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import {
  ChamadoDetalhe,
  ChamadoDetalheApiResponse,
  ChamadoRequest,
  ChamadoResponse,
  ChamadoResumo,
} from '../../shared/models/chamado.model';
import { mapearChamadoDetalhe } from '../../shared/utils/chamado-detalhe.mapper';

@Injectable({ providedIn: 'root' })
export class ChamadoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/chamados`;

  /** GET /chamados — fila de chamados de todos os canais. */
  listar(): Observable<ChamadoResumo[]> {
    return this.http
      .get<ApiResponse<ChamadoResumo[]>>(this.baseUrl)
      .pipe(map((res) => res.data ?? []));
  }

  /** POST /chamados — registra um chamado (HU01). */
  registrar(payload: ChamadoRequest): Observable<ChamadoResponse> {
    return this.http
      .post<ApiResponse<ChamadoResponse>>(this.baseUrl, payload)
      .pipe(map((res) => res.data as ChamadoResponse));
  }

  /**
   * GET /chamados/{id} — recupera detalhes completos do chamado com contatos,
   * status, responsável e linha do tempo cronológica convertida para o fuso local (HU02 e HU05).
   */
  buscarPorId(id: string): Observable<ChamadoDetalhe> {
    return this.http
      .get<ApiResponse<ChamadoDetalheApiResponse | ChamadoDetalhe>>(`${this.baseUrl}/${id}`)
      .pipe(map((res) => mapearChamadoDetalhe(res.data as ChamadoDetalheApiResponse | ChamadoDetalhe)));
  }
}
