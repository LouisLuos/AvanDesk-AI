package com.avandesk.api.core.seed;

import com.avandesk.api.catalogo.entity.Categoria;
import com.avandesk.api.catalogo.entity.SistemaEquipamento;
import com.avandesk.api.catalogo.repository.CategoriaRepository;
import com.avandesk.api.catalogo.repository.SistemaEquipamentoRepository;
import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.DirecaoContato;
import com.avandesk.api.chamado.entity.Escalonamento;
import com.avandesk.api.chamado.entity.EventoHistorico;
import com.avandesk.api.chamado.entity.MotivoEspera;
import com.avandesk.api.chamado.entity.OrigemDado;
import com.avandesk.api.chamado.entity.Prioridade;
import com.avandesk.api.chamado.entity.RegistroContato;
import com.avandesk.api.chamado.entity.Solicitante;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.chamado.entity.TipoEvento;
import com.avandesk.api.chamado.repository.ChamadoRepository;
import com.avandesk.api.chamado.repository.EscalonamentoRepository;
import com.avandesk.api.chamado.repository.EventoHistoricoRepository;
import com.avandesk.api.chamado.repository.RegistroContatoRepository;
import com.avandesk.api.chamado.repository.SolicitanteRepository;
import com.avandesk.api.sla.domain.CalculadoraPrioridadeMatriz3D;
import com.avandesk.api.unidade.entity.Unidade;
import com.avandesk.api.unidade.repository.UnidadeRepository;
import com.avandesk.api.usuario.entity.PerfilUsuario;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Seeder de infraestrutura para simulação operacional completa de 60 dias (180 chamados) no perfil {@code demo}.
 *
 * <p>Espelha o porte real da Rede Lúmen Diagnósticos (14 unidades ativas, 7 analistas, 30 solicitantes e
 * 5 fornecedores externos) com isolamento arquitetural completo:
 * <ul>
 *   <li>Ativado apenas com {@code @Profile("demo")} + {@code nexo.seed.simulacao-habilitada=true}.</li>
 *   <li>Nunca carregado em {@code prod} nem no perfil padrão (preservando os testes integrados).</li>
 *   <li>Estritamente idempotente (respeita a trigger de imutabilidade de {@code eventos_historico}).</li>
 *   <li>Determinístico ({@code Random(SEED_FIXA)}) e com datas relativas a {@code Instant.now()}.</li>
 * </ul>
 */
