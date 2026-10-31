import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import {
  ChamadoDetalhe,
  ChamadoDetalheApiResponse,
  ChamadoRequest,
  ChamadoResponse,
  FilaChamados,
  FiltrosFila,
  ResponsavelRequest,
  StatusRequest,
  TAMANHO_PAGINA_FILA,
} from '../../shared/models/chamado.model';
import { mapearChamadoDetalhe } from '../../shared/utils/chamado-detalhe.mapper';

@Injectable({ providedIn: 'root' })
export class ChamadoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/chamados`;

  /**
   * GET /chamados — fila paginada (HU14), do mais recente para o mais antigo.
   * Filtros vazios, falsos ou ausentes não são enviados.
   */
  listar(
    filtros: FiltrosFila = {},
    pagina = 0,
    tamanho = TAMANHO_PAGINA_FILA,
  ): Observable<FilaChamados> {
    let params = new HttpParams().set('page', pagina).set('size', tamanho);
    for (const status of filtros.status ?? []) {
      params = params.append('status', status);
    }
    const valores: Record<string, string | boolean | undefined> = {
      unidade_id: filtros.unidade_id,
      prioridade: filtros.prioridade,
      sem_prioridade: filtros.sem_prioridade,
      canal_origem: filtros.canal_origem,
      responsavel_id: filtros.responsavel_id,
      sem_responsavel: filtros.sem_responsavel,
      meus: filtros.meus,
      codigo: filtros.codigo?.trim(),
    };
    for (const [nome, valor] of Object.entries(valores)) {
      if (valor) params = params.set(nome, valor);
    }
    return this.http
      .get<ApiResponse<FilaChamados>>(this.baseUrl, { params })
      .pipe(map((res) => res.data as FilaChamados));
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

  /**
   * PATCH /chamados/{id}/responsavel — atribui responsável ao chamado (HU12).
   */
  atribuirResponsavel(id: string, responsavelId: string): Observable<ChamadoDetalhe> {
    const payload: ResponsavelRequest = {
      responsavel_id: responsavelId,
      responsavelId,
    };
    return this.http
      .patch<ApiResponse<ChamadoDetalheApiResponse | ChamadoDetalhe>>(
        `${this.baseUrl}/${id}/responsavel`,
        payload,
      )
      .pipe(map((res) => mapearChamadoDetalhe(res.data as ChamadoDetalheApiResponse | ChamadoDetalhe)));
  }

  /**
   * PATCH /chamados/{id}/status — atualiza o status do chamado conforme ciclo de vida (HU12).
   */
  alterarStatus(id: string, payload: StatusRequest): Observable<ChamadoDetalhe> {
    const corpoRequisicao = {
      status: payload.status,
      motivo_espera: payload.motivo_espera ?? payload.motivoEspera ?? null,
      motivoEspera: payload.motivo_espera ?? payload.motivoEspera ?? null,
      motivo_cancelamento: payload.motivo_cancelamento ?? payload.motivoCancelamento ?? null,
      motivoCancelamento: payload.motivo_cancelamento ?? payload.motivoCancelamento ?? null,
    };
    return this.http
      .patch<ApiResponse<ChamadoDetalheApiResponse | ChamadoDetalhe>>(
        `${this.baseUrl}/${id}/status`,
        corpoRequisicao,
      )
      .pipe(map((res) => mapearChamadoDetalhe(res.data as ChamadoDetalheApiResponse | ChamadoDetalhe)));
  }
}
