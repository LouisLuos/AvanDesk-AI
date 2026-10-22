package com.avandesk.api.service;

import com.avandesk.api.dto.ChamadoRequest;
import com.avandesk.api.dto.ChamadoResponse;
import com.avandesk.api.dto.SolicitanteRequest;
import com.avandesk.api.entity.CanalOrigem;
import com.avandesk.api.entity.Unidade;
import com.avandesk.api.repository.CategoriaRepository;
import com.avandesk.api.repository.UnidadeRepository;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes com o PostgreSQL de verdade (docker compose up -d), como no CI.
 * Os chamados criados aqui são desfeitos no fim de cada teste: nada fica no banco de dev,
 * só a sequence do código avança.
 */
@SpringBootTest
@WithMockUser(username = "analista1@lumen.local")
class ChamadoRegistroIntegracaoTest {

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
        unidade.setNome("Unidade de Teste de Integração");
        unidade.setIdentificacao("IT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        unidade = unidadeRepository.save(unidade);
        categoriaId = categoriaRepository.findByAtivoTrueOrderByNomeAsc().getFirst().getId();
    }

    @AfterEach
    void removerUnidade() {
        unidadeRepository.deleteById(unidade.getId());
    }

    @Test
    void deveGravarChamadoContatoEDoisEventosDoMesmoAutor() {
        emTransacaoDesfeita(jdbc -> {
            ChamadoResponse chamado = chamadoService.registrar(request("recepcao.it@lumen.local"));
            entityManager.flush();

            assertEquals(1L, jdbc.queryForObject(
                "SELECT count(*) FROM registros_contato WHERE chamado_id = ? AND direcao = 'RECEBIDO'",
                Long.class, chamado.id()));
            List<String> tipos = jdbc.queryForList(
                "SELECT tipo FROM eventos_historico WHERE chamado_id = ? ORDER BY tipo", String.class, chamado.id());
            assertEquals(List.of("CHAMADO_ABERTO", "REGISTRO_DE_CONTATO_RECEBIDO"), tipos);
            assertEquals(1L, jdbc.queryForObject(
                "SELECT count(DISTINCT e.autor_id) FROM eventos_historico e "
                    + "JOIN usuarios u ON u.id = e.autor_id "
                    + "WHERE e.chamado_id = ? AND u.email = 'analista1@lumen.local'",
                Long.class, chamado.id()));
            assertEquals("TESTE", jdbc.queryForObject(
                "SELECT origem_dado FROM chamados WHERE id = ?", String.class, chamado.id()));
        });
    }

    @Test
    void deveGerarCodigosUnicosESequenciaisEmCinquentaChamados() {
        emTransacaoDesfeita(jdbc -> {
            List<String> codigos = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                codigos.add(chamadoService.registrar(request("sequencia.it@lumen.local")).codigo());
            }

            assertEquals(50, new HashSet<>(codigos).size());
            for (int i = 1; i < codigos.size(); i++) {
                assertTrue(numero(codigos.get(i)) > numero(codigos.get(i - 1)),
                    "códigos fora de ordem: " + codigos.get(i - 1) + " e " + codigos.get(i));
            }
            assertTrue(codigos.getFirst().matches("LUM-\\d{4}-\\d{6}"), codigos.getFirst());
        });
    }

    @Test
    void deveImpedirAlteracaoEExclusaoDeEventosDoHistorico() {
        emTransacaoDesfeita(jdbc -> {
            ChamadoResponse chamado = chamadoService.registrar(request("trigger.it@lumen.local"));
            entityManager.flush();

            DataAccessException erro = assertThrows(DataAccessException.class, () -> jdbc.update(
                "UPDATE eventos_historico SET dados = '{}' WHERE chamado_id = ?", chamado.id()));
            assertTrue(erro.getMostSpecificCause().getMessage().contains("so aceita INSERT"),
                erro.getMostSpecificCause().getMessage());
        });
        emTransacaoDesfeita(jdbc -> {
            ChamadoResponse chamado = chamadoService.registrar(request("trigger.it@lumen.local"));
            entityManager.flush();

            assertThrows(DataAccessException.class,
                () -> jdbc.update("DELETE FROM eventos_historico WHERE chamado_id = ?", chamado.id()));
        });
    }

    // ---------- apoio ----------

    /** Roda o bloco numa transação que sempre termina em rollback, para não deixar dados no banco de dev. */
    private void emTransacaoDesfeita(Consumer<JdbcTemplate> bloco) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            status.setRollbackOnly();
            bloco.accept(jdbcTemplate);
        });
    }

    private ChamadoRequest request(String contato) {
        return new ChamadoRequest(unidade.getId(), CanalOrigem.LIGACAO,
            new SolicitanteRequest("Teste de Integração", contato),
            "Impressora de etiquetas da coleta não imprime.", categoriaId, null, null);
    }

    private long contar(String tabela) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + tabela, Long.class);
    }

    private static long numero(String codigo) {
        return Long.parseLong(codigo.substring(codigo.lastIndexOf('-') + 1));
    }
}
