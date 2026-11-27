import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { RxStomp, RxStompConfig, RxStompState } from '@stomp/rx-stomp';
import { IMessage, ReconnectionTimeMode } from '@stomp/stompjs';
import { BehaviorSubject, Subject } from 'rxjs';
import { EventoTempoReal } from '../../shared/models/tempo-real.model';
import {
  ATRASO_MAXIMO_RECONEXAO_MS,
  FABRICA_RX_STOMP,
  RealtimeChamadosService,
  TOPICO_CHAMADOS,
  resolverUrlWebSocket,
} from './realtime-chamados.service';

class RxStompFalso {
  config: RxStompConfig = {};
  readonly connectionState$ = new BehaviorSubject<RxStompState>(RxStompState.CLOSED);
  readonly mensagens = new Subject<IMessage>();
  readonly destinos: string[] = [];
  activate = vi.fn(() => this.connectionState$.next(RxStompState.CONNECTING));
  deactivate = vi.fn(async () => this.connectionState$.next(RxStompState.CLOSED));

  configure(config: RxStompConfig): void {
    this.config = { ...this.config, ...config };
  }

  watch(destino: string) {
    this.destinos.push(destino);
    return this.mensagens.asObservable();
  }
}

describe('RealtimeChamadosService', () => {
  let servico: RealtimeChamadosService;
  let httpMock: HttpTestingController;
  let clientes: RxStompFalso[];

  const evento: EventoTempoReal = {
    tipo: 'CHAMADO_CRIADO',
    chamado: {
      id: 'c-1',
      codigo: 'LUM-2026-000301',
      titulo: 'Impressora parada',
      unidade: { id: 'u-1', nome: 'U.03 Boa Viagem' },
      categoria: null,
      canal_origem: 'LIGACAO',
      status: 'ABERTO',
      prioridade: 'ALTA',
      responsavel: null,
      aberto_em: '2026-11-26T13:00:00Z',
      atualizado_em: '2026-11-26T13:00:00Z',
    },
    autor: { id: 'a-1', nome: 'Analista Dev Dois' },
    ocorrido_em: '2026-11-26T13:00:00Z',
  };

  beforeEach(() => {
    clientes = [];
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: FABRICA_RX_STOMP,
          useValue: () => {
            const cliente = new RxStompFalso();
            clientes.push(cliente);
            return cliente as unknown as RxStomp;
          },
        },
      ],
    });
    servico = TestBed.inject(RealtimeChamadosService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('deve conectar uma vez só, no tópico de chamados, com reconexão exponencial', () => {
    servico.conectar();
    servico.conectar();

    expect(clientes).toHaveLength(1);
    const cliente = clientes[0];
    expect(cliente.activate).toHaveBeenCalledTimes(1);
    expect(cliente.destinos).toEqual([TOPICO_CHAMADOS]);
    expect(cliente.config.brokerURL).toMatch(/^wss?:\/\/.+\/api\/v1\/ws$/);
    expect(cliente.config.reconnectTimeMode).toBe(ReconnectionTimeMode.EXPONENTIAL);
    expect(cliente.config.maxReconnectDelay).toBe(ATRASO_MAXIMO_RECONEXAO_MS);
  });

  it('deve pedir um ticket novo antes de cada conexão e mandar no CONNECT', async () => {
    servico.conectar();
    const cliente = clientes[0];

    const preparando = cliente.config.beforeConnect!(cliente as unknown as RxStomp);
    httpMock
      .expectOne('/api/v1/tempo-real/ticket')
      .flush({ status_code: 200, message: 'ok', data: { ticket: 'ticket-1', expira_em: '' }, errors: null });
    await preparando;

    expect(cliente.config.connectHeaders).toEqual({ Authorization: 'Bearer ticket-1' });
  });

  it('deve parar de tentar quando a sessão expirou (401 no ticket)', async () => {
    servico.conectar();
    const cliente = clientes[0];

    const preparando = cliente.config.beforeConnect!(cliente as unknown as RxStomp);
    httpMock
      .expectOne('/api/v1/tempo-real/ticket')
      .flush({ status_code: 401, message: 'Não autenticado', data: null, errors: null }, { status: 401, statusText: 'Unauthorized' });
    await preparando;

    expect(cliente.deactivate).toHaveBeenCalled();
    expect(servico.estado()).toBe('desconectado');
  });

  it('deve repassar as mensagens do tópico já convertidas', () => {
    const recebidos: EventoTempoReal[] = [];
    servico.eventos$.subscribe((e) => recebidos.push(e));
    servico.conectar();

    clientes[0].mensagens.next({ body: JSON.stringify(evento) } as IMessage);

    expect(recebidos).toEqual([evento]);
  });

  it('deve refletir o estado da conexão, inclusive durante a reconexão', () => {
    servico.conectar();
    const cliente = clientes[0];

    cliente.connectionState$.next(RxStompState.OPEN);
    expect(servico.estado()).toBe('conectado');

    cliente.connectionState$.next(RxStompState.CLOSED);
    expect(servico.estado()).toBe('conectando');
  });

  it('deve desconectar, parar de repassar mensagens e permitir conectar de novo', async () => {
    const recebidos: EventoTempoReal[] = [];
    servico.eventos$.subscribe((e) => recebidos.push(e));
    servico.conectar();
    const primeiro = clientes[0];

    await servico.desconectar();
    primeiro.mensagens.next({ body: JSON.stringify(evento) } as IMessage);

    expect(primeiro.deactivate).toHaveBeenCalled();
    expect(servico.estado()).toBe('desconectado');
    expect(recebidos).toEqual([]);

    servico.conectar();
    expect(clientes).toHaveLength(2);
  });

  it('deve montar a URL do WebSocket a partir do endereço da página', () => {
    expect(resolverUrlWebSocket('/api/v1/ws', { protocol: 'http:', host: 'localhost:4200' })).toBe(
      'ws://localhost:4200/api/v1/ws',
    );
    expect(resolverUrlWebSocket('/api/v1/ws', { protocol: 'https:', host: 'nexo.app' })).toBe(
      'wss://nexo.app/api/v1/ws',
    );
    expect(resolverUrlWebSocket('wss://api.nexo.app/api/v1/ws', { protocol: 'https:', host: 'nexo.app' })).toBe(
      'wss://api.nexo.app/api/v1/ws',
    );
  });
});
