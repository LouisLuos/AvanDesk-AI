-- =============================================
-- V9__indices_analiticos_dashboard.sql
-- HU08 (T08.1): indices compostos e parciais para
-- acelerar as agregacoes do Painel da Administracao
-- (SLA, comparativo de unidades, recorrencia e fornecedores)
-- =============================================

-- 1. Indice composto SARGable para filtro por origem_dado, janela de periodo (atual e anterior) e unidade
CREATE INDEX idx_chamados_dashboard_periodo
    ON chamados (origem_dado, aberto_em, unidade_id);

-- 2. Indice composto para agregacoes de SLA por prioridade (P1, P2, P3) e tempos de resolucao
CREATE INDEX idx_chamados_dashboard_prioridade_resolucao
    ON chamados (origem_dado, prioridade_final, aberto_em)
    WHERE resolvido_em IS NOT NULL;

-- 3. Indice parcial para a tabela de Problemas Recorrentes (HU06 / HU08)
CREATE INDEX idx_chamados_dashboard_recorrencia
    ON chamados (origem_dado, aberto_em)
    WHERE possivel_recorrencia = TRUE OR causa_raiz IS NOT NULL;

-- 4. Indice composto em eventos_historico para calculo de pausas (SOLICITANTE) e auditoria LGPD (M12)
CREATE INDEX idx_eventos_historico_origem_tipo_chamado
    ON eventos_historico (origem_dado, tipo, chamado_id);

-- 5. Indice composto em escalonamentos para o card de Tempo em Terceiros por fornecedor (M05)
CREATE INDEX idx_escalonamentos_dashboard_fornecedor
    ON escalonamentos (origem_dado, fornecedor, enviado_em);
