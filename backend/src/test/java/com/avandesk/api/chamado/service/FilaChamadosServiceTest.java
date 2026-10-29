package com.avandesk.api.chamado.service;

import com.avandesk.api.chamado.dto.ChamadoResumoResponse;
import com.avandesk.api.chamado.dto.FilaChamadosResponse;
import com.avandesk.api.chamado.dto.FiltroChamados;
import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.chamado.repository.ChamadoRepository;
import com.avandesk.api.chamado.repository.EventoHistoricoRepository;
import com.avandesk.api.chamado.repository.EventoHistoricoRepository.UltimaAtualizacao;
import com.avandesk.api.core.exception.RegraNegocioException;
import com.avandesk.api.unidade.entity.Unidade;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.service.UsuarioLogadoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilaChamadosServiceTest {

    private static final FiltroChamados SEM_FILTROS = new FiltroChamados(null, null, null, null, null, false, null);

    @Mock
    private ChamadoRepository chamadoRepository;

    @Mock
    private EventoHistoricoRepository eventoHistoricoRepository;

    @Mock
    private UsuarioLogadoService usuarioLogadoService;

    @InjectMocks
    private FilaChamadosService filaChamadosService;

    // ---------- paginação ----------

    @Test
    void deveRecusarPaginaNegativa() {
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
            () -> filaChamadosService.listar(SEM_FILTROS, false, -1, 20));

        assertEquals("O parâmetro 'page' deve ser 0 ou maior.", erro.getMessage());
        verifyNoInteractions(chamadoRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    void deveRecusarTamanhoForaDoLimite(int tamanho) {
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
            () -> filaChamadosService.listar(SEM_FILTROS, false, 0, tamanho));

        assertEquals("O parâmetro 'size' deve estar entre 1 e 100.", erro.getMessage());
        verifyNoInteractions(chamadoRepository);
    }

    @Test
    void deveOrdenarDoMaisRecenteParaOMaisAntigoComDesempatePeloId() {
        prepararPagina(List.of());

        filaChamadosService.listar(SEM_FILTROS, false, 2, 20);

        ArgumentCaptor<Pageable> paginacao = ArgumentCaptor.forClass(Pageable.class);
        verify(chamadoRepository).findAll(any(Specification.class), paginacao.capture());
        assertEquals(2, paginacao.getValue().getPageNumber());
        assertEquals(20, paginacao.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Order.desc("abertoEm"), Sort.Order.desc("id")), paginacao.getValue().getSort());
    }

    // ---------- "meus chamados" ----------

    @Test
    void naoDeveConsultarOUsuarioLogadoQuandoNaoPedeMeusChamados() {
        prepararPagina(List.of());

        filaChamadosService.listar(SEM_FILTROS, false, 0, 20);

        verifyNoInteractions(usuarioLogadoService);
    }

    @Test
    void deveDescobrirQuemSouEuPeloUsuarioAutenticado() {
        when(usuarioLogadoService.obterUsuarioLogado()).thenReturn(usuario(UUID.randomUUID()));
        prepararPagina(List.of());

        filaChamadosService.listar(SEM_FILTROS, true, 0, 20);

        verify(usuarioLogadoService).obterUsuarioLogado();
        verify(chamadoRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void deveDevolverPaginaVaziaQuandoMeusVemComOutroResponsavel() {
        when(usuarioLogadoService.obterUsuarioLogado()).thenReturn(usuario(UUID.randomUUID()));
        FiltroChamados deOutraPessoa = new FiltroChamados(null, null, null, null, UUID.randomUUID(), false, null);

        FilaChamadosResponse fila = filaChamadosService.listar(deOutraPessoa, true, 0, 20);

        assertTrue(fila.itens().isEmpty());
        assertEquals(0, fila.totalItens());
        assertEquals(0L, fila.totaisPorStatus().get(StatusChamado.ABERTO));
        verifyNoInteractions(chamadoRepository, eventoHistoricoRepository);
    }

    // ---------- resposta ----------

    @Test
    void deveMontarAsLinhasComOUltimoEventoOuAAberturaComoAtualizacao() {
        Instant abertura = Instant.parse("2026-10-29T12:00:00Z");
        Instant ultimoEvento = Instant.parse("2026-10-29T12:30:00Z");
        Chamado comEventos = chamado(abertura, "Impressora de etiquetas parada\nDetalhes...");
        Chamado semEventos = chamado(abertura.minusSeconds(60), "Rede instável");
        prepararPagina(List.of(comEventos, semEventos));
        when(eventoHistoricoRepository.buscarUltimaAtualizacao(anyCollection()))
            .thenReturn(List.of(ultimaAtualizacao(comEventos.getId(), ultimoEvento)));

        FilaChamadosResponse fila = filaChamadosService.listar(SEM_FILTROS, false, 0, 20);

        ChamadoResumoResponse primeira = fila.itens().get(0);
        assertEquals("Impressora de etiquetas parada", primeira.titulo());
        assertEquals(ultimoEvento, primeira.atualizadoEm());
        assertEquals(semEventos.getAbertoEm(), fila.itens().get(1).atualizadoEm());
        assertEquals(null, primeira.responsavel());
    }

    @Test
    void deveDevolverOsNoveStatusNosTotaisMesmoComZero() {
        prepararPagina(List.of());
        when(chamadoRepository.contarPorStatus(any())).thenReturn(Map.of(StatusChamado.ABERTO, 3L));

        FilaChamadosResponse fila = filaChamadosService.listar(SEM_FILTROS, false, 0, 20);

        assertEquals(StatusChamado.values().length, fila.totaisPorStatus().size());
        assertEquals(3L, fila.totaisPorStatus().get(StatusChamado.ABERTO));
        assertEquals(0L, fila.totaisPorStatus().get(StatusChamado.MESCLADO));
    }

    @Test
    void naoDeveBuscarEventosQuandoAPaginaEstaVazia() {
        prepararPagina(List.of());

        FilaChamadosResponse fila = filaChamadosService.listar(SEM_FILTROS, false, 0, 20);

        assertTrue(fila.itens().isEmpty());
        verifyNoInteractions(eventoHistoricoRepository);
    }

    // ---------- apoio ----------

    private void prepararPagina(List<Chamado> chamados) {
        Page<Chamado> pagina = new PageImpl<>(chamados);
        when(chamadoRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pagina);
    }

    private static Chamado chamado(Instant abertoEm, String descricao) {
        Unidade unidade = new Unidade();
        unidade.setId(UUID.randomUUID());
        unidade.setNome("Unidade Boa Viagem");
        Chamado chamado = new Chamado();
        chamado.setId(UUID.randomUUID());
        chamado.setCodigo("LUM-2026-000001");
        chamado.setDescricao(descricao);
        chamado.setUnidade(unidade);
        chamado.setCanalOrigem(CanalOrigem.LIGACAO);
        chamado.setAbertoEm(abertoEm);
        return chamado;
    }

    private static Usuario usuario(UUID id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        return usuario;
    }

    private static UltimaAtualizacao ultimaAtualizacao(UUID chamadoId, Instant quando) {
        return new UltimaAtualizacao() {
            @Override
            public UUID getChamadoId() {
                return chamadoId;
            }

            @Override
            public Instant getUltimaAtualizacao() {
                return quando;
            }
        };
    }
}
