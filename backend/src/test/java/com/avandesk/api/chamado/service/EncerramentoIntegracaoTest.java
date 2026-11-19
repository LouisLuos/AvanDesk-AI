package com.avandesk.api.chamado.service;

import com.avandesk.api.catalogo.repository.CategoriaRepository;
import com.avandesk.api.chamado.dto.ChamadoDetalheResponse;
import com.avandesk.api.chamado.dto.ChamadoRequest;
import com.avandesk.api.chamado.dto.ChamadoResponse;
import com.avandesk.api.chamado.dto.FechamentoRequest;
import com.avandesk.api.chamado.dto.ResolucaoRequest;
import com.avandesk.api.chamado.dto.SolicitanteRequest;
import com.avandesk.api.chamado.dto.VerificacaoEncerramentoResponse;
import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.core.exception.OperacaoNaoPermitidaException;
import com.avandesk.api.unidade.entity.Unidade;
import com.avandesk.api.unidade.repository.UnidadeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** HU15 com o PostgreSQL de verdade: verificação de rastreabilidade e encerramento, em transação desfeita. */
@SpringBootTest
@WithMockUser(username = "analista1@lumen.local")
class EncerramentoIntegracaoTest {

    @Autowired
    private ChamadoService chamadoService;

    @Autowired
    private EncerramentoService encerramentoService;

    @Autowired
    private UnidadeRepository unidadeRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Unidade unidade;
    private UUID categoriaId;

    @BeforeEach
    void criarUnidade() {
        unidade = new Unidade();
        unidade.setNome("Unidade de Teste HU15");
        unidade.setIdentificacao("IT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        unidade = unidadeRepository.save(unidade);
        categoriaId = categoriaRepository.findByAtivoTrueOrderByNomeAsc().getFirst().getId();
    }

    @AfterEach
    void removerUnidade() {
        unidadeRepository.deleteById(unidade.getId());
    }

    @Test
    void deveEncerrarChamadoResolvidoGravandoFechadoEmEOResultadoDaVerificacao() {
        emTransacaoDesfeita(jdbc -> {
            UUID id = chamadoResolvido("Cabo de energia danificado");

            ChamadoDetalheResponse fechado = encerramentoService.fechar(id, null);
            entityManager.flush();

            assertEquals(StatusChamado.FECHADO, fechado.status());
            assertNotNull(jdbc.queryForObject("SELECT fechado_em FROM chamados WHERE id = ?", Object.class, id));
            Map<String, Object> evento = jdbc.queryForMap(
                "SELECT dados ->> 'itens_conformes' AS conformes, dados ->> 'total_itens' AS total, "
                    + "dados ->> 'status_anterior' AS anterior, (dados ->> 'justificativa_sem_causa_raiz') IS NOT NULL AS tem_justificativa "
                    + "FROM eventos_historico WHERE chamado_id = ? AND tipo = 'CHAMADO_FECHADO'", id);
            assertEquals("6", evento.get("conformes"));
            assertEquals("6", evento.get("total"));
            assertEquals("RESOLVIDO", evento.get("anterior"));
            assertEquals(false, evento.get("tem_justificativa"));
        });
    }

    @Test
    void deveApontarACausaRaizPendenteEAceitarAJustificativaNoEncerramento() {
        emTransacaoDesfeita(jdbc -> {
            UUID id = chamadoResolvido(null);

            VerificacaoEncerramentoResponse verificacao = encerramentoService.verificar(id);
            assertFalse(verificacao.aprovado());
            assertEquals(5, verificacao.itensConformes());

            OperacaoNaoPermitidaException erro = assertThrows(OperacaoNaoPermitidaException.class,
                () -> encerramentoService.fechar(id, new FechamentoRequest("Sem causa")));
            assertEquals(1, erro.getErros().size());
            assertTrue(erro.getErros().getFirst().contains("causa raiz"));

            encerramentoService.fechar(id, new FechamentoRequest("Falha intermitente, sem causa identificada."));
            entityManager.flush();

            assertEquals("Falha intermitente, sem causa identificada.", jdbc.queryForObject(
                "SELECT dados ->> 'justificativa_sem_causa_raiz' FROM eventos_historico "
                    + "WHERE chamado_id = ? AND tipo = 'CHAMADO_FECHADO'", String.class, id));
        });
    }

    @Test
    void naoDeveEncerrarChamadoQueNaoEstaResolvido() {
        emTransacaoDesfeita(jdbc -> {
            ChamadoResponse chamado = chamadoService.registrar(request());

            OperacaoNaoPermitidaException erro = assertThrows(OperacaoNaoPermitidaException.class,
                () -> encerramentoService.fechar(chamado.id(), null));
            assertTrue(erro.getMessage().contains("RESOLVIDO"));
            assertEquals(List.of(), jdbc.queryForList(
                "SELECT tipo FROM eventos_historico WHERE chamado_id = ? AND tipo = 'CHAMADO_FECHADO'",
                String.class, chamado.id()));
        });
    }

    private UUID chamadoResolvido(String causaRaiz) {
        ChamadoResponse chamado = chamadoService.registrar(request());
        chamadoService.resolver(chamado.id(), new ResolucaoRequest(
            "Cabo trocado e impressora testada com etiqueta de amostra.", causaRaiz, false));
        return chamado.id();
    }

    private void emTransacaoDesfeita(Consumer<JdbcTemplate> bloco) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            status.setRollbackOnly();
            bloco.accept(jdbcTemplate);
        });
    }

    private ChamadoRequest request() {
        return new ChamadoRequest(unidade.getId(), CanalOrigem.LIGACAO,
            new SolicitanteRequest("Teste HU15", "hu15.it@lumen.local"),
            "Impressora de etiquetas da coleta não imprime.", categoriaId, null, null, true);
    }
}
