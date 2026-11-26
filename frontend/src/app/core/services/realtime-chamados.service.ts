import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { Injectable, InjectionToken, PLATFORM_ID, inject, signal } from '@angular/core';
import { ReconnectionTimeMode } from '@stomp/stompjs';
import { RxStomp, RxStompState } from '@stomp/rx-stomp';
import { Observable, Subject, Subscription, firstValueFrom, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import { EstadoTempoReal, EventoTempoReal, TicketTempoReal } from '../../shared/models/tempo-real.model';

export const TOPICO_CHAMADOS = '/topic/chamados';
export const ATRASO_INICIAL_RECONEXAO_MS = 1000;
export const ATRASO_MAXIMO_RECONEXAO_MS = 30000;

export const FABRICA_RX_STOMP = new InjectionToken<() => RxStomp>('FABRICA_RX_STOMP', {
  providedIn: 'root',
  factory: () => () => new RxStomp(),
});

export function resolverUrlWebSocket(url: string, local: Pick<Location, 'protocol' | 'host'>): string {
  if (/^wss?:\/\//.test(url)) return url;
  const protocolo = local.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocolo}//${local.host}${url}`;
}

@Injectable({ providedIn: 'root' })
export class RealtimeChamadosService {
  private readonly http = inject(HttpClient);
  private readonly fabrica = inject(FABRICA_RX_STOMP);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  private cliente: RxStomp | null = null;
  private assinaturas = new Subscription();
  private readonly eventosSubject = new Subject<EventoTempoReal>();

  readonly eventos$: Observable<EventoTempoReal> = this.eventosSubject.asObservable();
  readonly estado = signal<EstadoTempoReal>('desconectado');

  conectar(): void {
    if (!this.isBrowser || this.cliente) return;

    const cliente = this.fabrica();
    cliente.configure({
      brokerURL: resolverUrlWebSocket(environment.wsUrl, location),
      reconnectDelay: ATRASO_INICIAL_RECONEXAO_MS,
      maxReconnectDelay: ATRASO_MAXIMO_RECONEXAO_MS,
      reconnectTimeMode: ReconnectionTimeMode.EXPONENTIAL,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      beforeConnect: (stomp) => this.prepararConexao(stomp),
    });

    this.assinaturas = new Subscription();
    this.assinaturas.add(
      cliente.connectionState$.subscribe((estado) => this.estado.set(this.traduzirEstado(estado))),
    );
    this.assinaturas.add(
      cliente
        .watch(TOPICO_CHAMADOS)
        .pipe(map((mensagem) => JSON.parse(mensagem.body) as EventoTempoReal))
        .subscribe((evento) => this.eventosSubject.next(evento)),
    );
    this.cliente = cliente;
    cliente.activate();
  }

  async desconectar(): Promise<void> {
    const cliente = this.cliente;
    if (!cliente) return;
    this.cliente = null;
    this.assinaturas.unsubscribe();
    this.estado.set('desconectado');
    await cliente.deactivate();
  }

  private async prepararConexao(stomp: RxStomp): Promise<void> {
    try {
      const ticket = await firstValueFrom(
        this.http
          .get<ApiResponse<TicketTempoReal>>(`${environment.apiUrl}/tempo-real/ticket`)
          .pipe(map((res) => (res.data as TicketTempoReal).ticket)),
      );
      stomp.configure({ connectHeaders: { Authorization: `Bearer ${ticket}` } });
    } catch (erro) {
      if (erro instanceof HttpErrorResponse && erro.status === HttpStatusCode.Unauthorized) {
        await this.desconectar();
      }
    }
  }

  private traduzirEstado(estado: RxStompState): EstadoTempoReal {
    if (estado === RxStompState.OPEN) return 'conectado';
    if (estado === RxStompState.CONNECTING) return 'conectando';
    return this.cliente ? 'conectando' : 'desconectado';
  }
}
