package com.avandesk.api.chamado.service;

import com.avandesk.api.catalogo.entity.Categoria;
import com.avandesk.api.catalogo.repository.CategoriaRepository;
import com.avandesk.api.chamado.dto.ChamadoResumoResponse;
import com.avandesk.api.chamado.dto.FilaChamadosResponse;
import com.avandesk.api.chamado.dto.FiltroChamados;
import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.OrigemDado;
import com.avandesk.api.chamado.entity.Prioridade;
import com.avandesk.api.chamado.entity.Solicitante;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.chamado.entity.TipoEvento;
import com.avandesk.api.chamado.repository.ChamadoRepository;
import com.avandesk.api.chamado.repository.SolicitanteRepository;
import com.avandesk.api.unidade.entity.Unidade;
import com.avandesk.api.unidade.repository.UnidadeRepository;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fila de chamados (HU14) com o PostgreSQL de verdade (docker compose up -d), como no CI.
 * Cada teste cria os próprios chamados em unidades novas e desfaz tudo no fim: nada fica no banco de dev.
 * Os filtros sempre incluem a unidade do teste, para não misturar com chamados que já existam no banco.
 */
@SpringBootTest
@WithMockUser(username = "analista1@lumen.local")
class FilaChamadosIntegracaoTest {

    private static final Instant AGORA = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    @Autowired
    private FilaChamadosService filaChamadosService;

    @Autowired
    private EventoHistoricoService eventoHistoricoService;

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
    void deveListarDoMaisRecenteParaOMaisAntigo() {
        emTransacaoDesfeita(dados -> {
            Unidade unidade = dados.unidade();
            Chamado antigo = dados.chamado(unidade, StatusChamado.ABERTO, AGORA.minus(3, ChronoUnit.HOURS));
            Chamado recente = dados.chamado(unidade, StatusChamado.ABERTO, AGORA.minus(10, ChronoUnit.MINUTES));
            Chamado meio = dados.chamado(unidade, StatusChamado.ABERTO, AGORA.minus(1, ChronoUnit.HOURS));

            List<String> codigos = codigos(listar(daUnidade(unidade), false, 0, 20));

            assertEquals(List.of(recente.getCodigo(), meio.getCodigo(), antigo.getCodigo()), codigos);
        });
    }

    @Test
    void deveCombinarFiltrosDevolvendoSoAIntersecao() {
        emTransacaoDesfeita(dados -> {
            Unidade unidade = dados.unidade();
            Unidade outraUnidade = dados.unidade();
            Usuario analista2 = dados.analista2();
            Chamado alvo = dados.chamado(unidade, StatusChamado.EM_ATENDIMENTO, AGORA, Prioridade.ALTA,
                CanalOrigem.WHATSAPP, analista2);
            dados.chamado(unidade, StatusChamado.EM_ATENDIMENTO, AGORA, Prioridade.BAIXA, CanalOrigem.WHATSAPP, analista2);
            dados.chamado(unidade, StatusChamado.ABERTO, AGORA, Prioridade.ALTA, CanalOrigem.WHATSAPP, analista2);
            dados.chamado(unidade, StatusChamado.EM_ATENDIMENTO, AGORA, Prioridade.ALTA, CanalOrigem.EMAIL, analista2);
            dados.chamado(unidade, StatusChamado.EM_ATENDIMENTO, AGORA, Prioridade.ALTA, CanalOrigem.WHATSAPP, null);
            dados.chamado(outraUnidade, StatusChamado.EM_ATENDIMENTO, AGORA, Prioridade.ALTA, CanalOrigem.WHATSAPP,
                analista2);

            FiltroChamados filtro = new FiltroChamados(List.of(StatusChamado.EM_ATENDIMENTO), unidade.getId(),
                Prioridade.ALTA, CanalOrigem.WHATSAPP, analista2.getId(), false, false, null);

            assertEquals(List.of(alvo.getCodigo()), codigos(listar(filtro, false, 0, 20)));
        });
    }

    @Test
    void deveAceitarMaisDeUmStatus() {
        emTransacaoDesfeita(dados -> {
            Unidade unidade = dados.unidade();
            Chamado aberto = dados.chamado(unidade, StatusChamado.ABERTO, AGORA.minusSeconds(30));
            Chamado triagem = dados.chamado(unidade, StatusChamado.EM_TRIAGEM, AGORA.minusSeconds(20));
            dados.chamado(unidade, StatusChamado.EM_ATENDIMENTO, AGORA.minusSeconds(10));

            FiltroChamados filtro = new FiltroChamados(List.of(StatusChamado.ABERTO, StatusChamado.EM_TRIAGEM),
                unidade.getId(), null, null, null, false, false, null);

            assertEquals(List.of(triagem.getCodigo(), aberto.getCodigo()), codigos(listar(filtro, false, 0, 20)));
        });
    }

