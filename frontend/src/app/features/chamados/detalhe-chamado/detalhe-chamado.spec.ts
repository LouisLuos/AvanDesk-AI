import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ChamadoService } from '../../../core/services/chamado.service';
import { ToastService } from '../../../core/services/toast.service';
import { ChamadoDetalhe } from '../../../shared/models/chamado.model';
import { DetalheChamado } from './detalhe-chamado';

describe('DetalheChamado Component', () => {
  let component: DetalheChamado;
  let fixture: ComponentFixture<DetalheChamado>;
  let chamadoService: ChamadoService;
  let toastService: ToastService;

  const mockDetalhe: ChamadoDetalhe = {
    id: 'chm-127',
    codigo: 'LUM-2026-000127',
    titulo: 'Impressora de etiquetas parada na coleta',
    descricao: 'Impressora de etiquetas da coleta parou de imprimir.',
    unidade: { id: 'u-1', nome: 'U.03 Boa Viagem' },
    solicitante: {
      nome: 'Mariana Alves',
      contato: '+5581988771234',
      telefoneMascarado: '+55 81 9****-1234',
    },
    canal_origem: 'LIGACAO',
    status: 'EM_ATENDIMENTO',
    prioridade: 'ALTA',
    categoria: 'Equipamento de apoio',
    sistema: 'Impressora de etiquetas',
    responsavel: { id: 'r-1', nome: 'Ana Souza' },
    aberto_em: '07/10/2026 · 10:02',
    primeira_resposta_em: '10:04',
    evidencias: [{ id: 'ev-1', nome: 'erro.png', enviadoAs: '10:03' }],
    historico: [
      {
        id: 'h-1',
        tipo: 'CHAMADO_ABERTO',
        titulo: 'Chamado aberto via Ligação',
        hora: '10:02',
        dataCompleta: '07/10/2026 10:02',
        estiloDot: 'bg-[#94a3b8]',
      },
    ],
    slaItens: [
      { etapa: 'Triagem', meta: '15 min', situacaoTexto: 'Cumprida em 3 min', progresso: 100, estiloBarra: 'bg-[#22c55e]' },
    ],
    pausasMinutos: 0,
    tempoTerceirosHoras: 0,
    relatosVinculados: [],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DetalheChamado],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => (key === 'id' ? 'chm-127' : null),
              },
            },
          },
        },
      ],
    }).compileComponents();

    chamadoService = TestBed.inject(ChamadoService);
    toastService = TestBed.inject(ToastService);
  });

  it('deve carregar e exibir os dados do chamado', () => {
    vi.spyOn(chamadoService, 'buscarPorId').mockReturnValue(of(mockDetalhe));

    fixture = TestBed.createComponent(DetalheChamado);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(chamadoService.buscarPorId).toHaveBeenCalledWith('chm-127');
    expect(component['chamado']()?.codigo).toBe('LUM-2026-000127');
    expect(component['carregando']()).toBe(false);

    const elemento = fixture.nativeElement as HTMLElement;
    expect(elemento.textContent).toContain('LUM-2026-000127');
    expect(elemento.textContent).toContain('U.03 Boa Viagem');
    expect(elemento.textContent).toContain('+55 81 9****-1234');
  });

  it('deve exibir mensagem de erro ao falhar na busca', () => {
    vi.spyOn(chamadoService, 'buscarPorId').mockReturnValue(
      throwError(() => new Error('Erro 404')),
    );

    fixture = TestBed.createComponent(DetalheChamado);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component['erro']()).toBe('Não foi possível carregar os detalhes do chamado.');
    const elemento = fixture.nativeElement as HTMLElement;
    expect(elemento.textContent).toContain('Não foi possível carregar os detalhes do chamado.');
  });

  it('deve disparar toast informativo ao clicar em ações de levas futuras', () => {
    vi.spyOn(chamadoService, 'buscarPorId').mockReturnValue(of(mockDetalhe));
    const toastSpy = vi.spyOn(toastService, 'info');

    fixture = TestBed.createComponent(DetalheChamado);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component['acaoNaoDisponivel']('Avaliar prioridade', 'Leva 2 (HU04)');
    expect(toastSpy).toHaveBeenCalledWith('Ação "Avaliar prioridade" planejada para a Leva 2 (HU04).');
  });
});
