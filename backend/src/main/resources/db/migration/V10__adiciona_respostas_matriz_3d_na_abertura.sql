-- =============================================
-- V10__adiciona_respostas_matriz_3d_na_abertura.sql
-- HU22 (T22.1): respostas do solicitante na abertura, convertidas em notas da Matriz 3D
-- As notas validadas pelo analista continuam em severidade, urgencia e impacto_operacional (HU04)
-- V9 fica reservada para os indices do painel da administracao (HU08)
-- =============================================

ALTER TABLE chamados
    ADD COLUMN severidade_informada SMALLINT,
    ADD COLUMN urgencia_informada   SMALLINT,
    ADD COLUMN impacto_informado    SMALLINT;

ALTER TABLE chamados
    ADD CONSTRAINT ck_chamados_severidade_informada CHECK (severidade_informada BETWEEN 1 AND 3),
    ADD CONSTRAINT ck_chamados_urgencia_informada CHECK (urgencia_informada BETWEEN 1 AND 3),
    ADD CONSTRAINT ck_chamados_impacto_informado CHECK (impacto_informado BETWEEN 1 AND 3),
    ADD CONSTRAINT ck_chamados_respostas_informadas CHECK (
        (severidade_informada IS NULL AND urgencia_informada IS NULL AND impacto_informado IS NULL)
        OR (severidade_informada IS NOT NULL AND urgencia_informada IS NOT NULL AND impacto_informado IS NOT NULL)
    );
