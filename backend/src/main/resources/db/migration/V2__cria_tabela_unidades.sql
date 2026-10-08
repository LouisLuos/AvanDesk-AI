-- =============================================
-- V2__cria_tabela_unidades.sql
-- US-11 (HU11 - Cadastrar unidades), tarefa T11.1
-- Campos conforme o Trello e o modelo conceitual
-- =============================================

CREATE TABLE unidades (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nome            VARCHAR(120) NOT NULL,
    identificacao   VARCHAR(20)  NOT NULL UNIQUE,
    localizacao     VARCHAR(255),
    atendimento_24h BOOLEAN      NOT NULL DEFAULT FALSE,
    ativo           BOOLEAN      NOT NULL DEFAULT TRUE
);
