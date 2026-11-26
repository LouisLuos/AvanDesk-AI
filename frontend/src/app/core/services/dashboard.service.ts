import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import { DashboardMetricas } from '../../shared/models/dashboard.model';

export interface FiltrosDashboard {
  inicio?: string;
  fim?: string;
  unidade_id?: string;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/dashboard`;

  /** GET /dashboard/metricas — indicadores executivos de SLA e alertas da Seção 9.7 (HU08). */
  obterMetricas(filtros: FiltrosDashboard = {}): Observable<DashboardMetricas> {
    let params = new HttpParams();
    if (filtros.inicio) params = params.set('inicio', filtros.inicio);
    if (filtros.fim) params = params.set('fim', filtros.fim);
    if (filtros.unidade_id) params = params.set('unidade_id', filtros.unidade_id);

    return this.http
      .get<ApiResponse<DashboardMetricas>>(`${this.baseUrl}/metricas`, { params })
      .pipe(map((res) => res.data as DashboardMetricas));
  }
}
