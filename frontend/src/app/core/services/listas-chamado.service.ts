import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import { ItemLista } from '../../shared/models/chamado.model';

/** Opções dos campos do registro de chamado, mantidas pela Liderança de TI (só itens ativos). */
@Injectable({ providedIn: 'root' })
export class ListasChamadoService {
  private readonly http = inject(HttpClient);

  /** GET /categorias — ordenadas por nome. */
  listarCategorias(): Observable<ItemLista[]> {
    return this.listar('categorias');
  }

  /** GET /sistemas-equipamentos — ordenados por nome. */
  listarSistemasEquipamentos(): Observable<ItemLista[]> {
    return this.listar('sistemas-equipamentos');
  }

  private listar(recurso: string): Observable<ItemLista[]> {
    return this.http
      .get<ApiResponse<ItemLista[]>>(`${environment.apiUrl}/${recurso}`)
      .pipe(map((res) => res.data ?? []));
  }
}
