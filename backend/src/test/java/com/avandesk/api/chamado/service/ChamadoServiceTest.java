package com.avandesk.api.chamado.service;

import com.avandesk.api.catalogo.entity.Categoria;
import com.avandesk.api.catalogo.service.CategoriaService;
import com.avandesk.api.catalogo.service.SistemaEquipamentoService;
import com.avandesk.api.chamado.dto.ChamadoDetalheResponse;
import com.avandesk.api.chamado.dto.ChamadoRequest;
import com.avandesk.api.chamado.dto.ChamadoResponse;
import com.avandesk.api.chamado.dto.ResolucaoRequest;
import com.avandesk.api.chamado.dto.SolicitanteRequest;
import com.avandesk.api.chamado.dto.StatusRequest;
import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.DirecaoContato;
import com.avandesk.api.chamado.entity.EventoHistorico;
import com.avandesk.api.chamado.entity.MotivoEspera;
import com.avandesk.api.chamado.entity.OrigemDado;
import com.avandesk.api.chamado.entity.RegistroContato;
import com.avandesk.api.chamado.entity.Solicitante;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.chamado.entity.TipoEvento;
import com.avandesk.api.chamado.repository.ChamadoRepository;
import com.avandesk.api.chamado.repository.RegistroContatoRepository;
import com.avandesk.api.core.config.NexoProperties;
import com.avandesk.api.core.exception.OperacaoNaoPermitidaException;
import com.avandesk.api.core.exception.RecursoNaoEncontradoException;
import com.avandesk.api.core.exception.RegraNegocioException;
import com.avandesk.api.unidade.entity.Unidade;
import com.avandesk.api.unidade.service.UnidadeService;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.service.UsuarioLogadoService;
import com.avandesk.api.usuario.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChamadoServiceTest {

    @Mock
    private ChamadoRepository chamadoRepository;

    @Mock
    private RegistroContatoRepository registroContatoRepository;

    @Mock
    private EventoHistoricoService eventoHistoricoService;

    @Mock
    private UnidadeService unidadeService;

    @Mock
    private CategoriaService categoriaService;

    @Mock
    private SistemaEquipamentoService sistemaEquipamentoService;

    @Mock
    private SolicitanteService solicitanteService;

    @Mock
    private UsuarioLogadoService usuarioLogadoService;

    @Mock
    private UsuarioService usuarioService;

    private ChamadoService chamadoService;

    private final UUID unidadeId = UUID.randomUUID();
    private final UUID categoriaId = UUID.randomUUID();
    private final Unidade unidade = new Unidade();
    private final Categoria categoria = new Categoria();
    private final Usuario autor = new Usuario();
    private final Solicitante solicitante = new Solicitante();

    @BeforeEach
    void configurar() {
        chamadoService = new ChamadoService(chamadoRepository, registroContatoRepository, eventoHistoricoService,
            unidadeService, categoriaService, sistemaEquipamentoService, solicitanteService, usuarioLogadoService,
            usuarioService, new NexoProperties(OrigemDado.TESTE));
        autor.setEmail("analista1@lumen.local");
    }

    // ---------- sucesso ----------

    @Test
    void deveRegistrarChamadoComContatoInicialEDoisEventosDoMesmoAutor() {
        prepararCenarioValido(123L);

        ChamadoResponse response = chamadoService.registrar(request("  Impressora de etiquetas parada.  ", null));

        ArgumentCaptor<Chamado> chamadoSalvo = ArgumentCaptor.forClass(Chamado.class);
        verify(chamadoRepository).save(chamadoSalvo.capture());
        Chamado chamado = chamadoSalvo.getValue();
        assertSame(unidade, chamado.getUnidade());
        assertSame(categoria, chamado.getCategoria());
        assertSame(solicitante, chamado.getSolicitante());
        assertSame(autor, chamado.getCriadoPor());
        assertEquals(CanalOrigem.LIGACAO, chamado.getCanalOrigem());
        assertEquals("Impressora de etiquetas parada.", chamado.getDescricao());
        assertEquals(StatusChamado.ABERTO, chamado.getStatus());
        assertEquals(OrigemDado.TESTE, chamado.getOrigemDado());

        ArgumentCaptor<RegistroContato> contatoSalvo = ArgumentCaptor.forClass(RegistroContato.class);
        verify(registroContatoRepository).save(contatoSalvo.capture());
        RegistroContato contato = contatoSalvo.getValue();
        assertSame(chamado, contato.getChamado());
        assertEquals(DirecaoContato.RECEBIDO, contato.getDirecao());
        assertEquals(CanalOrigem.LIGACAO, contato.getCanal());
        assertEquals(chamado.getDescricao(), contato.getConteudo());
        assertEquals(chamado.getAbertoEm(), contato.getOcorridoEm());
        assertSame(autor, contato.getRegistradoPor());
        assertEquals(OrigemDado.TESTE, contato.getOrigemDado());

        ArgumentCaptor<TipoEvento> tipos = ArgumentCaptor.forClass(TipoEvento.class);
        verify(eventoHistoricoService, times(2)).registrar(eq(chamado), tipos.capture(), eq(autor), anyMap());
        assertEquals(List.of(TipoEvento.CHAMADO_ABERTO, TipoEvento.REGISTRO_DE_CONTATO_RECEBIDO), tipos.getAllValues());

        assertEquals(chamado.getId(), response.id());
        assertEquals(StatusChamado.ABERTO, response.status());
        assertEquals(OrigemDado.TESTE, response.origemDado());
        assertEquals(chamado.getAbertoEm(), response.dataCriacao());
    }

    @Test
    void deveGerarCodigoComAnoDeRecifeESeisDigitos() {
        prepararCenarioValido(123L);

        ChamadoResponse response = chamadoService.registrar(request("Impressora de etiquetas parada.", null));

        int ano = response.dataCriacao().atZone(ZoneId.of("America/Recife")).getYear();
        assertEquals("LUM-%d-000123".formatted(ano), response.codigo());
    }

    @Test
    void naoDeveConsultarSistemaQuandoNaoInformado() {
        prepararCenarioValido(1L);

        chamadoService.registrar(request("Impressora de etiquetas parada.", null));

        verifyNoInteractions(sistemaEquipamentoService);
    }

    // ---------- unidade ----------

    @Test
    void naoDeveGravarNadaQuandoUnidadeNaoExiste() {
        when(unidadeService.obterAtivaParaNovoChamado(unidadeId))
            .thenThrow(new RecursoNaoEncontradoException("Unidade não encontrada."));

        assertThrows(RecursoNaoEncontradoException.class,
            () -> chamadoService.registrar(request("Impressora de etiquetas parada.", null)));

        verify(chamadoRepository, never()).save(any());
        verify(registroContatoRepository, never()).save(any());
        verifyNoInteractions(eventoHistoricoService);
    }

    @Test
    void naoDeveGravarNadaQuandoUnidadeEstaInativa() {
        when(unidadeService.obterAtivaParaNovoChamado(unidadeId))
            .thenThrow(new RegraNegocioException("Unidade inativa não pode receber novos chamados."));

        assertThrows(RegraNegocioException.class,
            () -> chamadoService.registrar(request("Impressora de etiquetas parada.", null)));

        verify(chamadoRepository, never()).save(any());
        verify(registroContatoRepository, never()).save(any());
        verifyNoInteractions(eventoHistoricoService);
    }

    // ---------- registro_iniciado_em (métrica M11) ----------

    @Test
    void deveGuardarRegistroIniciadoEmDentroDaJanela() {
        prepararCenarioValido(1L);
        Instant inicio = Instant.now().minusSeconds(45);

        chamadoService.registrar(request("Impressora de etiquetas parada.", inicio));

        Chamado chamado = chamadoSalvo();
        assertEquals(inicio, chamado.getRegistroIniciadoEm());
        assertTrue(chamado.getAbertoEm().isAfter(inicio));
    }

    @Test
    void deveDescartarRegistroIniciadoEmNoFuturoSemRecusarOChamado() {
        prepararCenarioValido(1L);

        chamadoService.registrar(request("Impressora de etiquetas parada.", Instant.now().plusSeconds(600)));

        assertNull(chamadoSalvo().getRegistroIniciadoEm());
    }

    @Test
    void deveDescartarRegistroIniciadoEmMaisDeDuasHorasAntesSemRecusarOChamado() {
        prepararCenarioValido(1L);

        chamadoService.registrar(request("Impressora de etiquetas parada.", Instant.now().minusSeconds(3 * 3600)));

        assertNull(chamadoSalvo().getRegistroIniciadoEm());
    }

    // ---------- detalhar (HU02 / HU05) ----------

    @Test
    void deveDetalharChamadoComContatosEHistoricoOrdenados() {
        UUID chamadoId = UUID.randomUUID();
        unidade.setId(unidadeId);
        unidade.setNome("U.03 Boa Viagem");
        categoria.setId(categoriaId);
        categoria.setNome("Equipamento de apoio");
        solicitante.setNome("Recepção Boa Viagem");
        solicitante.setTelefone("+5581999999999");
        autor.setNome("Analista Dev Um");

        Chamado chamado = new Chamado();
        chamado.setId(chamadoId);
        chamado.setCodigo("LUM-2026-000001");
        chamado.setUnidade(unidade);
        chamado.setCategoria(categoria);
        chamado.setSolicitante(solicitante);
        chamado.setCanalOrigem(CanalOrigem.LIGACAO);
        chamado.setDescricao("Impressora de etiquetas parada.");
        chamado.setStatus(StatusChamado.ABERTO);
        chamado.setAbertoEm(Instant.parse("2026-10-29T12:00:00Z"));
        chamado.setOrigemDado(OrigemDado.TESTE);

        RegistroContato contato = new RegistroContato();
        contato.setId(UUID.randomUUID());
        contato.setChamado(chamado);
        contato.setSolicitante(solicitante);
        contato.setCanal(CanalOrigem.LIGACAO);
        contato.setDirecao(DirecaoContato.RECEBIDO);
        contato.setConteudo("Impressora de etiquetas parada.");
        contato.setOcorridoEm(chamado.getAbertoEm());
        contato.setRegistradoPor(autor);

        EventoHistorico evento = new EventoHistorico(
            chamado,
            TipoEvento.CHAMADO_ABERTO,
            chamado.getAbertoEm(),
            autor,
            Map.of("canal", "LIGACAO"),
            chamado.getOrigemDado()
        );
        ReflectionTestUtils.setField(evento, "id", UUID.randomUUID());

        when(chamadoRepository.findDetalheById(chamadoId)).thenReturn(Optional.of(chamado));
        when(registroContatoRepository.findByChamadoIdOrderByOcorridoEmAsc(chamadoId)).thenReturn(List.of(contato));
        when(eventoHistoricoService.listarDoChamado(chamadoId)).thenReturn(List.of(evento));

        ChamadoDetalheResponse detalhe = chamadoService.detalhar(chamadoId);

        assertEquals(chamadoId, detalhe.id());
        assertEquals("LUM-2026-000001", detalhe.codigo());
        assertEquals(StatusChamado.ABERTO, detalhe.status());
        assertNull(detalhe.responsavel());
        assertEquals(1, detalhe.registrosContato().size());
        assertEquals(1, detalhe.historico().size());
        assertEquals(TipoEvento.CHAMADO_ABERTO, detalhe.historico().getFirst().tipo());
        assertEquals("Analista Dev Um", detalhe.historico().getFirst().autor());
    }

    @Test
    void deveLancar404AoDetalharChamadoInexistente() {
        UUID chamadoId = UUID.randomUUID();
        when(chamadoRepository.findDetalheById(chamadoId)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> chamadoService.detalhar(chamadoId));
        verifyNoInteractions(registroContatoRepository, eventoHistoricoService);
    }

    // ---------- resolução, reabertura e retomada (HU06) ----------

    @Test
    void deveResolverChamadoEmAtendimentoComCausaRaizERecorrencia() {
        Chamado chamado = chamadoParaDetalhe(StatusChamado.EM_ATENDIMENTO);
        when(usuarioLogadoService.obterUsuarioLogado()).thenReturn(autor);

        ChamadoDetalheResponse detalhe = chamadoService.resolver(
            chamado.getId(),
            new ResolucaoRequest("  Cabo de rede substituído na bancada.  ", "  Conector RJ45 oxidado.  ", true)
        );

        assertEquals(StatusChamado.RESOLVIDO, chamado.getStatus());
        assertEquals("Cabo de rede substituído na bancada.", chamado.getResolucao());
        assertEquals("Conector RJ45 oxidado.", chamado.getCausaRaiz());
        assertTrue(chamado.isPossivelRecorrencia());
        assertNotNull(chamado.getResolvidoEm());
        assertNotNull(chamado.getPrimeiraRespostaEm());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> dadosCaptor = ArgumentCaptor.forClass(Map.class);
        verify(eventoHistoricoService).registrar(
            eq(chamado), eq(TipoEvento.CHAMADO_RESOLVIDO), eq(autor), dadosCaptor.capture()
        );
        Map<String, Object> dados = dadosCaptor.getValue();
        assertEquals("EM_ATENDIMENTO", dados.get("status_anterior"));
        assertEquals("Cabo de rede substituído na bancada.", dados.get("resolucao"));
        assertEquals("Conector RJ45 oxidado.", dados.get("causa_raiz"));
        assertEquals(true, dados.get("possivel_recorrencia"));

        assertEquals(StatusChamado.RESOLVIDO, detalhe.status());
        assertEquals("Cabo de rede substituído na bancada.", detalhe.resolucao());
        assertEquals("Conector RJ45 oxidado.", detalhe.causaRaiz());
        assertTrue(detalhe.possivelRecorrencia());
        assertNotNull(detalhe.resolvidoEm());
    }

    @Test
    void deveResolverChamadoAguardandoLimpandoEsperaESemCausaRaizQuandoEmBranco() {
        Chamado chamado = chamadoParaDetalhe(StatusChamado.AGUARDANDO);
        chamado.setMotivoEspera(MotivoEspera.PECA_EQUIPAMENTO);
        chamado.setEsperaIniciadaEm(Instant.now().minusSeconds(600));
        when(usuarioLogadoService.obterUsuarioLogado()).thenReturn(autor);

        chamadoService.resolver(
            chamado.getId(),
            new ResolucaoRequest("Troca do rolo térmico concluída.", "   ", null)
        );

        assertEquals(StatusChamado.RESOLVIDO, chamado.getStatus());
        assertNull(chamado.getCausaRaiz());
        assertFalse(chamado.isPossivelRecorrencia());
        assertNull(chamado.getMotivoEspera());
        assertNull(chamado.getEsperaIniciadaEm());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> dadosCaptor = ArgumentCaptor.forClass(Map.class);
        verify(eventoHistoricoService).registrar(
            eq(chamado), eq(TipoEvento.CHAMADO_RESOLVIDO), eq(autor), dadosCaptor.capture()
        );
        assertFalse(dadosCaptor.getValue().containsKey("causa_raiz"));
        assertEquals(false, dadosCaptor.getValue().get("possivel_recorrencia"));
    }

    @Test
    void naoDeveResolverChamadoForaDeEmAtendimentoOuAguardando() {
        Chamado chamado = chamadoParaDetalhe(StatusChamado.ABERTO);

        assertThrows(OperacaoNaoPermitidaException.class, () -> chamadoService.resolver(
            chamado.getId(),
            new ResolucaoRequest("Cabo de rede substituído na bancada.", null, false)
        ));

        assertEquals(StatusChamado.ABERTO, chamado.getStatus());
        verifyNoInteractions(eventoHistoricoService);
    }

    @Test
    void deveReabrirChamadoResolvidoDentroDoPrazoMantendoHistoricoDaResolucaoAnterior() {
        Chamado chamado = chamadoParaDetalhe(StatusChamado.RESOLVIDO);
        chamado.setResolvidoEm(Instant.now().minus(Duration.ofDays(2)));
        chamado.setResolucao("Serviço de impressão reiniciado.");
        chamado.setCausaRaiz("Fila de spooler travada.");
        chamado.setPossivelRecorrencia(true);
        when(usuarioLogadoService.obterUsuarioLogado()).thenReturn(autor);

        ChamadoDetalheResponse detalhe = chamadoService.alterarStatus(
            chamado.getId(),
            new StatusRequest(StatusChamado.REABERTO, null, null, "  Impressora voltou a travar no turno da tarde.  ")
        );

        assertEquals(StatusChamado.REABERTO, chamado.getStatus());
        assertNull(chamado.getResolvidoEm());
        assertEquals("Serviço de impressão reiniciado.", chamado.getResolucao());
        assertEquals("Fila de spooler travada.", chamado.getCausaRaiz());
        assertTrue(chamado.isPossivelRecorrencia());

        verify(eventoHistoricoService).registrar(
            eq(chamado),
            eq(TipoEvento.CHAMADO_REABERTO),
            eq(autor),
            eq(Map.of(
                "status_anterior", "RESOLVIDO",
                "motivo_reabertura", "Impressora voltou a travar no turno da tarde."
            ))
        );
        assertEquals(StatusChamado.REABERTO, detalhe.status());
    }

    @Test
    void naoDeveReabrirChamadoSemMotivoDeReabertura() {
        Chamado chamado = chamadoParaDetalhe(StatusChamado.RESOLVIDO);
        chamado.setResolvidoEm(Instant.now().minusSeconds(300));

        OperacaoNaoPermitidaException erro = assertThrows(OperacaoNaoPermitidaException.class,
            () -> chamadoService.alterarStatus(
                chamado.getId(),
                new StatusRequest(StatusChamado.REABERTO, null, null, "   ")
            ));

        assertEquals("Informe o motivo_reabertura para reabrir o chamado.", erro.getMessage());
        assertEquals(StatusChamado.RESOLVIDO, chamado.getStatus());
        verifyNoInteractions(eventoHistoricoService);
    }

    @Test
    void naoDeveReabrirChamadoAposPrazoDeSeteDias() {
        Chamado chamado = chamadoParaDetalhe(StatusChamado.RESOLVIDO);
        chamado.setResolvidoEm(Instant.now().minus(Duration.ofDays(8)));

        OperacaoNaoPermitidaException erro = assertThrows(OperacaoNaoPermitidaException.class,
            () -> chamadoService.alterarStatus(
                chamado.getId(),
                new StatusRequest(StatusChamado.REABERTO, null, null, "Problema voltou após uma semana.")
            ));

        assertTrue(erro.getMessage().contains("O prazo de 7 dias para reabrir terminou"));
        assertEquals(StatusChamado.RESOLVIDO, chamado.getStatus());
        verifyNoInteractions(eventoHistoricoService);
    }

    @Test
    void deveRetomarAtendimentoDeChamadoReabertoComResponsavel() {
        Chamado chamado = chamadoParaDetalhe(StatusChamado.REABERTO);
        autor.setId(UUID.randomUUID());
        autor.setNome("Analista Dev Um");
        chamado.setResponsavel(autor);
        when(usuarioLogadoService.obterUsuarioLogado()).thenReturn(autor);

        chamadoService.alterarStatus(
            chamado.getId(),
            new StatusRequest(StatusChamado.EM_ATENDIMENTO, null, null, null)
        );

        assertEquals(StatusChamado.EM_ATENDIMENTO, chamado.getStatus());
        verify(eventoHistoricoService).registrar(
            eq(chamado),
            eq(TipoEvento.ATENDIMENTO_RETOMADO),
            eq(autor),
            eq(Map.of("status_anterior", "REABERTO"))
        );
    }

    @Test
    void naoDeveRetomarAtendimentoDeChamadoReabertoSemResponsavel() {
        Chamado chamado = chamadoParaDetalhe(StatusChamado.REABERTO);
        chamado.setResponsavel(null);

        OperacaoNaoPermitidaException erro = assertThrows(OperacaoNaoPermitidaException.class,
            () -> chamadoService.alterarStatus(
                chamado.getId(),
                new StatusRequest(StatusChamado.EM_ATENDIMENTO, null, null, null)
            ));

        assertEquals("Atribua um responsável antes de retomar o atendimento.", erro.getMessage());
        assertEquals(StatusChamado.REABERTO, chamado.getStatus());
        verifyNoInteractions(eventoHistoricoService);
    }

    @Test
    void deveSobrescreverResolucaoECausaRaizAoResolverNovamenteAposReabertura() {
        Chamado chamado = chamadoParaDetalhe(StatusChamado.EM_ATENDIMENTO);
        chamado.setResolucao("Serviço de impressão reiniciado.");
        chamado.setCausaRaiz("Fila de spooler travada.");
        chamado.setPossivelRecorrencia(false);
        when(usuarioLogadoService.obterUsuarioLogado()).thenReturn(autor);

        ChamadoDetalheResponse detalhe = chamadoService.resolver(
            chamado.getId(),
            new ResolucaoRequest("Placa lógica da impressora substituída.", "Curto na controladora USB.", true)
        );

        assertEquals(StatusChamado.RESOLVIDO, chamado.getStatus());
        assertEquals("Placa lógica da impressora substituída.", chamado.getResolucao());
        assertEquals("Curto na controladora USB.", chamado.getCausaRaiz());
        assertTrue(chamado.isPossivelRecorrencia());
        assertNotNull(chamado.getResolvidoEm());

        assertEquals("Placa lógica da impressora substituída.", detalhe.resolucao());
        assertEquals("Curto na controladora USB.", detalhe.causaRaiz());
        assertTrue(detalhe.possivelRecorrencia());
    }

    // ---------- apoio ----------

    private void prepararCenarioValido(long numeroCodigo) {
        when(unidadeService.obterAtivaParaNovoChamado(unidadeId)).thenReturn(unidade);
        when(categoriaService.obterAtivaParaNovoChamado(categoriaId)).thenReturn(categoria);
        when(usuarioLogadoService.obterUsuarioLogado()).thenReturn(autor);
        when(solicitanteService.identificarOuCriar("Recepção Boa Viagem", "81999999999")).thenReturn(solicitante);
        when(chamadoRepository.proximoNumeroCodigo()).thenReturn(numeroCodigo);
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(invocacao -> {
            Chamado chamado = invocacao.getArgument(0);
            chamado.setId(UUID.randomUUID());
            return chamado;
        });
        when(registroContatoRepository.save(any(RegistroContato.class))).thenAnswer(invocacao -> {
            RegistroContato contato = invocacao.getArgument(0);
            contato.setId(UUID.randomUUID());
            return contato;
        });
    }

    private ChamadoRequest request(String descricao, Instant registroIniciadoEm) {
        return new ChamadoRequest(unidadeId, CanalOrigem.LIGACAO,
            new SolicitanteRequest("Recepção Boa Viagem", "81999999999"), descricao, categoriaId, null,
            registroIniciadoEm, null);
    }

    private Chamado chamadoSalvo() {
        ArgumentCaptor<Chamado> captor = ArgumentCaptor.forClass(Chamado.class);
        verify(chamadoRepository).save(captor.capture());
        return captor.getValue();
    }

    private Chamado chamadoParaDetalhe(StatusChamado status) {
        UUID chamadoId = UUID.randomUUID();
        unidade.setId(unidadeId);
        unidade.setNome("U.03 Boa Viagem");
        categoria.setId(categoriaId);
        categoria.setNome("Equipamento de apoio");
        solicitante.setNome("Recepção Boa Viagem");
        solicitante.setTelefone("+5581999999999");

        Chamado chamado = new Chamado();
        chamado.setId(chamadoId);
        chamado.setCodigo("LUM-2026-000042");
        chamado.setUnidade(unidade);
        chamado.setCategoria(categoria);
        chamado.setSolicitante(solicitante);
        chamado.setCanalOrigem(CanalOrigem.LIGACAO);
        chamado.setDescricao("Impressora de etiquetas parada.");
        chamado.setStatus(status);
        chamado.setAbertoEm(Instant.now().minusSeconds(1800));
        chamado.setOrigemDado(OrigemDado.TESTE);

        when(chamadoRepository.findDetalheById(chamadoId)).thenReturn(Optional.of(chamado));
        return chamado;
    }
}
