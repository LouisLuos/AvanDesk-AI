package com.avandesk.api.chamado.service;

import com.avandesk.api.catalogo.entity.Categoria;
import com.avandesk.api.catalogo.repository.CategoriaRepository;
import com.avandesk.api.chamado.dto.FilaChamadosResponse;
import com.avandesk.api.chamado.dto.FiltroChamados;
import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.OrigemDado;
import com.avandesk.api.chamado.entity.Solicitante;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.chamado.repository.ChamadoRepository;
import com.avandesk.api.chamado.repository.SolicitanteRepository;
import com.avandesk.api.unidade.entity.Unidade;
import com.avandesk.api.unidade.repository.UnidadeRepository;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RNF-004 / HU14: com 1.000 chamados no banco, a primeira página da fila responde com p95 abaixo de 1.000 ms.
 * Os chamados são criados numa transação desfeita no fim: nada fica no banco de dev.
 */
@Slf4j
@SpringBootTest
@WithMockUser(username = "analista1@lumen.local")
class FilaChamadosPerformanceTest {

    private static final int QUANTIDADE_DE_CHAMADOS = 1_000;
    private static final int AQUECIMENTO = 3;
    private static final int MEDICOES = 20;
    private static final Duration LIMITE_P95 = Duration.ofMillis(1_000);

    private static final StatusChamado[] STATUS_DA_FILA =
        {StatusChamado.ABERTO, StatusChamado.EM_TRIAGEM, StatusChamado.EM_ATENDIMENTO};

    @Autowired
    private FilaChamadosService filaChamadosService;

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private UnidadeRepository unidadeRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private SolicitanteRepository solicitanteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void deveListarAPrimeiraPaginaComMilChamadosEmMenosDeUmSegundo() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            status.setRollbackOnly();
            Unidade unidade = criarMilChamados();
            entityManager.flush();
            entityManager.clear();

            FiltroChamados semFiltros = new FiltroChamados(null, null, null, null, null, false, false, null);
            FiltroChamados daUnidade = new FiltroChamados(null, unidade.getId(), null, null, null, false, false, null);
            for (int i = 0; i < AQUECIMENTO; i++) {
                filaChamadosService.listar(semFiltros, false, 0, 20);
            }

            Duration p95SemFiltros = p95(() -> filaChamadosService.listar(semFiltros, false, 0, 20));
            Duration p95DaUnidade = p95(() -> filaChamadosService.listar(daUnidade, false, 0, 20));
            FilaChamadosResponse fila = filaChamadosService.listar(daUnidade, false, 0, 20);

            log.info("Fila com {} chamados: p95 sem filtros = {} ms, p95 por unidade = {} ms",
                QUANTIDADE_DE_CHAMADOS, p95SemFiltros.toMillis(), p95DaUnidade.toMillis());
            assertEquals(QUANTIDADE_DE_CHAMADOS, fila.totalItens());
            assertEquals(20, fila.itens().size());
            assertTrue(p95SemFiltros.compareTo(LIMITE_P95) < 0, "p95 sem filtros: " + p95SemFiltros.toMillis() + " ms");
            assertTrue(p95DaUnidade.compareTo(LIMITE_P95) < 0, "p95 por unidade: " + p95DaUnidade.toMillis() + " ms");
        });
    }

    private Duration p95(Runnable chamada) {
        List<Long> tempos = new ArrayList<>();
        for (int i = 0; i < MEDICOES; i++) {
            entityManager.clear();
            long inicio = System.nanoTime();
            chamada.run();
            tempos.add(System.nanoTime() - inicio);
        }
        tempos.sort(null);
        int indice = (int) Math.ceil(0.95 * MEDICOES) - 1;
        return Duration.ofNanos(tempos.get(indice));
    }

    private Unidade criarMilChamados() {
        Categoria categoria = categoriaRepository.findByAtivoTrueOrderByNomeAsc().getFirst();
        Usuario autor = usuarioRepository.findByEmail("analista1@lumen.local").orElseThrow();
        Solicitante solicitante = new Solicitante();
        solicitante.setNome("Solicitante de Teste de Performance");
        solicitante.setTelefone("+5581900000001");
        solicitanteRepository.save(solicitante);

        Unidade unidade = new Unidade();
        unidade.setNome("Unidade de Teste de Performance");
        unidade.setIdentificacao("PF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        unidadeRepository.save(unidade);

        int prefixo = ThreadLocalRandom.current().nextInt(10, 99);
        Instant agora = Instant.now();
        List<Chamado> chamados = new ArrayList<>(QUANTIDADE_DE_CHAMADOS);
        for (int i = 0; i < QUANTIDADE_DE_CHAMADOS; i++) {
            Chamado chamado = new Chamado();
            chamado.setCodigo("LUM-2098-%02d%04d".formatted(prefixo, i));
            chamado.setUnidade(unidade);
            chamado.setSolicitante(solicitante);
            chamado.setCanalOrigem(CanalOrigem.values()[i % CanalOrigem.values().length]);
            chamado.setDescricao("Chamado %d do teste de performance da fila (HU14).".formatted(i));
            chamado.setCategoria(categoria);
            chamado.setStatus(STATUS_DA_FILA[i % STATUS_DA_FILA.length]);
            chamado.setCriadoPor(autor);
            chamado.setResponsavel(i % 2 == 0 ? autor : null);
            chamado.setAbertoEm(agora.minusSeconds(i));
            chamado.setOrigemDado(OrigemDado.TESTE);
            chamados.add(chamado);
        }
        chamadoRepository.saveAll(chamados);
        return unidade;
    }
}
