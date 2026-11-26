import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { MODO_SIMULADO } from '../../core/config/modo-simulado';
import { AuthService, UsuarioAutenticado } from '../../core/services/auth.service';
import { RealtimeChamadosService } from '../../core/services/realtime-chamados.service';
import { ToastService } from '../../core/services/toast.service';
import { TokenStorageService } from '../../core/services/token-storage.service';
import { Subject } from 'rxjs';
import { EventoTempoReal } from '../../shared/models/tempo-real.model';
import { MAXIMO_ALERTAS_P1, MainLayout } from './main-layout';

describe('MainLayout', () => {
  let realtime: { conectar: ReturnType<typeof vi.fn>; desconectar: ReturnType<typeof vi.fn> };
  let eventos: Subject<EventoTempoReal>;

  function criarStorageEmMemoria(): Storage {
    const dados = new Map<string, string>();
    return {
      get length() {
        return dados.size;
      },
      clear: () => dados.clear(),
      getItem: (chave: string) => dados.get(chave) ?? null,
      key: (indice: number) => Array.from(dados.keys())[indice] ?? null,
      removeItem: (chave: string) => {
        dados.delete(chave);
      },
      setItem: (chave: string, valor: string) => {
        dados.set(chave, valor);
      },
    };
  }

  beforeEach(() => {
    eventos = new Subject<EventoTempoReal>();
    realtime = {
      conectar: vi.fn(),
      desconectar: vi.fn().mockResolvedValue(undefined),
      estado: signal('conectado'),
      eventos$: eventos.asObservable(),
    } as never;
    if (!globalThis.localStorage) {
      vi.stubGlobal('localStorage', criarStorageEmMemoria());
    }
  });

  function renderizar(modoSimulado: boolean, usuario: UsuarioAutenticado | null = null): HTMLElement {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [MainLayout],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MODO_SIMULADO, useValue: modoSimulado },
        { provide: RealtimeChamadosService, useValue: realtime },
      ],
    });
    TestBed.inject(AuthService).definirUsuario(usuario);
    const fixture = TestBed.createComponent(MainLayout);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  afterEach(() => {
    globalThis.sessionStorage?.clear();
    globalThis.localStorage?.clear();
    vi.unstubAllGlobals();
  });

  it('deve mostrar o selo de modo simulado quando o mock está ligado', () => {
    const selo = renderizar(true).querySelector('#badge-modo-simulado');

    expect(selo).not.toBeNull();
    expect(selo?.getAttribute('role')).toBe('status');
    expect(selo?.getAttribute('aria-label')).toContain('Modo simulado');
    expect(selo?.textContent).toContain('Simulado');
  });

  it('não deve mostrar o selo com a API real (integração e produção)', () => {
    expect(renderizar(false).querySelector('#badge-modo-simulado')).toBeNull();
  });

  it('deve mostrar a Administração só para a Liderança de TI', () => {
    const elemento = renderizar(false, {
      id: 'u-3',
      nome: 'Lideranca Dev',
      email: 'lideranca@lumen.local',
      perfil: 'LIDERANCA_TI',
    });

    expect(elemento.querySelector('#nav-administracao')).not.toBeNull();
  });

  it('deve esconder a Administração do analista de suporte', () => {
    const elemento = renderizar(false, {
      id: 'u-1',
      nome: 'Analista Dev Um',
      email: 'analista1@lumen.local',
      perfil: 'ANALISTA_SUPORTE',
    });

    expect(elemento.querySelector('#nav-fila-chamados')).not.toBeNull();
    expect(elemento.querySelector('#nav-administracao')).toBeNull();
  });

  it('não deve exibir dados de usuário nem botão de logout quando o usuário não estiver autenticado', () => {
    const elemento = renderizar(false);
    expect(elemento.querySelector('#btn-logout')).toBeNull();
  });

  it('deve encerrar a sessão no servidor e em memória e navegar para /login ao clicar em sair', () => {
    const elemento = renderizar(false, {
      id: 'u-1',
      nome: 'Analista Dev Um',
      email: 'analista1@lumen.local',
      perfil: 'ANALISTA_SUPORTE',
    });
    const tokenStorage = TestBed.inject(TokenStorageService);
    const authService = TestBed.inject(AuthService);
    const httpMock = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    const toast = TestBed.inject(ToastService);
    const navegarSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    const botaoLogout = elemento.querySelector('#btn-logout') as HTMLButtonElement;
    expect(botaoLogout).not.toBeNull();
    botaoLogout.click();

    const req = httpMock.expectOne('/api/v1/auth/logout');
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBe(true);
    req.flush({ status_code: 200, message: 'Logout realizado com sucesso', data: null, errors: null });
    httpMock.verify();

    expect(tokenStorage.obter()).toBeNull();
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
    expect(authService.usuario()).toBeNull();
    expect(authService.estaAutenticado()).toBe(false);
    expect(navegarSpy).toHaveBeenCalledWith(['/login']);
    expect(toast.toasts().map((t) => t.mensagem)).toContain('Sessão encerrada com sucesso.');
  });

  it('deve conectar o tempo real com usuário logado e desconectar no logout', () => {
    renderizar(false, {
      id: 'u-1',
      nome: 'Analista Dev Um',
      email: 'analista1@lumen.local',
      perfil: 'ANALISTA_SUPORTE',
    });
    expect(realtime.conectar).toHaveBeenCalledTimes(1);

    TestBed.inject(AuthService).limparSessaoLocal();
    TestBed.tick();

    expect(realtime.desconectar).toHaveBeenCalled();
  });

  it('não deve conectar o tempo real sem login nem no modo simulado', () => {
    renderizar(false);
    renderizar(true, {
      id: 'u-1',
      nome: 'Analista Dev Um',
      email: 'analista1@lumen.local',
      perfil: 'ANALISTA_SUPORTE',
    });

    expect(realtime.conectar).not.toHaveBeenCalled();
  });

  describe('alertas de P1 (HU25)', () => {
    const analista: UsuarioAutenticado = {
      id: 'eu',
      nome: 'Analista Dev Um',
      email: 'analista1@lumen.local',
      perfil: 'ANALISTA_SUPORTE',
    };

    function novoChamado(
      numero: number,
      prioridade: 'ALTA' | 'MEDIA' = 'ALTA',
      autorId = 'outro',
      tipo: EventoTempoReal['tipo'] = 'CHAMADO_CRIADO',
    ): EventoTempoReal {
      return {
        tipo,
        chamado: {
          id: `c-${numero}`,
          codigo: `LUM-2026-${String(numero).padStart(6, '0')}`,
          titulo: 'Sistema de agendamento fora do ar',
          unidade: { id: 'u-1', nome: 'U.03 Boa Viagem' },
          categoria: null,
          canal_origem: 'LIGACAO',
          status: 'ABERTO',
          prioridade,
          responsavel: null,
          aberto_em: '2026-11-26T13:00:00Z',
          atualizado_em: '2026-11-26T13:00:00Z',
        },
        autor: { id: autorId, nome: 'Analista Dev Dois' },
        ocorrido_em: '2026-11-26T13:00:00Z',
      };
    }

    function montar(): { elemento: HTMLElement; atualizar: () => void } {
      return { elemento: renderizar(false, analista), atualizar: () => TestBed.tick() };
    }

    it('mostra o banner quando chega um chamado novo de prioridade alta aberto por outra pessoa', () => {
      const { elemento, atualizar } = montar();

      eventos.next(novoChamado(301));
      atualizar();

      const banner = elemento.querySelector('#alertas-p1');
      expect(banner?.getAttribute('role')).toBe('alert');
      expect(banner?.textContent).toContain('LUM-2026-000301');
      expect(banner?.textContent).toContain('Sistema de agendamento fora do ar');
      expect(banner?.textContent).toContain('U.03 Boa Viagem');
    });

    it('não alerta para prioridade média, para o próprio chamado nem para outras mudanças', () => {
      const { elemento, atualizar } = montar();

      eventos.next(novoChamado(302, 'MEDIA'));
      eventos.next(novoChamado(303, 'ALTA', 'eu'));
      eventos.next(novoChamado(304, 'ALTA', 'outro', 'STATUS_ALTERADO'));
      atualizar();

      expect(elemento.querySelector('#alertas-p1')).toBeNull();
    });

    it('empilha no máximo três alertas, do mais novo para o mais antigo, e deixa dispensar', () => {
      const { elemento, atualizar } = montar();

      [305, 306, 307, 308].forEach((n) => eventos.next(novoChamado(n)));
      atualizar();
      const codigos = () =>
        Array.from(elemento.querySelectorAll('#alertas-p1 [data-chamado]')).map((a) => a.getAttribute('data-chamado'));
      expect(codigos()).toHaveLength(MAXIMO_ALERTAS_P1);
      expect(codigos()).toEqual(['c-308', 'c-307', 'c-306']);

      (elemento.querySelector('[data-chamado="c-308"] button[aria-label="Dispensar alerta"]') as HTMLButtonElement).click();
      atualizar();
      expect(codigos()).toEqual(['c-307', 'c-306']);
    });

    it('começa com o som desligado e guarda a escolha no navegador', () => {
      const { elemento, atualizar } = montar();
      const botao = () => elemento.querySelector('#btn-som-alerta-p1') as HTMLButtonElement;

      expect(botao().getAttribute('aria-pressed')).toBe('false');
      botao().click();
      atualizar();

      expect(botao().getAttribute('aria-pressed')).toBe('true');
      expect(localStorage.getItem('nexo.som-alerta-p1')).toBe('true');
    });

    it('mostra o estado da conexão em tempo real no cabeçalho', () => {
      const { elemento } = montar();

      const indicador = elemento.querySelector('#indicador-tempo-real');
      expect(indicador?.getAttribute('data-estado')).toBe('conectado');
      expect(indicador?.textContent).toContain('Tempo real');
    });
  });
});
