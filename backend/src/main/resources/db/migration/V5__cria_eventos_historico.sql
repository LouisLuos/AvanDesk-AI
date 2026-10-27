-- =============================================
-- V5__cria_eventos_historico.sql
-- US-01 (HU01), tarefa BE-02 (T02.1)
-- Historico so aceita INSERT: trigger bloqueia UPDATE e DELETE
-- =============================================

CREATE TABLE eventos_historico (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    chamado_id  UUID        NOT NULL REFERENCES chamados (id),
    tipo        VARCHAR(40) NOT NULL,
    ocorrido_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    autor_id    UUID        REFERENCES usuarios (id),
    dados       JSONB       NOT NULL DEFAULT '{}',
    origem_dado VARCHAR(10) NOT NULL,
    CONSTRAINT ck_eventos_historico_tipo CHECK (tipo IN (
        'CHAMADO_ABERTO', 'REGISTRO_DE_CONTATO_RECEBIDO', 'TRIAGEM_INICIADA', 'PRIORIDADE_DEFINIDA',
        'CHAMADO_TRIADO', 'CHAMADO_ATRIBUIDO', 'CHAMADO_AGUARDANDO', 'RETORNO_RECEBIDO',
        'CHAMADO_ESCALADO', 'CHAMADO_RESOLVIDO', 'CHAMADO_REABERTO', 'CHAMADO_FECHADO',
        'CHAMADO_CANCELADO', 'RELATO_VINCULADO', 'RELATO_DESVINCULADO', 'CHAMADO_MESCLADO',
        'NOTIFICACAO_ENVIADA', 'ANEXO_VISUALIZADO', 'ANOTACAO_ADICIONADA'
    )),
    CONSTRAINT ck_eventos_historico_origem_dado CHECK (origem_dado IN ('REAL', 'TESTE', 'SIMULADO'))
);

CREATE INDEX idx_eventos_historico_chamado ON eventos_historico (chamado_id, ocorrido_em);

CREATE FUNCTION impedir_alteracao_evento_historico() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'eventos_historico so aceita INSERT (% bloqueado)', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_eventos_historico_imutavel
    BEFORE UPDATE OR DELETE ON eventos_historico
    FOR EACH ROW EXECUTE FUNCTION impedir_alteracao_evento_historico();