    @Test
    void deveListarSoOsChamadosSemResponsavel() {
        emTransacaoDesfeita(dados -> {
            Unidade unidade = dados.unidade();
            Chamado semDono = dados.chamado(unidade, StatusChamado.ABERTO, AGORA);
            dados.chamado(unidade, StatusChamado.EM_ATENDIMENTO, AGORA, null, CanalOrigem.LIGACAO, dados.analista1());

            FiltroChamados filtro = new FiltroChamados(null, unidade.getId(), null, null, null, true, false, null);
            FilaChamadosResponse fila = listar(filtro, false, 0, 20);

            assertEquals(List.of(semDono.getCodigo()), codigos(fila));
            assertNull(fila.itens().getFirst().responsavel());
        });
    }

    @Test
    void deveListarSoOsChamadosSemPrioridade() {
        emTransacaoDesfeita(dados -> {
            Unidade unidade = dados.unidade();
            Chamado aguardandoTriagem = dados.chamado(unidade, StatusChamado.ABERTO, AGORA);
            dados.chamado(unidade, StatusChamado.EM_ATENDIMENTO, AGORA, Prioridade.MEDIA, CanalOrigem.LIGACAO, null);

            FiltroChamados filtro = new FiltroChamados(null, unidade.getId(), null, null, null, false, true, null);
            FilaChamadosResponse fila = listar(filtro, false, 0, 20);

            assertEquals(List.of(aguardandoTriagem.getCodigo()), codigos(fila));
            assertNull(fila.itens().getFirst().prioridade());
        });
    }

    @Test
    void deveListarMeusChamadosPeloUsuarioDoToken() {
        emTransacaoDesfeita(dados -> {
            Unidade unidade = dados.unidade();
            Chamado meu = dados.chamado(unidade, StatusChamado.EM_ATENDIMENTO, AGORA, null, CanalOrigem.LIGACAO,
                dados.analista1());
            dados.chamado(unidade, StatusChamado.EM_ATENDIMENTO, AGORA, null, CanalOrigem.LIGACAO, dados.analista2());
            dados.chamado(unidade, StatusChamado.ABERTO, AGORA);

            FilaChamadosResponse fila = listar(daUnidade(unidade), true, 0, 20);

            assertEquals(List.of(meu.getCodigo()), codigos(fila));
            assertEquals("Analista Dev Um", fila.itens().getFirst().responsavel().nome());
        });
    }

    @Test
    void deveBuscarPeloCodigoExatoSemDiferenciarMaiusculas() {
        emTransacaoDesfeita(dados -> {
            Unidade unidade = dados.unidade();
            Chamado alvo = dados.chamado(unidade, StatusChamado.ABERTO, AGORA);
            dados.chamado(unidade, StatusChamado.ABERTO, AGORA);

            FiltroChamados filtro = new FiltroChamados(null, null, null, null, null, false, false,
                "  " + alvo.getCodigo().toLowerCase() + " ");

            assertEquals(List.of(alvo.getCodigo()), codigos(listar(filtro, false, 0, 20)));
        });
    }

    @Test
    void devePaginarSemRepetirNemPerderChamados() {
        emTransacaoDesfeita(dados -> {
            Unidade unidade = dados.unidade();
            Set<String> criados = new HashSet<>();
            for (int i = 0; i < 5; i++) {
                criados.add(dados.chamado(unidade, StatusChamado.ABERTO, AGORA.minusSeconds(i)).getCodigo());
            }

            FilaChamadosResponse pagina0 = listar(daUnidade(unidade), false, 0, 2);
            FilaChamadosResponse pagina1 = listar(daUnidade(unidade), false, 1, 2);
            FilaChamadosResponse pagina2 = listar(daUnidade(unidade), false, 2, 2);

            assertEquals(5, pagina0.totalItens());
            assertEquals(3, pagina0.totalPaginas());
            assertEquals(List.of(2, 2, 1),
                List.of(pagina0.itens().size(), pagina1.itens().size(), pagina2.itens().size()));
            Set<String> vistos = new HashSet<>();
            vistos.addAll(codigos(pagina0));
            vistos.addAll(codigos(pagina1));
            vistos.addAll(codigos(pagina2));
            assertEquals(criados, vistos);
        });
    }

