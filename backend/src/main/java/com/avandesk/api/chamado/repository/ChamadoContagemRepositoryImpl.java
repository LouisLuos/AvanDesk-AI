package com.avandesk.api.chamado.repository;

import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.MotivoEspera;
import com.avandesk.api.chamado.entity.Prioridade;
import com.avandesk.api.chamado.entity.StatusChamado;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Implementação do fragmento {@link ChamadoContagemRepository}, montada pelo Spring Data. */
class ChamadoContagemRepositoryImpl implements ChamadoContagemRepository {

    private static final long META_TRIAGEM_SEGUNDOS = 15L * 60L;
    private static final long META_ALTA_SEGUNDOS = 120L * 60L;
    private static final long META_MEDIA_SEGUNDOS = 480L * 60L;
    private static final long META_BAIXA_SEGUNDOS = 1440L * 60L;
    private static final int TAMANHO_LOTE_IDS = 1000;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Map<StatusChamado, Long> contarPorStatus(Specification<Chamado> consulta) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Chamado> root = query.from(Chamado.class);
        Path<StatusChamado> status = root.get("status");

        Predicate condicao = consulta.toPredicate(root, query, cb);
        if (condicao != null) {
            query.where(condicao);
        }
        query.multiselect(status, cb.count(root)).groupBy(status);

        Map<StatusChamado, Long> totais = new EnumMap<>(StatusChamado.class);
        for (Tuple linha : entityManager.createQuery(query).getResultList()) {
            totais.put(linha.get(0, StatusChamado.class), linha.get(1, Long.class));
        }
        return totais;
    }

    @Override
    public long contarEstourados(Specification<Chamado> consulta) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Chamado> root = query.from(Chamado.class);

        EnumSet<StatusChamado> ativos = EnumSet.noneOf(StatusChamado.class);
        for (StatusChamado s : StatusChamado.values()) {
            if (s.emAndamento()) {
                ativos.add(s);
            }
        }

        List<Predicate> restricoes = new ArrayList<>();
        Predicate filtroConsulta = consulta.toPredicate(root, query, cb);
        if (filtroConsulta != null) {
            restricoes.add(filtroConsulta);
        }
        restricoes.add(root.get("status").in(ativos));
        restricoes.add(cb.not(cb.and(
            cb.equal(root.get("status"), StatusChamado.AGUARDANDO),
            cb.equal(root.get("motivoEspera"), MotivoEspera.SOLICITANTE)
        )));

        query.multiselect(
            root.get("id"),
            root.get("status"),
            root.get("prioridadeFinal"),
            root.get("prioridadeSugerida"),
            root.get("abertoEm")
        ).where(restricoes.toArray(Predicate[]::new));

        List<Tuple> linhas = entityManager.createQuery(query).getResultList();
        if (linhas.isEmpty()) {
            return 0L;
        }

        Instant agora = Instant.now();
        record Candidato(UUID id, long decorridoBrutoSegundos, long metaSegundos) {}
        List<Candidato> candidatos = new ArrayList<>();

        for (Tuple linha : linhas) {
            UUID id = linha.get(0, UUID.class);
            StatusChamado status = linha.get(1, StatusChamado.class);
            Prioridade prioFinal = linha.get(2, Prioridade.class);
            Prioridade prioSugerida = linha.get(3, Prioridade.class);
            Instant abertoEm = linha.get(4, Instant.class);

            Prioridade prioridade = prioFinal != null ? prioFinal : prioSugerida;
            long metaSegundos = obterMetaSegundos(status, prioridade);
            long decorridoBruto = Math.max(0L, Duration.between(abertoEm, agora).getSeconds());

            // Pausa de solicitante só reduz o tempo decorrido; quem não estourou no bruto não estoura após desconto.
            if (decorridoBruto > metaSegundos) {
                candidatos.add(new Candidato(id, decorridoBruto, metaSegundos));
            }
        }

        if (candidatos.isEmpty()) {
            return 0L;
        }

        Map<UUID, Double> pausasPorChamado = buscarPausasSolicitanteSegundos(
            candidatos.stream().map(Candidato::id).toList()
        );

        long totalEstourados = 0L;
        for (Candidato c : candidatos) {
            double pausado = pausasPorChamado.getOrDefault(c.id(), 0.0);
            double decorridoEfetivo = Math.max(0.0, c.decorridoBrutoSegundos() - pausado);
            if (decorridoEfetivo > c.metaSegundos()) {
                totalEstourados++;
            }
        }
        return totalEstourados;
    }

    private Map<UUID, Double> buscarPausasSolicitanteSegundos(List<UUID> ids) {
        Map<UUID, Double> resultado = new HashMap<>();
        String sql = """
            SELECT ev.chamado_id,
                   COALESCE(SUM(EXTRACT(EPOCH FROM (COALESCE(ev.proximo_evento_em, NOW()) - ev.ocorrido_em))), 0.0)
            FROM (
                SELECT e.chamado_id,
                       e.tipo,
                       e.dados->>'motivo_espera' AS motivo_espera,
                       e.ocorrido_em,
                       LEAD(e.ocorrido_em) OVER (PARTITION BY e.chamado_id ORDER BY e.ocorrido_em ASC) AS proximo_evento_em
                FROM eventos_historico e
                WHERE e.chamado_id IN (:ids)
            ) ev
            WHERE ev.tipo = 'CHAMADO_AGUARDANDO'
              AND ev.motivo_espera = 'SOLICITANTE'
            GROUP BY ev.chamado_id
            """;

        for (int i = 0; i < ids.size(); i += TAMANHO_LOTE_IDS) {
            List<UUID> lote = ids.subList(i, Math.min(i + TAMANHO_LOTE_IDS, ids.size()));
            @SuppressWarnings("unchecked")
            List<Object[]> linhas = entityManager.createNativeQuery(sql)
                .setParameter("ids", lote)
                .getResultList();
            for (Object[] linha : linhas) {
                UUID id = linha[0] instanceof UUID uuid ? uuid : UUID.fromString(linha[0].toString());
                double segundos = ((Number) linha[1]).doubleValue();
                resultado.put(id, segundos);
            }
        }
        return resultado;
    }

    private static long obterMetaSegundos(StatusChamado status, Prioridade prioridade) {
        if (prioridade == null || status == StatusChamado.EM_TRIAGEM) {
            return META_TRIAGEM_SEGUNDOS;
        }
        return switch (prioridade) {
            case ALTA -> META_ALTA_SEGUNDOS;
            case MEDIA -> META_MEDIA_SEGUNDOS;
            case BAIXA -> META_BAIXA_SEGUNDOS;
        };
    }

    @Override
    public Map<UUID, Long> contarEmAndamentoPorResponsavel() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Chamado> root = query.from(Chamado.class);
        Path<UUID> responsavel = root.get("responsavel").get("id");
        EnumSet<StatusChamado> emAndamento = EnumSet.noneOf(StatusChamado.class);
        for (StatusChamado status : StatusChamado.values()) {
            if (status.emAndamento()) {
                emAndamento.add(status);
            }
        }

        query.multiselect(responsavel, cb.count(root))
            .where(cb.isNotNull(root.get("responsavel")), root.get("status").in(emAndamento))
            .groupBy(responsavel);

        Map<UUID, Long> totais = new HashMap<>();
        for (Tuple linha : entityManager.createQuery(query).getResultList()) {
            totais.put(linha.get(0, UUID.class), linha.get(1, Long.class));
        }
        return totais;
    }
}
