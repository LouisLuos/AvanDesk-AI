-- =============================================
-- V7__adiciona_evento_atendimento_retomado.sql
-- HU06: REABERTO -> EM_ATENDIMENTO ("atendimento retomado", diagrama v3)
-- O gatilho de imutabilidade so barra UPDATE e DELETE de linhas, nao a troca da restricao
-- =============================================

ALTER TABLE eventos_historico DROP CONSTRAINT ck_eventos_historico_tipo;

ALTER TABLE eventos_historico ADD CONSTRAINT ck_eventos_historico_tipo CHECK (tipo IN (
    'CHAMADO_ABERTO', 'REGISTRO_DE_CONTATO_RECEBIDO', 'TRIAGEM_INICIADA', 'PRIORIDADE_DEFINIDA',
    'CHAMADO_TRIADO', 'CHAMADO_ATRIBUIDO', 'CHAMADO_AGUARDANDO', 'RETORNO_RECEBIDO',
    'CHAMADO_ESCALADO', 'CHAMADO_RESOLVIDO', 'CHAMADO_REABERTO', 'ATENDIMENTO_RETOMADO',
    'CHAMADO_FECHADO', 'CHAMADO_CANCELADO', 'RELATO_VINCULADO', 'RELATO_DESVINCULADO',
    'CHAMADO_MESCLADO', 'NOTIFICACAO_ENVIADA', 'ANEXO_VISUALIZADO', 'ANOTACAO_ADICIONADA'
));
