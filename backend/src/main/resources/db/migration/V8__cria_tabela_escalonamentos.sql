-- =============================================
-- V8__cria_tabela_escalonamentos.sql
-- HU07 (T07.1): escalonamento do chamado para fornecedor externo
-- Fornecedor em texto livre ate decidirem se tera cadastro proprio (secao 8 do backlog)
-- Um chamado pode ter varios escalonamentos, mas so um aberto (sem retorno) por vez
-- =============================================

CREATE TABLE escalonamentos (
    id                        UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    chamado_id                UUID          NOT NULL REFERENCES chamados (id),
    fornecedor                VARCHAR(120)  NOT NULL,
    protocolo_externo         VARCHAR(100),
    observacao                VARCHAR(1000),
    responsavel_id            UUID          NOT NULL REFERENCES usuarios (id),
    enviado_por_id            UUID          NOT NULL REFERENCES usuarios (id),
    enviado_em                TIMESTAMPTZ   NOT NULL,
    retorno                   VARCHAR(2000),
    retorno_em                TIMESTAMPTZ,
    retorno_registrado_por_id UUID          REFERENCES usuarios (id),
    origem_dado               VARCHAR(10)   NOT NULL,
    CONSTRAINT ck_escalonamentos_retorno_depois_do_envio CHECK (retorno_em IS NULL OR retorno_em >= enviado_em),
    CONSTRAINT ck_escalonamentos_retorno_completo CHECK (
        (retorno_em IS NULL) = (retorno IS NULL)
        AND (retorno_em IS NULL) = (retorno_registrado_por_id IS NULL)
    ),
    CONSTRAINT ck_escalonamentos_origem_dado CHECK (origem_dado IN ('REAL', 'TESTE', 'SIMULADO'))
);

CREATE INDEX idx_escalonamentos_chamado ON escalonamentos (chamado_id);
CREATE UNIQUE INDEX uq_escalonamentos_aberto ON escalonamentos (chamado_id) WHERE retorno_em IS NULL;

-- Chamados que ja aguardam fornecedor (PATCH /status da HU12) ganham um escalonamento aberto,
-- para poderem sair da espera pelo registro do retorno
INSERT INTO escalonamentos (chamado_id, fornecedor, observacao, responsavel_id, enviado_por_id, enviado_em, origem_dado)
SELECT c.id,
       'Não informado',
       'Criado na implantação do escalonamento: o chamado já aguardava fornecedor.',
       COALESCE(c.responsavel_id, c.criado_por_id),
       COALESCE(
           (SELECT e.autor_id
            FROM eventos_historico e
            WHERE e.chamado_id = c.id AND e.tipo = 'CHAMADO_AGUARDANDO' AND e.autor_id IS NOT NULL
            ORDER BY e.ocorrido_em DESC
            LIMIT 1),
           c.responsavel_id,
           c.criado_por_id),
       COALESCE(c.espera_iniciada_em, c.aberto_em),
       c.origem_dado
FROM chamados c
WHERE c.status = 'AGUARDANDO'
  AND c.motivo_espera = 'FORNECEDOR';
