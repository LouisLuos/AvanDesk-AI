package com.avandesk.api.dashboard.repository;

import com.avandesk.api.chamado.entity.OrigemDado;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.ChamadosPorResponsavelDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.ChamadosPorUnidadeDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.ProblemaRecorrenteDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.SlaPorPrioridadeDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.TempoTerceirosFornecedorDto;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse.VolumeItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DashboardRepository {

    private static final double META_INICIAL_SLA_PERCENTUAL = 80.0;
    private static final String SEPARADOR_UNIDADES = ";;";
    /**
     * Apenas eventos que alteram efetivamente o estado do relógio de SLA entram na janela do LEAD().
     * Eventos informativos (ANEXO_VISUALIZADO, NOTIFICACAO_ENVIADA, ANOTACAO_ADICIONADA, contatos)
     * não podem encerrar prematuramente uma pausa por SOLICITANTE.
     */
    private static final String TIPOS_EVENTO_RELOGIO_SLA =
        "'CHAMADO_AGUARDANDO', 'ATENDIMENTO_RETOMADO', 'RETORNO_RECEBIDO', "
        + "'CHAMADO_ESCALADO', 'CHAMADO_RESOLVIDO', 'CHAMADO_REABERTO', "
        + "'CHAMADO_FECHADO', 'CHAMADO_CANCELADO', 'CHAMADO_MESCLADO'";

    private final JdbcClient jdbcClient;

    /**
     * Projeta os KPIs de volume, mediana/média de resolução, comparativo com o período anterior,
     * rastreabilidade (M01), conformidade SLA (M04) e métricas da Seção 9.7 (TT, TPR, TR, M08, M11, M12).
     */
    public ResumoIndicadoresRow buscarResumoIndicadores(
        Instant inicio,
        Instant fim,
        Instant inicioAnterior,
        Instant fimAnterior,
        UUID unidadeId,
        OrigemDado origemDado
    ) {
        String filtroUnidadeChamado = unidadeId != null ? " AND c.unidade_id = :unidadeId " : "";
        String filtroUnidadeSub = unidadeId != null ? " AND ch.unidade_id = :unidadeId " : "";

        String sql = """
            WITH chamados_janela AS (
                SELECT
                    c.id,
                    c.unidade_id,
                    c.responsavel_id,
                    c.status,
                    c.descricao,
                    c.resolucao,
                    c.chamado_principal_id,
                    COALESCE(c.prioridade_final, c.prioridade_sugerida) AS prioridade_efetiva,
                    c.prioridade_sugerida,
                    c.prioridade_final,
                    c.aberto_em,
                    c.registro_iniciado_em,
                    c.primeira_resposta_em,
                    c.resolvido_em,
                    (c.aberto_em >= :inicio AND c.aberto_em <= :fim) AS no_periodo_atual
                FROM chamados c
                WHERE c.origem_dado = :origemDado
                  AND c.aberto_em >= :inicioAnterior
                  AND c.aberto_em <= :fim
                  %s
            ),
            pausas_solicitante AS (
                SELECT
                    ev.chamado_id,
                    SUM(
                        GREATEST(
                            EXTRACT(EPOCH FROM (COALESCE(ev.proximo_evento_em, ev.resolvido_em, NOW()) - ev.ocorrido_em)),
                            0.0
                        )
                    ) AS segundos_pausados
                FROM (
                    SELECT
                        e.chamado_id,
                        e.tipo,
                        e.dados->>'motivo_espera' AS motivo_espera,
                        e.ocorrido_em,
                        cj.resolvido_em,
                        LEAD(e.ocorrido_em) OVER (PARTITION BY e.chamado_id ORDER BY e.ocorrido_em ASC) AS proximo_evento_em
                    FROM eventos_historico e
                    JOIN chamados_janela cj ON cj.id = e.chamado_id
                    WHERE e.origem_dado = :origemDado
                      AND e.tipo IN (%s)
                ) ev
                WHERE ev.tipo = 'CHAMADO_AGUARDANDO'
                  AND ev.motivo_espera = 'SOLICITANTE'
                GROUP BY ev.chamado_id
            ),
            metricas_por_chamado AS (
                SELECT
                    cj.*,
                    CASE
                        WHEN cj.resolvido_em IS NOT NULL THEN
                            GREATEST(
                                EXTRACT(EPOCH FROM (cj.resolvido_em - cj.aberto_em)) - COALESCE(p.segundos_pausados, 0.0),
                                0.0
                            )
                        ELSE NULL
                    END AS tr_segundos,
                    CASE
                        WHEN cj.resolvido_em IS NOT NULL AND cj.prioridade_efetiva IS NOT NULL THEN
                            GREATEST(
                                EXTRACT(EPOCH FROM (cj.resolvido_em - cj.aberto_em)) - COALESCE(p.segundos_pausados, 0.0),
                                0.0
                            ) <= CASE cj.prioridade_efetiva
                                WHEN 'ALTA'  THEN 7200.0
                                WHEN 'MEDIA' THEN 28800.0
                                WHEN 'BAIXA' THEN 86400.0
                            END
                        ELSE NULL
                    END AS dentro_sla,
                    CASE
                        WHEN cj.status IN ('RESOLVIDO', 'FECHADO') THEN (
                            cj.unidade_id IS NOT NULL
                            AND cj.responsavel_id IS NOT NULL
                            AND LENGTH(TRIM(cj.descricao)) >= 10
                            AND NULLIF(TRIM(cj.resolucao), '') IS NOT NULL
                            AND EXISTS (SELECT 1 FROM registros_contato rc WHERE rc.chamado_id = cj.id)
                            AND EXISTS (
                                SELECT 1 FROM eventos_historico eh
                                WHERE eh.chamado_id = cj.id
                                  AND eh.tipo = 'CHAMADO_RESOLVIDO'
                            )
                        )
                        ELSE NULL
                    END AS rastreavel
                FROM chamados_janela cj
                LEFT JOIN pausas_solicitante p ON p.chamado_id = cj.id
            )
            SELECT
                COUNT(id) FILTER (WHERE no_periodo_atual) AS total_atual,
                COUNT(id) FILTER (WHERE NOT no_periodo_atual AND aberto_em < :fimAnterior) AS total_anterior,

                COUNT(id) FILTER (WHERE no_periodo_atual AND tr_segundos IS NOT NULL) AS resolvidos_atual,
                COUNT(id) FILTER (WHERE NOT no_periodo_atual AND aberto_em < :fimAnterior AND tr_segundos IS NOT NULL) AS resolvidos_anterior,
                COALESCE(
                    PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY tr_segundos)
                        FILTER (WHERE no_periodo_atual AND tr_segundos IS NOT NULL),
                    0.0
                ) AS tr_mediano_atual_segundos,
                COALESCE(
                    PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY tr_segundos)
                        FILTER (WHERE NOT no_periodo_atual AND aberto_em < :fimAnterior AND tr_segundos IS NOT NULL),
                    0.0
                ) AS tr_mediano_anterior_segundos,
                COALESCE(
                    AVG(tr_segundos) FILTER (WHERE no_periodo_atual AND tr_segundos IS NOT NULL),
                    0.0
                ) AS tr_medio_atual_segundos,
                COALESCE(
                    AVG(tr_segundos) FILTER (WHERE NOT no_periodo_atual AND aberto_em < :fimAnterior AND tr_segundos IS NOT NULL),
                    0.0
                ) AS tr_medio_anterior_segundos,
                COALESCE(
                    PERCENTILE_CONT(0.90) WITHIN GROUP (ORDER BY tr_segundos)
                        FILTER (WHERE no_periodo_atual AND tr_segundos IS NOT NULL),
                    0.0
                ) AS tr_p90_atual_segundos,

                COUNT(id) FILTER (WHERE no_periodo_atual AND rastreavel IS NOT NULL) AS rastreaveis_avaliados,
                COUNT(id) FILTER (WHERE no_periodo_atual AND rastreavel IS TRUE) AS rastreaveis_conformes,

                COUNT(id) FILTER (WHERE no_periodo_atual AND dentro_sla IS NOT NULL) AS sla_avaliados,
                COUNT(id) FILTER (WHERE no_periodo_atual AND dentro_sla IS TRUE) AS sla_conformes,

                COALESCE(
                    AVG(EXTRACT(EPOCH FROM (resolvido_em - aberto_em)))
                        FILTER (WHERE no_periodo_atual AND resolvido_em IS NOT NULL),
                    0.0
                ) AS tt_medio_segundos,
                COALESCE(
                    AVG(EXTRACT(EPOCH FROM (primeira_resposta_em - aberto_em)))
                        FILTER (WHERE no_periodo_atual AND primeira_resposta_em IS NOT NULL),
                    0.0
                ) AS tpr_medio_segundos,
                COALESCE(
                    (COUNT(id) FILTER (
                        WHERE no_periodo_atual
                          AND (
                              status = 'REABERTO'
                              OR EXISTS (
                                  SELECT 1 FROM eventos_historico eh
                                  WHERE eh.chamado_id = metricas_por_chamado.id
                                    AND eh.tipo = 'CHAMADO_REABERTO'
                              )
                          )
                    ) * 100.0)
                    / NULLIF(COUNT(id) FILTER (
                        WHERE no_periodo_atual
                          AND (status IN ('RESOLVIDO', 'FECHADO', 'REABERTO') OR resolvido_em IS NOT NULL)
                    ), 0),
                    0.0
                ) AS m06_taxa_reabertura,
                COUNT(id) FILTER (
                    WHERE no_periodo_atual
                      AND (status = 'MESCLADO' OR chamado_principal_id IS NOT NULL)
                ) AS m07_total_duplicados,
                COALESCE(
                    (COUNT(id) FILTER (
                        WHERE no_periodo_atual
                          AND (status = 'MESCLADO' OR chamado_principal_id IS NOT NULL)
                    ) * 100.0)
                    / NULLIF(COUNT(id) FILTER (WHERE no_periodo_atual), 0),
                    0.0
                ) AS m07_taxa_duplicidade,
                COALESCE(
                    AVG(EXTRACT(EPOCH FROM (aberto_em - registro_iniciado_em)))
                        FILTER (WHERE no_periodo_atual AND registro_iniciado_em IS NOT NULL),
                    0.0
                ) AS m11_medio_segundos,
                COALESCE(
                    (COUNT(id) FILTER (
                        WHERE no_periodo_atual
                          AND prioridade_sugerida IS NOT NULL
                          AND prioridade_final IS NOT NULL
                          AND prioridade_final <> prioridade_sugerida
                    ) * 100.0)
                    / NULLIF(COUNT(id) FILTER (WHERE no_periodo_atual AND prioridade_sugerida IS NOT NULL), 0),
                    0.0
                ) AS m08_percentual_divergencia,
                (
                    SELECT COUNT(eh.id)
                    FROM eventos_historico eh
                    JOIN chamados ch ON ch.id = eh.chamado_id
                    LEFT JOIN usuarios u ON u.id = eh.autor_id
                    WHERE eh.tipo = 'ANEXO_VISUALIZADO'
                      AND eh.origem_dado = :origemDado
                      AND (
                          u.perfil = 'LIDERANCA_TI'
                          OR LOWER(COALESCE(eh.dados->>'violacao_privacidade', 'false')) = 'true'
                      )
                      AND ch.aberto_em >= :inicio
                      AND ch.aberto_em <= :fim
                      %s
                ) AS m12_incidentes_privacidade
            FROM metricas_por_chamado
            """.formatted(filtroUnidadeChamado, TIPOS_EVENTO_RELOGIO_SLA, filtroUnidadeSub);

        var spec = jdbcClient.sql(sql)
            .param("inicio", Timestamp.from(inicio))
            .param("fim", Timestamp.from(fim))
            .param("inicioAnterior", Timestamp.from(inicioAnterior))
            .param("fimAnterior", Timestamp.from(fimAnterior))
            .param("origemDado", origemDado.name());

        if (unidadeId != null) {
            spec = spec.param("unidadeId", unidadeId);
        }

        return spec.query((rs, rowNum) -> new ResumoIndicadoresRow(
            rs.getLong("total_atual"),
            rs.getLong("total_anterior"),
            rs.getLong("resolvidos_atual"),
            rs.getLong("resolvidos_anterior"),
            rs.getDouble("tr_mediano_atual_segundos"),
            rs.getDouble("tr_mediano_anterior_segundos"),
            rs.getDouble("tr_medio_atual_segundos"),
            rs.getDouble("tr_medio_anterior_segundos"),
            rs.getDouble("tr_p90_atual_segundos"),
            rs.getLong("rastreaveis_avaliados"),
            rs.getLong("rastreaveis_conformes"),
            rs.getLong("sla_avaliados"),
            rs.getLong("sla_conformes"),
            rs.getDouble("tt_medio_segundos"),
            rs.getDouble("tpr_medio_segundos"),
            rs.getDouble("m06_taxa_reabertura"),
            rs.getLong("m07_total_duplicados"),
            rs.getDouble("m07_taxa_duplicidade"),
            rs.getDouble("m11_medio_segundos"),
            rs.getDouble("m08_percentual_divergencia"),
            rs.getLong("m12_incidentes_privacidade")
        )).single();
    }

    /**
     * Gráfico horizontal ("Chamados por unidade"): mantém todas as filiais ativas via LEFT JOIN
     * com os filtros de janela no ON, retornando volume e % dentro da meta de SLA.
     */
    public List<ChamadosPorUnidadeDto> buscarChamadosPorUnidade(Instant inicio, Instant fim, OrigemDado origemDado) {
        String sql = """
            WITH pausas_solicitante AS (
                SELECT
                    ev.chamado_id,
                    SUM(
                        GREATEST(
                            EXTRACT(EPOCH FROM (COALESCE(ev.proximo_evento_em, ev.resolvido_em, NOW()) - ev.ocorrido_em)),
                            0.0
                        )
                    ) AS segundos_pausados
                FROM (
                    SELECT
                        e.chamado_id,
                        e.tipo,
                        e.dados->>'motivo_espera' AS motivo_espera,
                        e.ocorrido_em,
                        ch.resolvido_em,
                        LEAD(e.ocorrido_em) OVER (PARTITION BY e.chamado_id ORDER BY e.ocorrido_em ASC) AS proximo_evento_em
                    FROM eventos_historico e
                    JOIN chamados ch ON ch.id = e.chamado_id
                    WHERE e.origem_dado = :origemDado
                      AND e.tipo IN (%s)
                      AND ch.origem_dado = :origemDado
                      AND ch.aberto_em >= :inicio
                      AND ch.aberto_em <= :fim
                ) ev
                WHERE ev.tipo = 'CHAMADO_AGUARDANDO'
                  AND ev.motivo_espera = 'SOLICITANTE'
                GROUP BY ev.chamado_id
            ),
            chamados_unidade AS (
                SELECT
                    c.id,
                    c.unidade_id,
                    c.aberto_em,
                    c.resolvido_em,
                    CASE
                        WHEN c.resolvido_em IS NOT NULL AND COALESCE(c.prioridade_final, c.prioridade_sugerida) IS NOT NULL THEN
                            GREATEST(
                                EXTRACT(EPOCH FROM (c.resolvido_em - c.aberto_em)) - COALESCE(p.segundos_pausados, 0.0),
                                0.0
                            ) <= CASE COALESCE(c.prioridade_final, c.prioridade_sugerida)
                                WHEN 'ALTA'  THEN 7200.0
                                WHEN 'MEDIA' THEN 28800.0
                                WHEN 'BAIXA' THEN 86400.0
                            END
                        ELSE NULL
                    END AS dentro_sla,
                    CASE
                        WHEN c.resolvido_em IS NOT NULL THEN
                            GREATEST(
                                EXTRACT(EPOCH FROM (c.resolvido_em - c.aberto_em)) - COALESCE(p.segundos_pausados, 0.0),
                                0.0
                            )
                        ELSE NULL
                    END AS tr_segundos
                FROM chamados c
                LEFT JOIN pausas_solicitante p ON p.chamado_id = c.id
                WHERE c.origem_dado = :origemDado
                  AND c.aberto_em >= :inicio
                  AND c.aberto_em <= :fim
            )
            SELECT
                u.id            AS unidade_id,
                u.nome          AS unidade_nome,
                u.identificacao AS unidade_identificacao,
                COUNT(cu.id)    AS total_chamados,
                COUNT(cu.id) FILTER (WHERE cu.resolvido_em IS NOT NULL) AS chamados_resolvidos,
                COALESCE(
                    (COUNT(cu.id) FILTER (WHERE cu.dentro_sla IS TRUE) * 100.0)
                    / NULLIF(COUNT(cu.id) FILTER (WHERE cu.dentro_sla IS NOT NULL), 0),
                    0.0
                ) AS percentual_dentro_da_meta,
                COALESCE(
                    AVG(cu.tr_segundos) FILTER (WHERE cu.tr_segundos IS NOT NULL),
                    0.0
                ) AS tempo_medio_atendimento_segundos
            FROM unidades u
            LEFT JOIN chamados_unidade cu ON cu.unidade_id = u.id
            WHERE u.ativo = TRUE
            GROUP BY u.id, u.nome, u.identificacao
            ORDER BY total_chamados DESC, u.nome ASC
            """.formatted(TIPOS_EVENTO_RELOGIO_SLA);

        return jdbcClient.sql(sql)
            .param("inicio", Timestamp.from(inicio))
            .param("fim", Timestamp.from(fim))
            .param("origemDado", origemDado.name())
            .query((rs, rowNum) -> new ChamadosPorUnidadeDto(
                UUID.fromString(rs.getString("unidade_id")),
                rs.getString("unidade_nome"),
                rs.getString("unidade_identificacao"),
                rs.getLong("total_chamados"),
                rs.getLong("chamados_resolvidos"),
                arredondar(rs.getDouble("percentual_dentro_da_meta")),
                arredondar(rs.getDouble("tempo_medio_atendimento_segundos"))
            ))
            .list();
    }

    /**
     * Distribuição de chamados em aberto (em andamento) por analista responsável (M02).
     * Chamados sem responsável atribuído aparecem explicitamente como "Sem responsável".
     */
    public List<ChamadosPorResponsavelDto> buscarChamadosAbertosPorResponsavel(
        Instant inicio,
        Instant fim,
        UUID unidadeId,
        OrigemDado origemDado
    ) {
        String filtroUnidade = unidadeId != null ? " AND c.unidade_id = :unidadeId " : "";

        String sql = """
            SELECT
                u.id AS responsavel_id,
                COALESCE(u.nome, 'Sem responsável') AS responsavel_nome,
                COUNT(c.id) AS total_em_aberto,
                COUNT(c.id) FILTER (WHERE COALESCE(c.prioridade_final, c.prioridade_sugerida) = 'ALTA') AS total_alta_prioridade
            FROM chamados c
            LEFT JOIN usuarios u ON u.id = c.responsavel_id
            WHERE c.origem_dado = :origemDado
              AND c.aberto_em >= :inicio
              AND c.aberto_em <= :fim
              AND c.status NOT IN ('RESOLVIDO', 'FECHADO', 'CANCELADO', 'MESCLADO')
              %s
            GROUP BY u.id, u.nome
            ORDER BY total_em_aberto DESC, (u.id IS NULL) DESC, responsavel_nome ASC
            """.formatted(filtroUnidade);

        var spec = jdbcClient.sql(sql)
            .param("inicio", Timestamp.from(inicio))
            .param("fim", Timestamp.from(fim))
            .param("origemDado", origemDado.name());

        if (unidadeId != null) {
            spec = spec.param("unidadeId", unidadeId);
        }

        return spec.query((rs, rowNum) -> {
            String respId = rs.getString("responsavel_id");
            return new ChamadosPorResponsavelDto(
                respId != null ? UUID.fromString(respId) : null,
                rs.getString("responsavel_nome"),
                rs.getLong("total_em_aberto"),
                rs.getLong("total_alta_prioridade")
            );
        }).list();
    }

    /**
     * Card "SLA por prioridade": retorna sempre Alta (P1), Média (P2) e Baixa (P3).
     */
    public List<SlaPorPrioridadeDto> buscarSlaPorPrioridade(
        Instant inicio,
        Instant fim,
        UUID unidadeId,
        OrigemDado origemDado
    ) {
        String filtroUnidade = unidadeId != null ? " AND c.unidade_id = :unidadeId " : "";

        String sql = """
            WITH faixas(codigo, rotulo, meta_segundos, ordem) AS (
                VALUES
                    ('ALTA',  'Alta (P1)',  7200,  1),
                    ('MEDIA', 'Média (P2)', 28800, 2),
                    ('BAIXA', 'Baixa (P3)', 86400, 3)
            ),
            pausas_solicitante AS (
                SELECT
                    ev.chamado_id,
                    SUM(
                        GREATEST(
                            EXTRACT(EPOCH FROM (COALESCE(ev.proximo_evento_em, ev.resolvido_em, NOW()) - ev.ocorrido_em)),
                            0.0
                        )
                    ) AS segundos_pausados
                FROM (
                    SELECT
                        e.chamado_id,
                        e.tipo,
                        e.dados->>'motivo_espera' AS motivo_espera,
                        e.ocorrido_em,
                        ch.resolvido_em,
                        LEAD(e.ocorrido_em) OVER (PARTITION BY e.chamado_id ORDER BY e.ocorrido_em ASC) AS proximo_evento_em
                    FROM eventos_historico e
                    JOIN chamados ch ON ch.id = e.chamado_id
                    WHERE e.origem_dado = :origemDado
                      AND e.tipo IN (%s)
                      AND ch.origem_dado = :origemDado
                      AND ch.aberto_em >= :inicio
                      AND ch.aberto_em <= :fim
                ) ev
                WHERE ev.tipo = 'CHAMADO_AGUARDANDO'
                  AND ev.motivo_espera = 'SOLICITANTE'
                GROUP BY ev.chamado_id
            ),
            chamados_avaliados AS (
                SELECT
                    COALESCE(c.prioridade_final, c.prioridade_sugerida) AS prioridade,
                    GREATEST(
                        EXTRACT(EPOCH FROM (c.resolvido_em - c.aberto_em)) - COALESCE(p.segundos_pausados, 0.0),
                        0.0
                    ) AS tr_segundos
                FROM chamados c
                LEFT JOIN pausas_solicitante p ON p.chamado_id = c.id
                WHERE c.origem_dado = :origemDado
                  AND c.aberto_em >= :inicio
                  AND c.aberto_em <= :fim
                  AND c.resolvido_em IS NOT NULL
                  AND COALESCE(c.prioridade_final, c.prioridade_sugerida) IS NOT NULL
                  %s
            )
            SELECT
                f.codigo,
                f.rotulo,
                f.meta_segundos,
                COUNT(ca.prioridade) AS total_avaliados,
                COUNT(ca.prioridade) FILTER (WHERE ca.tr_segundos <= f.meta_segundos) AS total_dentro_da_meta,
                COALESCE(
                    (COUNT(ca.prioridade) FILTER (WHERE ca.tr_segundos <= f.meta_segundos) * 100.0)
                    / NULLIF(COUNT(ca.prioridade), 0),
                    0.0
                ) AS percentual_dentro_da_meta
            FROM faixas f
            LEFT JOIN chamados_avaliados ca ON ca.prioridade = f.codigo
            GROUP BY f.codigo, f.rotulo, f.meta_segundos, f.ordem
            ORDER BY f.ordem ASC
            """.formatted(TIPOS_EVENTO_RELOGIO_SLA, filtroUnidade);

        var spec = jdbcClient.sql(sql)
            .param("inicio", Timestamp.from(inicio))
            .param("fim", Timestamp.from(fim))
            .param("origemDado", origemDado.name());

        if (unidadeId != null) {
            spec = spec.param("unidadeId", unidadeId);
        }

        return spec.query((rs, rowNum) -> {
            long totalAvaliados = rs.getLong("total_avaliados");
            double percentual = arredondar(rs.getDouble("percentual_dentro_da_meta"));
            boolean abaixoDaMeta = totalAvaliados > 0 && percentual < META_INICIAL_SLA_PERCENTUAL;
            return new SlaPorPrioridadeDto(
                rs.getString("codigo"),
                rs.getString("rotulo"),
                rs.getLong("meta_segundos"),
                totalAvaliados,
                rs.getLong("total_dentro_da_meta"),
                percentual,
                abaixoDaMeta
            );
        }).list();
    }

    /**
     * Tabela "Problemas recorrentes": agrupa ocorrências com flag de recorrência ou causa raiz preenchida.
     */
    public List<ProblemaRecorrenteDto> buscarProblemasRecorrentes(
        Instant inicio,
        Instant fim,
        UUID unidadeId,
        OrigemDado origemDado
    ) {
        String filtroUnidade = unidadeId != null ? " AND c.unidade_id = :unidadeId " : "";

        String sql = """
            WITH recorrentes_por_unidade AS (
                SELECT
                    COALESCE(
                        NULLIF(TRIM(c.causa_raiz), ''),
                        se.nome,
                        cat.nome,
                        'Recorrência não detalhada'
                    ) AS problema,
                    u.id AS unidade_id,
                    u.nome AS unidade_nome,
                    COUNT(c.id) AS qtd_unidade,
                    MAX(c.aberto_em) AS ultima_unidade
                FROM chamados c
                JOIN unidades u ON u.id = c.unidade_id
                LEFT JOIN sistemas_equipamentos se ON se.id = c.sistema_equipamento_id
                LEFT JOIN categorias cat ON cat.id = c.categoria_id
                WHERE c.origem_dado = :origemDado
                  AND c.aberto_em >= :inicio
                  AND c.aberto_em <= :fim
                  AND (c.possivel_recorrencia = TRUE OR NULLIF(TRIM(c.causa_raiz), '') IS NOT NULL)
                  %s
                GROUP BY 1, u.id, u.nome
            )
            SELECT
                problema,
                SUM(qtd_unidade) AS ocorrencias,
                MAX(ultima_unidade) AS ultima_ocorrencia,
                STRING_AGG(unidade_nome, ';;' ORDER BY qtd_unidade DESC, unidade_nome ASC) AS unidades_concatenadas
            FROM recorrentes_por_unidade
            GROUP BY problema
            ORDER BY ocorrencias DESC, ultima_ocorrencia DESC
            LIMIT 10
            """.formatted(filtroUnidade);

        var spec = jdbcClient.sql(sql)
            .param("inicio", Timestamp.from(inicio))
            .param("fim", Timestamp.from(fim))
            .param("origemDado", origemDado.name());

        if (unidadeId != null) {
            spec = spec.param("unidadeId", unidadeId);
        }

        return spec.query((rs, rowNum) -> {
            String concatenadas = rs.getString("unidades_concatenadas");
            List<String> unidades = concatenadas == null || concatenadas.isBlank()
                ? List.of()
                : Arrays.stream(concatenadas.split(SEPARADOR_UNIDADES)).limit(3).toList();
            return new ProblemaRecorrenteDto(
                rs.getString("problema"),
                rs.getLong("ocorrencias"),
                rs.getTimestamp("ultima_ocorrencia").toInstant(),
                unidades
            );
        }).list();
    }

    /**
     * Card "Tempo em terceiros" (M05): tempo médio de retorno agrupado por fornecedor externo.
     */
    public List<TempoTerceirosFornecedorDto> buscarTempoEmTerceiros(
        Instant inicio,
        Instant fim,
        UUID unidadeId,
        OrigemDado origemDado
    ) {
        String filtroUnidade = unidadeId != null ? " AND c.unidade_id = :unidadeId " : "";

        String sql = """
            SELECT
                e.fornecedor,
                COUNT(e.id) AS total_escalonamentos,
                COUNT(e.id) FILTER (WHERE e.retorno_em IS NOT NULL) AS escalonamentos_concluidos,
                COALESCE(
                    AVG(EXTRACT(EPOCH FROM (e.retorno_em - e.enviado_em)))
                        FILTER (WHERE e.retorno_em IS NOT NULL),
                    0.0
                ) AS tempo_medio_retorno_segundos
            FROM escalonamentos e
            JOIN chamados c ON c.id = e.chamado_id
            WHERE e.origem_dado = :origemDado
              AND e.enviado_em >= :inicio
              AND e.enviado_em <= :fim
              %s
            GROUP BY e.fornecedor
            ORDER BY tempo_medio_retorno_segundos DESC, total_escalonamentos DESC, e.fornecedor ASC
            """.formatted(filtroUnidade);

        var spec = jdbcClient.sql(sql)
            .param("inicio", Timestamp.from(inicio))
            .param("fim", Timestamp.from(fim))
            .param("origemDado", origemDado.name());

        if (unidadeId != null) {
            spec = spec.param("unidadeId", unidadeId);
        }

        return spec.query((rs, rowNum) -> new TempoTerceirosFornecedorDto(
            rs.getString("fornecedor"),
            rs.getLong("total_escalonamentos"),
            rs.getLong("escalonamentos_concluidos"),
            arredondar(rs.getDouble("tempo_medio_retorno_segundos"))
        )).list();
    }

    /** Volume de chamados por categoria ativa. */
    public List<VolumeItemDto> buscarVolumePorCategoria(
        Instant inicio,
        Instant fim,
        UUID unidadeId,
        OrigemDado origemDado
    ) {
        String filtroUnidade = unidadeId != null ? " AND c.unidade_id = :unidadeId " : "";

        String sql = """
            SELECT
                cat.id,
                cat.nome,
                COUNT(c.id) AS total_chamados
            FROM categorias cat
            LEFT JOIN chamados c
                ON c.categoria_id = cat.id
               AND c.origem_dado = :origemDado
               AND c.aberto_em >= :inicio
               AND c.aberto_em <= :fim
               %s
            WHERE cat.ativo = TRUE
            GROUP BY cat.id, cat.nome
            ORDER BY total_chamados DESC, cat.nome ASC
            """.formatted(filtroUnidade);

        var spec = jdbcClient.sql(sql)
            .param("inicio", Timestamp.from(inicio))
            .param("fim", Timestamp.from(fim))
            .param("origemDado", origemDado.name());

        if (unidadeId != null) {
            spec = spec.param("unidadeId", unidadeId);
        }

        return spec.query((rs, rowNum) -> new VolumeItemDto(
            UUID.fromString(rs.getString("id")),
            rs.getString("nome"),
            rs.getLong("total_chamados")
        )).list();
    }

    /** Volume de chamados por sistema/equipamento ativo. */
    public List<VolumeItemDto> buscarVolumePorEquipamento(
        Instant inicio,
        Instant fim,
        UUID unidadeId,
        OrigemDado origemDado
    ) {
        String filtroUnidade = unidadeId != null ? " AND c.unidade_id = :unidadeId " : "";

        String sql = """
            SELECT
                se.id,
                se.nome,
                COUNT(c.id) AS total_chamados
            FROM sistemas_equipamentos se
            LEFT JOIN chamados c
                ON c.sistema_equipamento_id = se.id
               AND c.origem_dado = :origemDado
               AND c.aberto_em >= :inicio
               AND c.aberto_em <= :fim
               %s
            WHERE se.ativo = TRUE
            GROUP BY se.id, se.nome
            ORDER BY total_chamados DESC, se.nome ASC
            """.formatted(filtroUnidade);

        var spec = jdbcClient.sql(sql)
            .param("inicio", Timestamp.from(inicio))
            .param("fim", Timestamp.from(fim))
            .param("origemDado", origemDado.name());

        if (unidadeId != null) {
            spec = spec.param("unidadeId", unidadeId);
        }

        return spec.query((rs, rowNum) -> new VolumeItemDto(
            UUID.fromString(rs.getString("id")),
            rs.getString("nome"),
            rs.getLong("total_chamados")
        )).list();
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public record ResumoIndicadoresRow(
        long totalAtual,
        long totalAnterior,
        long resolvidosAtual,
        long resolvidosAnterior,
        double trMedianoAtualSegundos,
        double trMedianoAnteriorSegundos,
        double trMedioAtualSegundos,
        double trMedioAnteriorSegundos,
        double trP90AtualSegundos,
        long rastreaveisAvaliados,
        long rastreaveisConformes,
        long slaAvaliados,
        long slaConformes,
        double ttMedioSegundos,
        double tprMedioSegundos,
        double m06TaxaReabertura,
        long m07TotalDuplicados,
        double m07TaxaDuplicidade,
        double m11MedioSegundos,
        double m08PercentualDivergencia,
        long m12IncidentesPrivacidade
    ) {}
}
