import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import { ChamadoRequest, ChamadoResponse } from '../../shared/models/chamado.model';

@Injectable({ providedIn: 'root' })
export class ChamadoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/chamados`;

  /** POST /chamados — registra um chamado (HU01). */
  registrar(payload: ChamadoRequest): Observable<ChamadoResponse> {
    return this.http
      .post<ApiResponse<ChamadoResponse>>(this.baseUrl, payload)
      .pipe(map((res) => res.data as ChamadoResponse));
  }
}