    @Test
    void deveContarOsTotaisPorStatusIgnorandoOFiltroDeStatus() {
        emTransacaoDesfeita(dados -> {
            Unidade unidade = dados.unidade();
            dados.chamado(unidade, StatusChamado.ABERTO, AGORA);
            dados.chamado(unidade, StatusChamado.ABERTO, AGORA);
            dados.chamado(unidade, StatusChamado.EM_TRIAGEM, AGORA);

            FiltroChamados soTriagem = new FiltroChamados(List.of(StatusChamado.EM_TRIAGEM), unidade.getId(),
                null, null, null, false, false, null);
            FilaChamadosResponse fila = listar(soTriagem, false, 0, 20);

            assertEquals(1, fila.totalItens());
            Map<StatusChamado, Long> totais = fila.totaisPorStatus();
            assertEquals(2L, totais.get(StatusChamado.ABERTO));
            assertEquals(1L, totais.get(StatusChamado.EM_TRIAGEM));
            assertEquals(0L, totais.get(StatusChamado.RESOLVIDO));
        });
    }

    @Test
    void deveTrazerOUltimoEventoComoAtualizacao() {
        emTransacaoDesfeita(dados -> {
            Unidade unidade = dados.unidade();
            Chamado comEvento = dados.chamado(unidade, StatusChamado.ABERTO, AGORA.minus(2, ChronoUnit.HOURS));
            Chamado semEvento = dados.chamado(unidade, StatusChamado.ABERTO, AGORA.minus(3, ChronoUnit.HOURS));
            eventoHistoricoService.registrar(comEvento, TipoEvento.CHAMADO_ABERTO, dados.analista1(), Map.of());
            entityManager.flush();

            FilaChamadosResponse fila = listar(daUnidade(unidade), false, 0, 20);

            ChamadoResumoResponse linhaComEvento = fila.itens().get(0);
            ChamadoResumoResponse linhaSemEvento = fila.itens().get(1);
            assertTrue(linhaComEvento.atualizadoEm().isAfter(comEvento.getAbertoEm()));
            assertEquals(semEvento.getAbertoEm(), linhaSemEvento.atualizadoEm());
        });
    }

    // ---------- apoio ----------

    private FilaChamadosResponse listar(FiltroChamados filtro, boolean meus, int pagina, int tamanho) {
        entityManager.flush();
        return filaChamadosService.listar(filtro, meus, pagina, tamanho);
    }

    private static FiltroChamados daUnidade(Unidade unidade) {
        return new FiltroChamados(null, unidade.getId(), null, null, null, false, false, null);
    }

    private static List<String> codigos(FilaChamadosResponse fila) {
        return fila.itens().stream().map(ChamadoResumoResponse::codigo).toList();
    }

    /** Roda o teste numa transação que sempre termina em rollback, para não deixar dados no banco de dev. */
    private void emTransacaoDesfeita(Consumer<Dados> teste) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            status.setRollbackOnly();
            teste.accept(new Dados());
        });
    }

    /** Fábrica de dados de teste, criada dentro da transação desfeita. */
    private final class Dados {

        private final Categoria categoria = categoriaRepository.findByAtivoTrueOrderByNomeAsc().getFirst();
        private final Usuario analista1 = usuarioRepository.findByEmail("analista1@lumen.local").orElseThrow();
        private final Usuario analista2 = usuarioRepository.findByEmail("analista2@lumen.local").orElseThrow();
        private final Solicitante solicitante = criarSolicitante();
        private final int prefixo = ThreadLocalRandom.current().nextInt(100, 999);
        private int sequencia;

        Usuario analista1() {
            return analista1;
        }

        Usuario analista2() {
            return analista2;
        }

        Unidade unidade() {
            Unidade unidade = new Unidade();
            unidade.setNome("Unidade de Teste da Fila");
            unidade.setIdentificacao("IT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            return unidadeRepository.save(unidade);
        }

        Chamado chamado(Unidade unidade, StatusChamado status, Instant abertoEm) {
            return chamado(unidade, status, abertoEm, null, CanalOrigem.LIGACAO, null);
        }

        Chamado chamado(Unidade unidade, StatusChamado status, Instant abertoEm, Prioridade prioridade,
                        CanalOrigem canal, Usuario responsavel) {
            Chamado chamado = new Chamado();
            chamado.setCodigo("LUM-2099-%03d%03d".formatted(prefixo, ++sequencia));
            chamado.setUnidade(unidade);
            chamado.setSolicitante(solicitante);
            chamado.setCanalOrigem(canal);
            chamado.setDescricao("Chamado de teste da fila (HU14).");
            chamado.setCategoria(categoria);
            chamado.setStatus(status);
            chamado.setCriadoPor(analista1);
            chamado.setResponsavel(responsavel);
            chamado.setPrioridadeSugerida(prioridade);
            chamado.setPrioridadeFinal(prioridade);
            chamado.setAbertoEm(abertoEm);
            chamado.setOrigemDado(OrigemDado.TESTE);
            return chamadoRepository.save(chamado);
        }

        private Solicitante criarSolicitante() {
            Solicitante novo = new Solicitante();
            novo.setNome("Solicitante de Teste da Fila");
            novo.setTelefone("+5581900000000");
            return solicitanteRepository.save(novo);
        }
    }
}
