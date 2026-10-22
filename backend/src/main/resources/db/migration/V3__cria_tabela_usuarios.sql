-- =============================================
-- V3__cria_tabela_usuarios.sql
-- US-01 (HU01), tarefa T09.1
-- Autor de chamados, contatos e eventos
-- =============================================

CREATE TABLE usuarios (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nome        VARCHAR(120) NOT NULL,
    email       VARCHAR(254) NOT NULL,
    senha_hash  VARCHAR(100) NOT NULL,
    perfil      VARCHAR(30)  NOT NULL,
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_usuarios_email UNIQUE (email),
    CONSTRAINT ck_usuarios_email_minusculo CHECK (email = lower(email)),
    CONSTRAINT ck_usuarios_perfil CHECK (perfil IN ('ANALISTA_SUPORTE', 'LIDERANCA_TI', 'ADMIN'))
);