@Slf4j
@Component
@Profile("demo")
@ConditionalOnProperty(name = "nexo.seed.simulacao-habilitada", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class SimulacaoOperacaoSeeder implements ApplicationRunner {

    private static final long SEED_FIXA = 20261119L;
    private static final OrigemDado ORIGEM_SIMULACAO = OrigemDado.SIMULADO;
    private static final ZoneId FUSO_DO_CODIGO = ZoneId.of("America/Recife");
    /** Mesmo hash BCrypt de R__dados_dev.sql (senha: nexo-dev-2026). */
    private static final String SENHA_DEV_HASH = "$2a$10$wP24GU5mL0ysN//Ga3xCOusMZmd/oxAXXqlRX4XQtK7LUhWPLYAa.";

    private final ChamadoRepository chamadoRepository;
    private final RegistroContatoRepository registroContatoRepository;
    private final EventoHistoricoRepository eventoHistoricoRepository;
    private final EscalonamentoRepository escalonamentoRepository;
    private final SolicitanteRepository solicitanteRepository;
    private final UsuarioRepository usuarioRepository;
    private final UnidadeRepository unidadeRepository;
    private final CategoriaRepository categoriaRepository;
    private final SistemaEquipamentoRepository sistemaEquipamentoRepository;
    private final TransactionTemplate transactionTemplate;

    private final CalculadoraPrioridadeMatriz3D calculadoraMatriz3D = new CalculadoraPrioridadeMatriz3D();

    @Override
    public void run(ApplicationArguments args) {
        transactionTemplate.executeWithoutResult(status -> popularSeNecessario());
    }

    /**
     * Executa a carga simulada de forma idempotente (não altera nem remove registros existentes).
     *
     * @return quantidade de chamados criados nesta execução (0 se já estiver populado).
     */
    public int popularSeNecessario() {
        return popularSeNecessario(ORIGEM_SIMULACAO);
    }

    int popularSeNecessario(OrigemDado origem) {
        if (chamadoRepository.countByOrigemDado(origem) > 0) {
            log.info("Simulação operacional (origem_dado={}) já presente no banco — seeder ignorado.", origem);
            return 0;
        }

        List<Unidade> unidades = garantirUnidadesSimulacao();
        List<Categoria> categorias = categoriaRepository.findByAtivoTrueOrderByNomeAsc();
        List<SistemaEquipamento> sistemas = sistemaEquipamentoRepository.findByAtivoTrueOrderByNomeAsc();

        if (unidades.isEmpty() || categorias.isEmpty() || sistemas.isEmpty()) {
            log.warn("Catálogos base (unidades/categorias/sistemas) vazios — seeder de simulação abortado.");
            return 0;
        }

        List<Usuario> analistas = garantirAnalistasSimulacao();
        if (analistas.isEmpty()) {
            log.warn("Nenhum analista ativo disponível — seeder de simulação abortado.");
            return 0;
        }

        List<Solicitante> solicitantes = garantirSolicitantesSimulacao();
        Random rng = new Random(SEED_FIXA);
        Instant agora = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        ContextoSimulacao ctx = new ContextoSimulacao(
            agora, rng, origem, unidades, categorias, sistemas, analistas, solicitantes
        );

        int totalCriados = 0;
        totalCriados += gerarPeriodoAnterior(ctx); // 72 chamados (D-58 a D-32)
        totalCriados += gerarPeriodoAtual(ctx);    // 108 chamados (D-29 a D-0)

        log.info(
            "Simulação operacional concluída: {} chamados criados (origem_dado={}, {} unidades ativas, {} analistas, janela de 60 dias).",
            totalCriados, origem, unidades.size(), analistas.size()
        );
        return totalCriados;
    }

    // -------------------------------------------------------------------------
    // Período Anterior (D-58 a D-32): 72 chamados — base comparativa do painel
    // SLA ~70% e mediana mais alta para destacar a evolução operacional no mês atual
    // -------------------------------------------------------------------------
    private int gerarPeriodoAnterior(ContextoSimulacao ctx) {
        List<Chamado> criados = new ArrayList<>();

        // 60 chamados encerrados (FECHADO / RESOLVIDO)
        for (int i = 0; i < 60; i++) {
            int diasAtras = 58 - (i * 26 / 60); // D-58 até D-32
            Instant abertoEm = instanteNoDia(ctx, diasAtras);
            CenarioCatalogo cenario = escolherCenarioPonderado(ctx.rng, i);
            boolean dentroSla = (i % 10) < 7; // 70% dentro da meta
            boolean comPausa = (i % 5) == 0;
            boolean divergencia = (i % 6) == 0;
            boolean comEscalonamento = (i % 6) == 1;
            boolean rastreavel = (i != 13 && i != 31 && i != 47); // 3 sem contato (~95% M01)

            Chamado chamado = criarChamadoEncerrado(
                ctx, abertoEm, cenario,
                (i % 3 == 0) ? StatusChamado.RESOLVIDO : StatusChamado.FECHADO,
                dentroSla, comPausa, divergencia, comEscalonamento, rastreavel,
                true // período anterior mais lento
            );
            criados.add(chamado);
        }

        // 6 chamados duplicados (MESCLADO)
        for (int i = 0; i < 6; i++) {
            Chamado principal = criados.get(i * 8);
            Instant abertoEm = principal.getAbertoEm().plus(18 + i * 9L, ChronoUnit.MINUTES);
            criados.add(criarChamadoMesclado(ctx, abertoEm, principal));
        }

        // 6 chamados cancelados
        for (int i = 0; i < 6; i++) {
            Instant abertoEm = instanteNoDia(ctx, 55 - i * 4);
            criados.add(criarChamadoCancelado(ctx, abertoEm, MOTIVOS_CANCELAMENTO.get(i % MOTIVOS_CANCELAMENTO.size())));
        }

        return criados.size();
    }

    // -------------------------------------------------------------------------
    // Período Atual (D-29 a D-0): 108 chamados — enriquece todos os KPIs, gráficos e fila
    // -------------------------------------------------------------------------
    private int gerarPeriodoAtual(ContextoSimulacao ctx) {
        List<Chamado> criados = new ArrayList<>();

        // 1) 54 chamados RESOLVIDO / FECHADO (3x a base anterior, com curvas realistas por prioridade)
        List<PrioridadeAlvo> planoEncerrados = montarPlanoEncerradosPeriodoAtual();

        for (int i = 0; i < planoEncerrados.size(); i++) {
            PrioridadeAlvo alvo = planoEncerrados.get(i);
            int diasAtras = 28 - (i * 26 / planoEncerrados.size()); // D-28 até D-2
            Instant abertoEm = instanteNoDia(ctx, diasAtras);
            CenarioCatalogo cenario = escolherCenarioPonderado(ctx.rng, i);
            StatusChamado statusFinal = (i % 2 == 0) ? StatusChamado.FECHADO : StatusChamado.RESOLVIDO;

            Chamado chamado = criarChamadoEncerradoComMatriz(ctx, abertoEm, cenario, statusFinal, alvo, false, i);
            criados.add(chamado);
        }

        // 2) 9 chamados REABERTO (alimenta M06 — taxa de reabertura realista ~14%)
        for (int i = 0; i < 9; i++) {
            int diasAtras = 24 - (i * 2); // D-24 até D-8
            Instant abertoEm = instanteNoDia(ctx, diasAtras);
            CenarioCatalogo cenario = CENARIOS_CLINICOS.get(i % 8); // focado nos cenários recorrentes
            criados.add(criarChamadoReaberto(ctx, abertoEm, cenario));
        }

        // 3) 6 chamados MESCLADO (alimenta M07 — taxa de duplicidade ~5.5%)
        for (int i = 0; i < 6; i++) {
            Chamado principal = criados.get(i * 7);
            Instant abertoEm = principal.getAbertoEm().plus(15 + i * 8L, ChronoUnit.MINUTES);
            criados.add(criarChamadoMesclado(ctx, abertoEm, principal));
        }

        // 4) 6 chamados CANCELADO
        for (int i = 0; i < 6; i++) {
            Instant abertoEm = instanteNoDia(ctx, 25 - i * 4);
            criados.add(criarChamadoCancelado(ctx, abertoEm, MOTIVOS_CANCELAMENTO.get(i % MOTIVOS_CANCELAMENTO.size())));
        }

        // 5) 33 chamados ATIVOS na fila (alimenta Fila paginada e M02 — Carga por responsável)
        // 5a) 9 ABERTO (6 sem responsável + 3 atribuídos)
        for (int i = 0; i < 9; i++) {
            Usuario resp = (i < 6) ? null : ctx.analista(i);
            CenarioCatalogo cenario = escolherCenarioPonderado(ctx.rng, i + 3);
            int diasAtras = (i < 5) ? 0 : 1;
            criados.add(criarChamadoAtivo(
                ctx, instanteNoDia(ctx, diasAtras), cenario, StatusChamado.ABERTO, resp,
                cenario.severidade, cenario.urgencia, cenario.impacto, null, i
            ));
        }

        // 5b) 6 EM_TRIAGEM (3 sem responsável + 3 atribuídos -> total de 9 "Sem responsável" em M02)
        for (int i = 0; i < 6; i++) {
            Usuario resp = (i < 3) ? null : ctx.analista(i);
            CenarioCatalogo cenario = escolherCenarioPonderado(ctx.rng, i + 7);
            int diasAtras = (i < 3) ? 0 : 1;
            criados.add(criarChamadoAtivo(
                ctx, instanteNoDia(ctx, diasAtras), cenario, StatusChamado.EM_TRIAGEM, resp,
                cenario.severidade, cenario.urgencia, cenario.impacto, null, i + 10
            ));
        }

        // 5c) 9 EM_ATENDIMENTO (carga realista e heterogênea nos 7 analistas, incluindo P1 críticos)
        int[] indicesAnalistasAtendimento = {0, 0, 1, 2, 2, 3, 4, 5, 6};
        for (int i = 0; i < 9; i++) {
            CenarioCatalogo cenario = escolherCenarioPonderado(ctx.rng, i + 11);
            int sev = (i % 3 == 0) ? 3 : cenario.severidade;
            int urg = (i % 3 == 0) ? 3 : cenario.urgencia;
            int imp = (i % 3 == 0) ? 2 : cenario.impacto;
            criados.add(criarChamadoAtivo(
                ctx, instanteNoDia(ctx, i % 3), cenario, StatusChamado.EM_ATENDIMENTO,
                ctx.analista(indicesAnalistasAtendimento[i]), sev, urg, imp, null, i + 20
            ));
        }

        // 5d) 9 AGUARDANDO (6 FORNECEDOR com escalonamento aberto nos 5 fornecedores + 3 SOLICITANTE em pausa)
        for (int i = 0; i < 6; i++) {
            CenarioCatalogo cenario = CENARIOS_CLINICOS.get(i % 8);
            criados.add(criarChamadoAtivo(
                ctx, instanteNoDia(ctx, 1 + (i % 4)), cenario, StatusChamado.AGUARDANDO,
                ctx.analista(i), cenario.severidade, cenario.urgencia, cenario.impacto,
                MotivoEspera.FORNECEDOR, i
            ));
        }
        for (int i = 0; i < 3; i++) {
            CenarioCatalogo cenario = CENARIOS_CLINICOS.get(8 + i);
            criados.add(criarChamadoAtivo(
                ctx, instanteNoDia(ctx, 1 + i), cenario, StatusChamado.AGUARDANDO,
                ctx.analista(i + 2), cenario.severidade, cenario.urgencia, cenario.impacto,
                MotivoEspera.SOLICITANTE, i + 30
            ));
        }

        return criados.size();
    }

    /**
     * Plano determinístico de 54 chamados encerrados no período atual:
     * - 12 P1 (ALTA): 9 dentro / 3 fora -> 75.0% (abaixoDaMeta = true para destaque visual no card de SLA)
     * - 24 P2 (MEDIA): 21 dentro / 3 fora -> 87.5%
     * - 18 P3 (BAIXA): 16 dentro / 2 fora -> 88.9%
     */
    private static List<PrioridadeAlvo> montarPlanoEncerradosPeriodoAtual() {
        List<PrioridadeAlvo> plano = new ArrayList<>(54);

        // 12 P1 (ALTA)
        for (int i = 0; i < 12; i++) {
            boolean dentroSla = i < 9; // 9/12 = 75%
            boolean comPausa = (i % 3 == 0);
            boolean divergencia = (i == 3 || i == 9);
            boolean comEscalonamento = (i == 4 || i == 9 || i == 10 || i == 11);
            boolean rastreavel = (i != 7); // 1 sem contato
            int imp = (i % 2 == 0) ? 3 : 2;
            plano.add(new PrioridadeAlvo(3, 3, imp, dentroSla, comPausa, divergencia, comEscalonamento, rastreavel));
        }

        // 24 P2 (MEDIA)
        for (int i = 0; i < 24; i++) {
            boolean dentroSla = i < 21; // 21/24 = 87.5%
            boolean comPausa = (i % 3 == 1);
            boolean divergencia = (i % 6 == 2);
            boolean comEscalonamento = (i % 3 == 2); // 8 escalonamentos
            boolean rastreavel = (i != 5 && i != 17); // 2 sem contato
            int urg = (i % 2 == 0) ? 2 : 3;
            plano.add(new PrioridadeAlvo(2, urg, 2, dentroSla, comPausa, divergencia, comEscalonamento, rastreavel));
        }

        // 18 P3 (BAIXA)
        for (int i = 0; i < 18; i++) {
            boolean dentroSla = i < 16; // 16/18 = 88.9%
            boolean comPausa = (i % 3 == 0);
            boolean divergencia = (i % 6 == 4);
            boolean comEscalonamento = (i == 5 || i == 11 || i == 17);
            boolean rastreavel = (i != 8 && i != 14); // 2 sem contato (total 5/54 -> M01 ~90.7%)
            int urg = (i % 2 == 0) ? 1 : 2;
            plano.add(new PrioridadeAlvo(1, urg, 1 + (i % 2), dentroSla, comPausa, divergencia, comEscalonamento, rastreavel));
        }

        return plano;
    }

    // -------------------------------------------------------------------------
    // Builders de ciclos de vida coerentes (chamados + contatos + eventos + escalonamentos)
    // -------------------------------------------------------------------------
    private Chamado criarChamadoEncerrado(
        ContextoSimulacao ctx,
        Instant abertoEm,
        CenarioCatalogo cenario,
        StatusChamado statusFinal,
        boolean dentroSla,
        boolean comPausaSolicitante,
        boolean divergenciaPrioridade,
        boolean comEscalonamento,
        boolean rastreavel,
        boolean periodoMaisLento
    ) {
        PrioridadeAlvo alvo = new PrioridadeAlvo(
            cenario.severidade, cenario.urgencia, cenario.impacto,
            dentroSla, comPausaSolicitante, divergenciaPrioridade, comEscalonamento, rastreavel
        );
        return criarChamadoEncerradoComMatriz(ctx, abertoEm, cenario, statusFinal, alvo, periodoMaisLento, ctx.rng.nextInt(5));
    }

    private Chamado criarChamadoEncerradoComMatriz(
        ContextoSimulacao ctx,
        Instant abertoEm,
        CenarioCatalogo cenario,
        StatusChamado statusFinal,
        PrioridadeAlvo alvo,
        boolean periodoMaisLento,
        int indiceFornecedor
    ) {
        Usuario criador = ctx.analistaAleatorio();
        Usuario responsavel = ctx.analistaAleatorio();
        Solicitante solicitante = ctx.solicitanteAleatorio();
        Unidade unidade = ctx.unidadePonderada();

        Prioridade sugerida = calculadoraMatriz3D.calcular(alvo.sev, alvo.urg, alvo.imp);
        Prioridade efetiva = alvo.divergencia ? alternarPrioridade(sugerida) : sugerida;
        String justificativa = alvo.divergencia
            ? JUSTIFICATIVAS_DIVERGENCIA.get(ctx.rng.nextInt(JUSTIFICATIVAS_DIVERGENCIA.size()))
            : null;

        long metaSegundos = metaSegundos(efetiva);
        long trAlvoSegundos = calcularTempoLiquidoSegundos(ctx.rng, metaSegundos, alvo.dentroSla, periodoMaisLento);
        long pausaSegundos = alvo.comPausa ? (5400L + ctx.rng.nextInt(7200)) : 0L; // 1h30 a 3h30 de pausa
        long tempoCorridoSegundos = trAlvoSegundos + pausaSegundos;

        Instant primeiraRespostaEm = abertoEm.plusSeconds(180 + ctx.rng.nextInt(960));
        Instant resolvidoEm = abertoEm.plusSeconds(tempoCorridoSegundos);
        if (resolvidoEm.isAfter(ctx.agora.minus(5, ChronoUnit.MINUTES))) {
            resolvidoEm = ctx.agora.minus(5, ChronoUnit.MINUTES);
        }
        Instant fechadoEm = statusFinal == StatusChamado.FECHADO
            ? resolvidoEm.plus(2, ChronoUnit.HOURS)
            : null;
        if (fechadoEm != null && fechadoEm.isAfter(ctx.agora)) {
            fechadoEm = ctx.agora.minus(1, ChronoUnit.MINUTES);
        }

        Chamado chamado = baseChamado(ctx, abertoEm, unidade, solicitante, criador, cenario);
        chamado.setResponsavel(responsavel);
        chamado.setStatus(statusFinal);
        chamado.aplicarTriagemPrioridade(
            sugerida, efetiva, justificativa,
            (short) alvo.sev, (short) alvo.urg, (short) alvo.imp
        );
        chamado.setPrimeiraRespostaEm(primeiraRespostaEm);
        chamado.setResolvidoEm(resolvidoEm);
        chamado.setFechadoEm(fechadoEm);
        chamado.setResolucao(cenario.resolucaoPadrao);
        chamado.setCausaRaiz(cenario.causaRaiz);
        chamado.setPossivelRecorrencia(cenario.recorrente);
        chamadoRepository.save(chamado);

        if (alvo.rastreavel) {
            RegistroContato contatoInicial = salvarContato(chamado, solicitante, criador, DirecaoContato.RECEBIDO,
                chamado.getDescricao(), abertoEm);
            registrarEventosAbertura(chamado, criador, abertoEm, contatoInicial);

            // Chamados mais ricos recebem retorno enviado ao solicitante e anotação técnica na timeline
            if (alvo.comPausa || alvo.comEscalonamento) {
                Instant contatoRetornoEm = primeiraRespostaEm.plusSeconds(120);
                if (contatoRetornoEm.isBefore(resolvidoEm)) {
                    salvarContato(chamado, solicitante, responsavel, DirecaoContato.ENVIADO,
                        "Diagnóstico inicial concluído pela equipe de TI; acompanhando estabilização junto à unidade.",
                        contatoRetornoEm);
                    salvarEvento(chamado, TipoEvento.ANOTACAO_ADICIONADA, contatoRetornoEm.plusSeconds(60), responsavel,
                        Map.of("resumo", "Logs coletados e validados com o coordenador local da unidade."));
                }
            }
        } else {
            // Propositalmente sem RegistroContato para M01 refletir conformidade realista (~90%)
            salvarEvento(chamado, TipoEvento.CHAMADO_ABERTO, abertoEm, criador,
                Map.of("canal", chamado.getCanalOrigem().name()));
        }

        salvarEvento(chamado, TipoEvento.PRIORIDADE_DEFINIDA, abertoEm.plusSeconds(150), criador,
            Map.of("prioridade_sugerida", sugerida.name(), "prioridade_final", efetiva.name()));
        salvarEvento(chamado, TipoEvento.CHAMADO_ATRIBUIDO, primeiraRespostaEm, responsavel,
            Map.of("responsavel_id", responsavel.getId().toString()));

        if (alvo.comPausa && pausaSegundos > 0) {
            Instant inicioPausa = abertoEm.plusSeconds(Math.max(600, trAlvoSegundos / 3));
            Instant fimPausa = inicioPausa.plusSeconds(pausaSegundos);
            if (fimPausa.isBefore(resolvidoEm)) {
                salvarEvento(chamado, TipoEvento.CHAMADO_AGUARDANDO, inicioPausa, responsavel,
                    Map.of("motivo_espera", MotivoEspera.SOLICITANTE.name()));
                // Evento informativo no meio da pausa valida que o LEAD() do DashboardRepository não quebra o relógio
                salvarEvento(chamado, TipoEvento.NOTIFICACAO_ENVIADA, inicioPausa.plusSeconds(pausaSegundos / 2),
                    responsavel, Map.of("canal", "EMAIL"));
                salvarEvento(chamado, TipoEvento.RETORNO_RECEBIDO, fimPausa, responsavel,
                    Map.of("status_anterior", StatusChamado.AGUARDANDO.name()));
            }
        }

        if (alvo.comEscalonamento) {
            FornecedorPerfil fornecedor = FORNECEDORES.get(Math.abs(indiceFornecedor) % FORNECEDORES.size());
            Instant enviadoEm = abertoEm.plusSeconds(900);
            Instant retornoEm = enviadoEm.plusSeconds(fornecedor.tempoMedioSegundos + ctx.rng.nextInt(7200));
            if (retornoEm.isAfter(resolvidoEm)) {
                retornoEm = resolvidoEm.minusSeconds(300);
            }
            if (!retornoEm.isBefore(enviadoEm)) {
                salvarEscalonamento(chamado, fornecedor.nome, responsavel, enviadoEm, retornoEm,
                    "Fornecedor " + fornecedor.nome + " concluiu intervenção técnica e validou estabilização com a unidade.");
                salvarEvento(chamado, TipoEvento.CHAMADO_ESCALADO, enviadoEm, responsavel,
                    Map.of("fornecedor", fornecedor.nome));
            }
        }

        salvarEvento(chamado, TipoEvento.CHAMADO_RESOLVIDO, resolvidoEm, responsavel,
            Map.of("resolucao", chamado.getResolucao()));

        if (statusFinal == StatusChamado.FECHADO && fechadoEm != null) {
            salvarEvento(chamado, TipoEvento.CHAMADO_FECHADO, fechadoEm, responsavel, Map.of());
        }

        return chamado;
    }

    private Chamado criarChamadoReaberto(ContextoSimulacao ctx, Instant abertoEm, CenarioCatalogo cenario) {
        Usuario criador = ctx.analistaAleatorio();
        Usuario responsavel = ctx.analistaAleatorio();
        Solicitante solicitante = ctx.solicitanteAleatorio();
        Unidade unidade = ctx.unidadePonderada();

        Prioridade sugerida = calculadoraMatriz3D.calcular(cenario.severidade, cenario.urgencia, cenario.impacto);
        Instant primeiraRespostaEm = abertoEm.plus(10, ChronoUnit.MINUTES);
        Instant resolvidoEm = abertoEm.plus(2 + ctx.rng.nextInt(4), ChronoUnit.HOURS);
        Instant reabertoEm = resolvidoEm.plus(12 + ctx.rng.nextInt(24), ChronoUnit.HOURS);
        if (reabertoEm.isAfter(ctx.agora)) {
            reabertoEm = ctx.agora.minus(30, ChronoUnit.MINUTES);
        }

        Chamado chamado = baseChamado(ctx, abertoEm, unidade, solicitante, criador, cenario);
        chamado.setResponsavel(responsavel);
        chamado.setStatus(StatusChamado.REABERTO);
        chamado.aplicarTriagemPrioridade(sugerida, sugerida, null,
            (short) cenario.severidade, (short) cenario.urgencia, (short) cenario.impacto);
        chamado.setPrimeiraRespostaEm(primeiraRespostaEm);
        chamado.setResolvidoEm(resolvidoEm);
        chamado.setResolucao(cenario.resolucaoPadrao);
        chamado.setCausaRaiz(cenario.causaRaiz);
        chamado.setPossivelRecorrencia(true);
        chamadoRepository.save(chamado);

        RegistroContato contato = salvarContato(chamado, solicitante, criador, DirecaoContato.RECEBIDO,
            chamado.getDescricao(), abertoEm);
        registrarEventosAbertura(chamado, criador, abertoEm, contato);
        salvarEvento(chamado, TipoEvento.CHAMADO_ATRIBUIDO, primeiraRespostaEm, responsavel,
            Map.of("responsavel_id", responsavel.getId().toString()));
        salvarEvento(chamado, TipoEvento.CHAMADO_RESOLVIDO, resolvidoEm, responsavel,
            Map.of("resolucao", chamado.getResolucao()));
        salvarEvento(chamado, TipoEvento.CHAMADO_REABERTO, reabertoEm, responsavel,
            Map.of("motivo", "Sintoma voltou a ocorrer na troca de turno da unidade."));
        salvarContato(chamado, solicitante, responsavel, DirecaoContato.RECEBIDO,
            "Incidente retornou após reinício do expediente; solicitada nova intervenção definitiva.", reabertoEm);

        return chamado;
    }

    private Chamado criarChamadoMesclado(ContextoSimulacao ctx, Instant abertoEm, Chamado principal) {
        Usuario criador = ctx.analistaAleatorio();
        Solicitante solicitante = ctx.solicitanteAleatorio();

        Chamado chamado = new Chamado();
        chamado.setCodigo(gerarCodigo(abertoEm));
        chamado.setUnidade(principal.getUnidade());
        chamado.setSolicitante(solicitante);
        chamado.setCanalOrigem(CANAIS[ctx.rng.nextInt(CANAIS.length)]);
        chamado.setDescricao("Relato duplicado da unidade sobre o mesmo incidente: " + principal.getDescricao());
        chamado.setCategoria(principal.getCategoria());
        chamado.setSistemaEquipamento(principal.getSistemaEquipamento());
        chamado.setCriadoPor(criador);
        chamado.setResponsavel(principal.getResponsavel());
        chamado.setStatus(StatusChamado.MESCLADO);
        chamado.setChamadoPrincipal(principal);
        chamado.setAbertoEm(abertoEm);
        chamado.setRegistroIniciadoEm(abertoEm.minusSeconds(25 + ctx.rng.nextInt(25)));
        chamado.setOrigemDado(ctx.origem);
        chamadoRepository.save(chamado);

        RegistroContato contato = salvarContato(chamado, solicitante, criador, DirecaoContato.RECEBIDO,
            chamado.getDescricao(), abertoEm);
        registrarEventosAbertura(chamado, criador, abertoEm, contato);
        salvarEvento(chamado, TipoEvento.CHAMADO_MESCLADO, abertoEm.plus(8, ChronoUnit.MINUTES), criador,
            Map.of("chamado_principal_id", principal.getId().toString()));

        return chamado;
    }

    private Chamado criarChamadoCancelado(ContextoSimulacao ctx, Instant abertoEm, String motivo) {
        Usuario criador = ctx.analistaAleatorio();
        Solicitante solicitante = ctx.solicitanteAleatorio();
        CenarioCatalogo cenario = CENARIOS_CLINICOS.get(10 + ctx.rng.nextInt(CENARIOS_CLINICOS.size() - 10));

        Chamado chamado = baseChamado(ctx, abertoEm, ctx.unidadePonderada(), solicitante, criador, cenario);
        chamado.setStatus(StatusChamado.CANCELADO);
        chamado.setMotivoCancelamento(motivo);
        chamadoRepository.save(chamado);

        RegistroContato contato = salvarContato(chamado, solicitante, criador, DirecaoContato.RECEBIDO,
            chamado.getDescricao(), abertoEm);
        registrarEventosAbertura(chamado, criador, abertoEm, contato);
        salvarEvento(chamado, TipoEvento.CHAMADO_CANCELADO, abertoEm.plus(14, ChronoUnit.MINUTES), criador,
            Map.of("motivo", motivo));

        return chamado;
    }

    private Chamado criarChamadoAtivo(
        ContextoSimulacao ctx,
        Instant abertoEm,
        CenarioCatalogo cenario,
        StatusChamado status,
        Usuario responsavel,
        int sev,
        int urg,
        int imp,
        MotivoEspera motivoEspera,
        int indiceFornecedor
    ) {
        Usuario criador = ctx.analistaAleatorio();
        Solicitante solicitante = ctx.solicitanteAleatorio();
        Unidade unidade = ctx.unidadePonderada();

        Chamado chamado = baseChamado(ctx, abertoEm, unidade, solicitante, criador, cenario);
        chamado.setStatus(status);
        chamado.setResponsavel(responsavel);

        if (status != StatusChamado.ABERTO || responsavel != null) {
            Prioridade sugerida = calculadoraMatriz3D.calcular(sev, urg, imp);
            chamado.aplicarTriagemPrioridade(sugerida, sugerida, null, (short) sev, (short) urg, (short) imp);
        }

        if (responsavel != null && status != StatusChamado.ABERTO) {
            chamado.setPrimeiraRespostaEm(abertoEm.plus(6 + ctx.rng.nextInt(10), ChronoUnit.MINUTES));
        }

        Instant esperaIniciada = null;
        if (status == StatusChamado.AGUARDANDO && motivoEspera != null) {
            esperaIniciada = abertoEm.plus(35, ChronoUnit.MINUTES);
            if (esperaIniciada.isAfter(ctx.agora)) {
                esperaIniciada = ctx.agora.minus(10, ChronoUnit.MINUTES);
            }
            chamado.setMotivoEspera(motivoEspera);
            chamado.setEsperaIniciadaEm(esperaIniciada);
        }

        chamadoRepository.save(chamado);

        RegistroContato contato = salvarContato(chamado, solicitante, criador, DirecaoContato.RECEBIDO,
            chamado.getDescricao(), abertoEm);
        registrarEventosAbertura(chamado, criador, abertoEm, contato);

        if (responsavel != null) {
            salvarEvento(chamado, TipoEvento.CHAMADO_ATRIBUIDO, abertoEm.plus(5, ChronoUnit.MINUTES), responsavel,
                Map.of("responsavel_id", responsavel.getId().toString()));
        }

        if (status == StatusChamado.AGUARDANDO && motivoEspera != null && esperaIniciada != null) {
            Usuario autorEvento = responsavel != null ? responsavel : criador;
            salvarEvento(chamado, TipoEvento.CHAMADO_AGUARDANDO, esperaIniciada, autorEvento,
                Map.of("motivo_espera", motivoEspera.name()));

            if (motivoEspera == MotivoEspera.FORNECEDOR) {
                FornecedorPerfil fornecedor = FORNECEDORES.get(Math.abs(indiceFornecedor) % FORNECEDORES.size());
                salvarEscalonamento(chamado, fornecedor.nome, autorEvento, esperaIniciada, null, null);
                salvarEvento(chamado, TipoEvento.CHAMADO_ESCALADO, esperaIniciada, autorEvento,
                    Map.of("fornecedor", fornecedor.nome));
            }
        }

        return chamado;
    }

    private Chamado baseChamado(
        ContextoSimulacao ctx,
        Instant abertoEm,
        Unidade unidade,
        Solicitante solicitante,
        Usuario criador,
        CenarioCatalogo cenario
    ) {
        String setor = SETORES_CLINICOS.get(ctx.rng.nextInt(SETORES_CLINICOS.size()));
        Chamado chamado = new Chamado();
        chamado.setCodigo(gerarCodigo(abertoEm));
        chamado.setUnidade(unidade);
        chamado.setSolicitante(solicitante);
        chamado.setCanalOrigem(CANAIS[ctx.rng.nextInt(CANAIS.length)]);
        chamado.setDescricao("[" + setor + "] " + cenario.descricao);
        chamado.setCategoria(ctx.categoriaPorNomeOuPrimeira(cenario.categoriaNome));
        chamado.setSistemaEquipamento(ctx.sistemaPorNomeOuPrimeiro(cenario.sistemaNome));
        chamado.setCriadoPor(criador);
        chamado.setAbertoEm(abertoEm);
        chamado.setRegistroIniciadoEm(abertoEm.minusSeconds(20 + ctx.rng.nextInt(38))); // M11 médio ~39s (< 60s)
        chamado.setOrigemDado(ctx.origem);
        return chamado;
    }

    private RegistroContato salvarContato(
        Chamado chamado,
        Solicitante solicitante,
        Usuario registrador,
        DirecaoContato direcao,
        String conteudo,
        Instant ocorridoEm
    ) {
        RegistroContato contato = new RegistroContato();
        contato.setChamado(chamado);
        contato.setSolicitante(solicitante);
        contato.setCanal(chamado.getCanalOrigem());
        contato.setDirecao(direcao);
        contato.setConteudo(conteudo);
        contato.setOcorridoEm(ocorridoEm);
        contato.setRegistradoPor(registrador);
        contato.setOrigemDado(chamado.getOrigemDado());
        return registroContatoRepository.save(contato);
    }

    private void registrarEventosAbertura(Chamado chamado, Usuario autor, Instant abertoEm, RegistroContato contato) {
        Map<String, Object> dadosAbertura = new LinkedHashMap<>();
        dadosAbertura.put("canal", chamado.getCanalOrigem().name());
        salvarEvento(chamado, TipoEvento.CHAMADO_ABERTO, abertoEm, autor, dadosAbertura);
        salvarEvento(chamado, TipoEvento.REGISTRO_DE_CONTATO_RECEBIDO, abertoEm.plusSeconds(1), autor,
            Map.of("registro_contato_id", contato.getId().toString()));
    }

    private void salvarEvento(Chamado chamado, TipoEvento tipo, Instant ocorridoEm, Usuario autor,
                              Map<String, Object> dados) {
        eventoHistoricoRepository.save(
            new EventoHistorico(chamado, tipo, ocorridoEm, autor, dados, chamado.getOrigemDado())
        );
    }

    private void salvarEscalonamento(
        Chamado chamado,
        String fornecedor,
        Usuario responsavel,
        Instant enviadoEm,
        Instant retornoEm,
        String retorno
    ) {
        Escalonamento esc = new Escalonamento();
        esc.setChamado(chamado);
        esc.setFornecedor(fornecedor);
        esc.setProtocoloExterno("EXT-" + Math.abs((chamado.getCodigo() + fornecedor).hashCode() % 900000 + 100000));
        esc.setObservacao("Escalonado para atuação técnica especializada na unidade " + chamado.getUnidade().getIdentificacao());
        esc.setResponsavel(responsavel);
        esc.setEnviadoPor(responsavel);
        esc.setEnviadoEm(enviadoEm);
        if (retornoEm != null) {
            esc.setRetornoEm(retornoEm);
            esc.setRetorno(retorno);
            esc.setRetornoRegistradoPor(responsavel);
        }
        esc.setOrigemDado(chamado.getOrigemDado());
        escalonamentoRepository.save(esc);
    }

    // -------------------------------------------------------------------------
    // Atores e unidades idempotentes (porte completo da Rede Lúmen: 14 unidades, 7 analistas, 30 solicitantes)
    // -------------------------------------------------------------------------
    private List<Unidade> garantirUnidadesSimulacao() {
        List<UnidadeSeed> extras = List.of(
            new UnidadeSeed("U.01 Derby",                  "LUM-DRB", "Praça do Derby, 210",             true),
            new UnidadeSeed("U.02 Graças",                 "LUM-GRC", "Av. Rui Barbosa, 890",            false),
            new UnidadeSeed("U.04 Espinheiro",             "LUM-ESP", "Rua da Hora, 420",                false),
            new UnidadeSeed("U.05 Olinda",                 "LUM-OLI", "Av. Getúlio Vargas, 1120",        false),
            new UnidadeSeed("U.06 Casa Forte",             "LUM-CFT", "Av. Dezessete de Agosto, 650",    false),
            new UnidadeSeed("U.08 Pina",                   "LUM-PIN", "Av. Herculano Bandeira, 310",     true),
            new UnidadeSeed("U.09 Madalena",               "LUM-MAD", "Rua Real da Torre, 780",          false),
            new UnidadeSeed("U.10 Paulista",               "LUM-PAU", "Av. Marechal Floriano, 145",      false),
            new UnidadeSeed("U.12 Cabo de Santo Agostinho","LUM-CAB", "Av. Historiador Pereira, 505",    false)
        );

        for (UnidadeSeed item : extras) {
            if (!unidadeRepository.existsByIdentificacao(item.identificacao)) {
                Unidade u = new Unidade();
                u.setNome(item.nome);
                u.setIdentificacao(item.identificacao);
                u.setLocalizacao(item.localizacao);
                u.setAtendimento24h(item.atendimento24h);
                u.setAtivo(true);
                unidadeRepository.save(u);
            }
        }
        return unidadeRepository.findByAtivoOrderByNomeAsc(true);
    }

    private List<Usuario> garantirAnalistasSimulacao() {
        List<String[]> extras = List.of(
            new String[]{"Camila Ferreira",  "camila.ferreira@lumen.local"},
            new String[]{"Rafael Monteiro",  "rafael.monteiro@lumen.local"},
            new String[]{"Lucas Barbosa",    "lucas.barbosa@lumen.local"},
            new String[]{"Mariana Alencar",  "mariana.alencar@lumen.local"},
            new String[]{"Thiago Santana",   "thiago.santana@lumen.local"}
        );

        for (String[] dados : extras) {
            if (!usuarioRepository.existsByEmail(dados[1])) {
                Usuario analista = new Usuario();
                analista.setNome(dados[0]);
                analista.setEmail(dados[1]);
                analista.setSenhaHash(SENHA_DEV_HASH);
                analista.setPerfil(PerfilUsuario.ANALISTA_SUPORTE);
                analista.setAtivo(true);
                usuarioRepository.save(analista);
            }
        }
        return usuarioRepository.findByPerfilAndAtivoOrderByNomeAsc(PerfilUsuario.ANALISTA_SUPORTE, true);
    }

    private List<Solicitante> garantirSolicitantesSimulacao() {
        List<String[]> catalogo = List.of(
            new String[]{"Ana Paula Ribeiro",      "+5581991010001", "ana.ribeiro@lumen.local"},
            new String[]{"Bruno Henrique Costa",   "+5581991010002", "bruno.costa@lumen.local"},
            new String[]{"Carla Mendes Silva",     "+5581991010003", "carla.mendes@lumen.local"},
            new String[]{"Diego Almeida Lima",     "+5581991010004", "diego.almeida@lumen.local"},
            new String[]{"Eduarda Nogueira",       "+5581991010005", "eduarda.nogueira@lumen.local"},
            new String[]{"Fernanda Cavalcanti",    "+5581991010006", "fernanda.cavalcanti@lumen.local"},
            new String[]{"Gabriel Azevedo",        "+5581991010007", "gabriel.azevedo@lumen.local"},
            new String[]{"Helena Vasconcelos",     "+5581991010008", "helena.vasconcelos@lumen.local"},
            new String[]{"Igor Batista",           "+5581991010009", "igor.batista@lumen.local"},
            new String[]{"Juliana Peixoto",        "+5581991010010", "juliana.peixoto@lumen.local"},
            new String[]{"Karina Tavares",         "+5581991010011", "karina.tavares@lumen.local"},
            new String[]{"Marcelo Queiroz",        "+5581991010012", "marcelo.queiroz@lumen.local"},
            new String[]{"Natália Bezerra",        "+5581991010013", "natalia.bezerra@lumen.local"},
            new String[]{"Otávio Lira",            "+5581991010014", "otavio.lira@lumen.local"},
            new String[]{"Patrícia Amorim",        "+5581991010015", "patricia.amorim@lumen.local"},
            new String[]{"Renato Albuquerque",     "+5581991010016", "renato.albuquerque@lumen.local"},
            new String[]{"Sabrina Wanderley",      "+5581991010017", "sabrina.wanderley@lumen.local"},
            new String[]{"Tatiana Medeiros",       "+5581991010018", "tatiana.medeiros@lumen.local"},
            new String[]{"Vinícius Cordeiro",      "+5581991010019", "vinicius.cordeiro@lumen.local"},
            new String[]{"Yasmim Arruda",          "+5581991010020", "yasmim.arruda@lumen.local"},
            new String[]{"André Luiz Farias",      "+5581991010021", "andre.farias@lumen.local"},
            new String[]{"Beatriz Magalhães",      "+5581991010022", "beatriz.magalhaes@lumen.local"},
            new String[]{"Cássio Oliveira",        "+5581991010023", "cassio.oliveira@lumen.local"},
            new String[]{"Daniela Fonseca",        "+5581991010024", "daniela.fonseca@lumen.local"},
            new String[]{"Eduardo Paiva",          "+5581991010025", "eduardo.paiva@lumen.local"},
            new String[]{"Flávia Siqueira",        "+5581991010026", "flavia.siqueira@lumen.local"},
            new String[]{"Gustavo Rocha",          "+5581991010027", "gustavo.rocha@lumen.local"},
            new String[]{"Isabela Carneiro",       "+5581991010028", "isabela.carneiro@lumen.local"},
            new String[]{"João Pedro Moura",       "+5581991010029", "joao.moura@lumen.local"},
            new String[]{"Larissa Duarte",         "+5581991010030", "larissa.duarte@lumen.local"}
        );

        List<Solicitante> resultado = new ArrayList<>();
        for (String[] item : catalogo) {
            Solicitante s = solicitanteRepository.findByEmail(item[2]).orElseGet(() -> {
                Solicitante novo = new Solicitante();
                novo.setNome(item[0]);
                novo.setTelefone(item[1]);
                novo.setEmail(item[2]);
                return solicitanteRepository.save(novo);
            });
            resultado.add(s);
        }
        return resultado;
    }

    // -------------------------------------------------------------------------
    // Helpers determinísticos de tempo, prioridade, cenário e código
    // -------------------------------------------------------------------------
    private String gerarCodigo(Instant abertoEm) {
        int ano = abertoEm.atZone(FUSO_DO_CODIGO).getYear();
        return "LUM-%d-%06d".formatted(ano, chamadoRepository.proximoNumeroCodigo());
    }

    private static Instant instanteNoDia(ContextoSimulacao ctx, int diasAtras) {
        Instant base = ctx.agora.minus(diasAtras, ChronoUnit.DAYS)
            .truncatedTo(ChronoUnit.HOURS)
            .minus(ctx.rng.nextInt(4), ChronoUnit.HOURS)
            .plus(ctx.rng.nextInt(55), ChronoUnit.MINUTES);
        if (base.isAfter(ctx.agora.minus(15, ChronoUnit.MINUTES))) {
            return ctx.agora.minus(15 + ctx.rng.nextInt(45), ChronoUnit.MINUTES);
        }
        return base;
    }

    /** Pondera para que os 8 cenários recorrentes formem um ranking natural (curva de Pareto) no Top 10. */
    private static CenarioCatalogo escolherCenarioPonderado(Random rng, int indice) {
        int sorteio = rng.nextInt(100);
        if (sorteio < 18) return CENARIOS_CLINICOS.get(0); // Impressora térmica (top 1)
        if (sorteio < 33) return CENARIOS_CLINICOS.get(1); // Link VPN MPLS (top 2)
        if (sorteio < 45) return CENARIOS_CLINICOS.get(2); // Portal de resultados / LIS (top 3)
        if (sorteio < 55) return CENARIOS_CLINICOS.get(3); // Credenciais AD (top 4)
        if (sorteio < 63) return CENARIOS_CLINICOS.get(4); // Sincronização PACS/DICOM (top 5)
        if (sorteio < 70) return CENARIOS_CLINICOS.get(5); // Leitor código de barras (top 6)
        if (sorteio < 76) return CENARIOS_CLINICOS.get(6); // Token certificado digital (top 7)
        if (sorteio < 81) return CENARIOS_CLINICOS.get(7); // Wi-Fi corporativo / VLAN (top 8)
        // Demais 7 cenários operacionais não recorrentes
        return CENARIOS_CLINICOS.get(8 + (indice % (CENARIOS_CLINICOS.size() - 8)));
    }

    private static long metaSegundos(Prioridade prioridade) {
        return switch (prioridade) {
            case ALTA  -> 7200L;   // 2h
            case MEDIA -> 28800L;  // 8h
            case BAIXA -> 86400L;  // 24h
        };
    }

    private static long calcularTempoLiquidoSegundos(Random rng, long metaSegundos, boolean dentroSla, boolean maisLento) {
        double fatorBase = dentroSla
            ? (maisLento ? 0.55 + rng.nextDouble() * 0.35 : 0.28 + rng.nextDouble() * 0.48)
            : (1.18 + rng.nextDouble() * 0.70);
        return Math.max(900L, Math.round(metaSegundos * fatorBase));
    }

    private static Prioridade alternarPrioridade(Prioridade sugerida) {
        return switch (sugerida) {
            case BAIXA -> Prioridade.MEDIA;
            case MEDIA -> Prioridade.ALTA;
            case ALTA  -> Prioridade.MEDIA;
        };
    }

    // -------------------------------------------------------------------------
    // Catálogo realista expandido (15 cenários clínicos, 5 fornecedores, setores e motivos)
    // -------------------------------------------------------------------------
    private static final CanalOrigem[] CANAIS = {
        CanalOrigem.LIGACAO, CanalOrigem.WHATSAPP, CanalOrigem.EMAIL, CanalOrigem.PRESENCIAL
    };

    private static final List<String> SETORES_CLINICOS = List.of(
        "Recepção", "Posto de Coleta", "Sala de Laudos", "Triagem",
        "Tomografia", "Ultrassonografia", "Laboratório Central", "Faturamento", "Atendimento 24h"
    );

    private static final List<String> JUSTIFICATIVAS_DIVERGENCIA = List.of(
        "Ajuste validado na triagem devido ao impacto direto na agenda de exames em jejum da unidade.",
        "Prioridade elevada a pedido da coordenação clínica por fila acumulada na recepção.",
        "Reclassificado após confirmar existência de estação reserva operando normalmente no setor.",
        "Elevado para alta prioridade por afetar liberação de laudos de urgência 24h."
    );

    private static final List<String> MOTIVOS_CANCELAMENTO = List.of(
        "Solicitante informou que o equipamento voltou a operar normalmente após reinício local.",
        "Requisição fora do escopo da TI (manutenção predial redirecionada para Facilities).",
        "Chamado aberto em duplicidade por telefone antes da triagem inicial.",
        "Usuário abriu relato de teste durante treinamento da equipe de recepção.",
        "Troca de insumo (ribbon/papel) realizada diretamente pela enfermagem sem intervenção técnica."
    );

    private static final List<FornecedorPerfil> FORNECEDORES = List.of(
        new FornecedorPerfil("Vivo Empresas", 79200L), // ~22h (principal gargalo em M05)
        new FornecedorPerfil("GE HealthCare", 57600L), // ~16h (equipamentos de imagem)
        new FornecedorPerfil("Philips Saúde", 39600L), // ~11h (PACS / RIS)
        new FornecedorPerfil("TOTVS Saúde",   25200L), // ~7h  (sistema hospitalar)
        new FornecedorPerfil("Simpress",      12600L)  // ~3h30 (parque de impressoras)
    );

    private static final List<CenarioCatalogo> CENARIOS_CLINICOS = List.of(
        // 8 cenários recorrentes (populam Top Problemas Recorrentes com ranking natural)
        new CenarioCatalogo(
            "Impressora térmica de etiquetas travou com fila presa e não imprime códigos de barras de amostras.",
            "Equipamento de apoio", "Impressora de etiquetas",
            "Spooler reiniciado, driver atualizado e cabeçote calibrado com teste de impressão validado.",
            "Falha no spooler da impressora térmica de etiquetas", true, 3, 2, 3
        ),
        new CenarioCatalogo(
            "Queda intermitente da conexão VPN impedindo envio de laudos e acesso ao sistema interno.",
            "Infraestrutura / Rede", "Rede / Wi-Fi",
            "Failover de link acionado e túnel IPsec restabelecido junto à operadora.",
            "Instabilidade intermitente no link VPN MPLS", true, 3, 3, 3
        ),
        new CenarioCatalogo(
            "Portal de resultados apresentando timeout ao liberar exames laboratoriais em horário de pico.",
            "Sistema hospitalar", "Portal de resultados",
            "Pool de conexões reiniciado e fila de integração LIS normalizada.",
            "Timeout na integração do Portal de Resultados (LIS)", true, 3, 3, 2
        ),
        new CenarioCatalogo(
            "Bloqueio recorrente de credenciais de rede no guichê durante troca de turno dos operadores.",
            "Acesso e senhas", "Computador da recepção",
            "Conta desbloqueada no AD e política de cache de sessão ajustada na estação.",
            "Bloqueio recorrente de credenciais AD na recepção", true, 2, 2, 2
        ),
        new CenarioCatalogo(
            "Atraso na sincronização de imagens DICOM entre o tomógrafo e o servidor PACS central.",
            "Sistema hospitalar", "Portal de resultados",
            "Serviço DICOM C-STORE reiniciado e fila de retransmissão de estudos concluída.",
            "Atraso na sincronização DICOM/PACS", true, 3, 2, 3
        ),
        new CenarioCatalogo(
            "Leitor de código de barras da bancada de coleta perdeu pareamento e não lê etiquetas dos tubos.",
            "Equipamento de apoio", "Impressora de etiquetas",
            "Firmware reconfigurado para modo USB HID e leitura de amostras validada em bancada.",
            "Desconfiguração de leitor de código de barras na coleta", true, 2, 3, 2
        ),
        new CenarioCatalogo(
            "Certificado digital A3 do médico radiologista não reconhecido para assinatura eletrônica de laudos.",
            "Acesso e senhas", "Computador da recepção",
            "Cadeia de certificação ICP-Brasil reinstalada e middleware do token reiniciado.",
            "Falha no reconhecimento de token/certificado digital A3", true, 2, 3, 2
        ),
        new CenarioCatalogo(
            "Oscilação de Wi-Fi corporativo desconectando tablets de check-in de pacientes no corredor.",
            "Infraestrutura / Rede", "Rede / Wi-Fi",
            "Access point reiniciado, canal ajustado na controladora e roaming validado.",
            "Oscilação de sinal Wi-Fi nos coletores/tablets", true, 2, 2, 2
        ),

        // 7 cenários operacionais variados (não recorrentes)
        new CenarioCatalogo(
            "Sistema de agendamento com lentidão na busca de pacientes por CPF e convênio.",
            "Sistema hospitalar", "Sistema de agendamento",
            "Cache local limpo e sessão do cliente reautenticada sem perda de agendamentos.",
            null, false, 2, 2, 2
        ),
        new CenarioCatalogo(
            "Ramal telefônico do balcão sem sinal para transferência de ligações entre unidades.",
            "Telefonia e comunicação", "Telefone / ramal",
            "Aparelho IP reprovisionado na VLAN de voz e testes de chamada confirmados.",
            null, false, 1, 2, 2
        ),
        new CenarioCatalogo(
            "Impressora comum da sala administrativa apresentando atolamento e erro de comunicação na rede.",
            "Equipamento de apoio", "Impressora comum",
            "IP fixo reassociado na fila de impressão e sensor de bandeja calibrado.",
            null, false, 1, 1, 2
        ),
        new CenarioCatalogo(
            "Solicitação de novo acesso ao sistema de agendamento para colaboradora recém-admitida.",
            "Acesso e senhas", "Sistema de agendamento",
            "Perfil provisionado conforme matriz RBAC e primeiro login validado com a usuária.",
            null, false, 1, 2, 1
        ),
        new CenarioCatalogo(
            "Estação de atendimento travando na abertura de guias TISS em PDF no navegador.",
            "Equipamento de apoio", "Computador da recepção",
            "Extensão conflitante removida, navegador atualizado e memória verificada.",
            null, false, 2, 2, 1
        ),
        new CenarioCatalogo(
            "Intermitência no recebimento de ligações externas da central 0800 na fila da unidade.",
            "Telefonia e comunicação", "Telefone / ramal",
            "Rota SIP ajustada no PABX em nuvem e chamadas de entrada testadas com sucesso.",
            null, false, 2, 3, 2
        ),
        new CenarioCatalogo(
            "Dúvida operacional e ajuste de permissões de pasta compartilhada para planilhas de faturamento.",
            "Outro", "Outro",
            "Permissões NTFS revisadas conforme aprovação do gestor e mapeamento de rede recriado.",
            null, false, 1, 1, 1
        )
    );

    private record UnidadeSeed(String nome, String identificacao, String localizacao, boolean atendimento24h) {}

    private record FornecedorPerfil(String nome, long tempoMedioSegundos) {}

    private record CenarioCatalogo(
        String descricao,
        String categoriaNome,
        String sistemaNome,
        String resolucaoPadrao,
        String causaRaiz,
        boolean recorrente,
        int severidade,
        int urgencia,
        int impacto
    ) {}

    private record PrioridadeAlvo(
        int sev,
        int urg,
        int imp,
        boolean dentroSla,
        boolean comPausa,
        boolean divergencia,
        boolean comEscalonamento,
        boolean rastreavel
    ) {}

    private record ContextoSimulacao(
        Instant agora,
        Random rng,
        OrigemDado origem,
        List<Unidade> unidades,
        List<Categoria> categorias,
        List<SistemaEquipamento> sistemas,
        List<Usuario> analistas,
        List<Solicitante> solicitantes
    ) {
        /** Unidades 24h e grandes polos recebem mais chamados para gerar distribuição realista no gráfico por unidade. */
        Unidade unidadePonderada() {
            if (unidades.size() >= 5 && rng.nextInt(100) < 55) {
                List<Unidade> altoFluxo = unidades.stream()
                    .filter(u -> u.isAtendimento24h()
                        || "LUM-BVG".equals(u.getIdentificacao())
                        || "LUM-BVT".equals(u.getIdentificacao()))
                    .toList();
                if (!altoFluxo.isEmpty()) {
                    return altoFluxo.get(rng.nextInt(altoFluxo.size()));
                }
            }
            return unidades.get(rng.nextInt(unidades.size()));
        }

        Usuario analistaAleatorio() {
            return analistas.get(rng.nextInt(analistas.size()));
        }

        Usuario analista(int indice) {
            return analistas.get(indice % analistas.size());
        }

        Solicitante solicitanteAleatorio() {
            return solicitantes.get(rng.nextInt(solicitantes.size()));
        }

        Categoria categoriaPorNomeOuPrimeira(String nome) {
            return categorias.stream()
                .filter(c -> c.getNome().equalsIgnoreCase(nome))
                .findFirst()
                .orElse(categorias.getFirst());
        }

        SistemaEquipamento sistemaPorNomeOuPrimeiro(String nome) {
            return sistemas.stream()
                .filter(s -> s.getNome().equalsIgnoreCase(nome))
                .findFirst()
                .orElse(sistemas.getFirst());
        }
    }
}
