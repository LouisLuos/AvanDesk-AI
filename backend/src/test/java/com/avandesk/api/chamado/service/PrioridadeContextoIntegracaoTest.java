package com.avandesk.api.chamado.service;

import com.avandesk.api.catalogo.repository.CategoriaRepository;
import com.avandesk.api.chamado.dto.ChamadoRequest;
import com.avandesk.api.chamado.dto.ChamadoResponse;
import com.avandesk.api.chamado.dto.PrioridadeRequest;
import com.avandesk.api.chamado.dto.RespostasPrioridadeRequest;
import com.avandesk.api.chamado.dto.SolicitanteRequest;
import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.Prioridade;
import com.avandesk.api.core.exception.OperacaoNaoPermitidaException;
import com.avandesk.api.unidade.entity.Unidade;
import com.avandesk.api.unidade.repository.UnidadeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * HU22 com o PostgreSQL de verdade: abertura com as respostas do solicitante, confirmação direta e
 * reclassificação justificada pelo analista. Tudo roda em transação desfeita.
 */
@SpringBootTest
@WithMockUser(username = "analista1@lumen.local")
class PrioridadeContextoIntegracaoTest {

    @Autowired
    private ChamadoService chamadoService;

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
        unidade.setNome("Unidade de Teste HU22");
        unidade.setIdentificacao("IT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        unidade = unidadeRepository.save(unidade);
        categoriaId = categoriaRepository.findByAtivoTrueOrderByNomeAsc().getFirst().getId();
    }

    @AfterEach
    void removerUnidade() {
        unidadeRepository.deleteById(unidade.getId());
    }

    @Test
    void deveAbrirComASugeridaDasRespostasESemPrioridadeFinal() {
        emTransacaoDesfeita(jdbc -> {
            ChamadoResponse chamado = chamadoService.registrar(request(3, 3, 2));
            entityManager.flush();

            Map<String, Object> linha = jdbc.queryForMap(
                "SELECT severidade_informada, urgencia_informada, impacto_informado, prioridade_sugerida, "
                    + "prioridade_final, severidade FROM chamados WHERE id = ?", chamado.id());
            assertEquals(3, ((Number) linha.get("severidade_informada")).intValue());
            assertEquals(3, ((Number) linha.get("urgencia_informada")).intValue());
            assertEquals(2, ((Number) linha.get("impacto_informado")).intValue());
            assertEquals("ALTA", linha.get("prioridade_sugerida"));
            assertNull(linha.get("prioridade_final"));
            assertNull(linha.get("severidade"));
            assertEquals(Prioridade.ALTA, chamado.prioridadeSugerida());
        });
    }

    @Test
    void deveConfirmarASugeridaSemJustificativaEContarComoConcordante() {
        emTransacaoDesfeita(jdbc -> {
            ChamadoResponse chamado = chamadoService.registrar(request(2, 2, 2));
            chamadoService.definirPrioridade(chamado.id(),
                new PrioridadeRequest((short) 2, (short) 2, (short) 2, null, null));
            entityManager.flush();

            Map<String, Object> linha = jdbc.queryForMap(
                "SELECT prioridade_sugerida, prioridade_final, justificativa_prioridade FROM chamados WHERE id = ?",
                chamado.id());
            assertEquals("MEDIA", linha.get("prioridade_sugerida"));
            assertEquals("MEDIA", linha.get("prioridade_final"));
            assertNull(linha.get("justificativa_prioridade"));
            assertEquals(0L, divergentes(jdbc, chamado.id()));
        });
    }

    @Test
    void deveReclassificarComJustificativaMantendoASugeridaDaAbertura() {
        emTransacaoDesfeita(jdbc -> {
            ChamadoResponse chamado = chamadoService.registrar(request(3, 3, 3));
            chamadoService.definirPrioridade(chamado.id(), new PrioridadeRequest((short) 1, (short) 2, (short) 1,
                null, "Só uma estação afetada, há alternativa."));
            entityManager.flush();

            Map<String, Object> linha = jdbc.queryForMap(
                "SELECT prioridade_sugerida, prioridade_final, severidade_informada, severidade FROM chamados "
                    + "WHERE id = ?", chamado.id());
            assertEquals("ALTA", linha.get("prioridade_sugerida"));
            assertEquals("BAIXA", linha.get("prioridade_final"));
            assertEquals(3, ((Number) linha.get("severidade_informada")).intValue());
            assertEquals(1, ((Number) linha.get("severidade")).intValue());
            assertEquals(1L, divergentes(jdbc, chamado.id()));
        });
    }

    @Test
    void deveRecusarReclassificacaoComJustificativaCurta() {
        emTransacaoDesfeita(jdbc -> {
            ChamadoResponse chamado = chamadoService.registrar(request(3, 3, 3));

            assertThrows(OperacaoNaoPermitidaException.class, () -> chamadoService.definirPrioridade(chamado.id(),
                new PrioridadeRequest((short) 1, (short) 1, (short) 1, null, "Não é urgente")));
        });
    }

    @Test
    void deveBarrarNotaInformadaForaDeUmATresOuRespostasIncompletasNoBanco() {
        emTransacaoDesfeita(jdbc -> {
            ChamadoResponse chamado = chamadoService.registrar(request(1, 1, 1));
            entityManager.flush();

            assertThrows(DataAccessException.class, () -> jdbc.update(
                "UPDATE chamados SET severidade_informada = 4 WHERE id = ?", chamado.id()));
        });
        emTransacaoDesfeita(jdbc -> {
            ChamadoResponse chamado = chamadoService.registrar(request(1, 1, 1));
            entityManager.flush();

            assertThrows(DataAccessException.class, () -> jdbc.update(
                "UPDATE chamados SET urgencia_informada = NULL WHERE id = ?", chamado.id()));
        });
    }

    private long divergentes(JdbcTemplate jdbc, UUID chamadoId) {
        return jdbc.queryForObject(
            "SELECT count(*) FROM chamados WHERE id = ? AND prioridade_final <> prioridade_sugerida",
            Long.class, chamadoId);
    }

    private void emTransacaoDesfeita(Consumer<JdbcTemplate> bloco) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            status.setRollbackOnly();
            bloco.accept(jdbcTemplate);
        });
    }

    private ChamadoRequest request(int severidade, int urgencia, int impacto) {
        return new ChamadoRequest(unidade.getId(), CanalOrigem.LIGACAO,
            new SolicitanteRequest("Teste HU22", "hu22.it@lumen.local"),
            "Impressora de etiquetas da coleta não imprime.", categoriaId, null, null, null, null,
            new RespostasPrioridadeRequest((short) severidade, (short) urgencia, (short) impacto));
    }
}
