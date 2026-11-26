ALTER TABLE usuarios DROP CONSTRAINT ck_usuarios_perfil;

ALTER TABLE usuarios
    ADD CONSTRAINT ck_usuarios_perfil CHECK (perfil IN ('ANALISTA_SUPORTE', 'LIDERANCA_TI', 'ADMIN', 'SOLICITANTE'));

ALTER TABLE usuarios
    ADD COLUMN status_conta VARCHAR(30) NOT NULL DEFAULT 'ATIVA',
    ADD COLUMN unidade_id   UUID REFERENCES unidades (id);

ALTER TABLE usuarios
    ADD CONSTRAINT ck_usuarios_status_conta CHECK (status_conta IN ('PENDENTE_VERIFICACAO', 'ATIVA')),
    ADD CONSTRAINT ck_usuarios_verificacao_so_do_solicitante CHECK (status_conta = 'ATIVA' OR perfil = 'SOLICITANTE'),
    ADD CONSTRAINT ck_usuarios_unidade_do_solicitante CHECK (perfil <> 'SOLICITANTE' OR unidade_id IS NOT NULL);

CREATE INDEX idx_usuarios_unidade ON usuarios (unidade_id) WHERE unidade_id IS NOT NULL;

ALTER TABLE solicitantes
    ADD COLUMN usuario_id UUID REFERENCES usuarios (id),
    ADD CONSTRAINT uq_solicitantes_usuario UNIQUE (usuario_id);

CREATE TABLE tokens_verificacao_email (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id   UUID        NOT NULL REFERENCES usuarios (id),
    token_hash   VARCHAR(64) NOT NULL,
    expira_em    TIMESTAMPTZ NOT NULL,
    utilizado_em TIMESTAMPTZ,
    revogado_em  TIMESTAMPTZ,
    criado_em    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_tokens_verificacao_email_hash UNIQUE (token_hash),
    CONSTRAINT ck_tokens_verificacao_email_hash CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_tokens_verificacao_email_validade CHECK (expira_em > criado_em),
    CONSTRAINT ck_tokens_verificacao_email_um_desfecho CHECK (utilizado_em IS NULL OR revogado_em IS NULL)
);

CREATE INDEX idx_tokens_verificacao_email_usuario ON tokens_verificacao_email (usuario_id, criado_em DESC);
