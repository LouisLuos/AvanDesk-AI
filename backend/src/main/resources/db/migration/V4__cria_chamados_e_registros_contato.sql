-- =============================================
-- V4__cria_chamados_e_registros_contato.sql
-- US-01 (HU01), tarefa BE-01 (T01.1 e T01.2)
-- Colunas das Levas 2 e 3 ja nascem nulas (secao 9.5)
-- Categorias e sistemas vem das telas do Figma (21/10)
-- =============================================

CREATE TABLE categorias (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nome       VARCHAR(80)  NOT NULL,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_categorias_nome UNIQUE (nome)
);

CREATE TABLE sistemas_equipamentos (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nome       VARCHAR(80)  NOT NULL,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_sistemas_equipamentos_nome UNIQUE (nome)
);

CREATE TABLE solicitantes (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nome       VARCHAR(120),
    telefone   VARCHAR(16),
    email      VARCHAR(254),
    criado_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_solicitantes_email UNIQUE (email),
    CONSTRAINT ck_solicitantes_contato CHECK (telefone IS NOT NULL OR email IS NOT NULL),
    CONSTRAINT ck_solicitantes_telefone_e164 CHECK (telefone ~ '^\+[1-9][0-9]{7,14}$'),
    CONSTRAINT ck_solicitantes_email_minusculo CHECK (email = lower(email))
);

CREATE INDEX idx_solicitantes_telefone ON solicitantes (telefone);

CREATE SEQUENCE chamados_codigo_seq START WITH 1;

CREATE TABLE chamados (
    id                       UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo                   VARCHAR(20)   NOT NULL,
    unidade_id               UUID          NOT NULL REFERENCES unidades (id),
    solicitante_id           UUID          NOT NULL REFERENCES solicitantes (id),
    canal_origem             VARCHAR(20)   NOT NULL,
    descricao                VARCHAR(2000) NOT NULL,
    categoria_id             UUID          REFERENCES categorias (id),
    sistema_equipamento_id   UUID          REFERENCES sistemas_equipamentos (id),
    status                   VARCHAR(20)   NOT NULL DEFAULT 'ABERTO',
    criado_por_id            UUID          NOT NULL REFERENCES usuarios (id),
    responsavel_id           UUID          REFERENCES usuarios (id),
    severidade               SMALLINT,
    urgencia                 SMALLINT,
    impacto_operacional      SMALLINT,
    prioridade_sugerida      VARCHAR(10),
    prioridade_final         VARCHAR(10),
    justificativa_prioridade VARCHAR(500),
    motivo_espera            VARCHAR(30),
    espera_iniciada_em       TIMESTAMPTZ,
    aberto_em                TIMESTAMPTZ   NOT NULL DEFAULT now(),
    registro_iniciado_em     TIMESTAMPTZ,
    primeira_resposta_em     TIMESTAMPTZ,
    resolvido_em             TIMESTAMPTZ,
    fechado_em               TIMESTAMPTZ,
    resolucao                VARCHAR(2000),
    causa_raiz               VARCHAR(1000),
    possivel_recorrencia     BOOLEAN       NOT NULL DEFAULT FALSE,
    motivo_cancelamento      VARCHAR(500),
    chamado_principal_id     UUID          REFERENCES chamados (id),
    origem_dado              VARCHAR(10)   NOT NULL,
    CONSTRAINT uq_chamados_codigo UNIQUE (codigo),
    CONSTRAINT ck_chamados_canal_origem CHECK (canal_origem IN ('LIGACAO', 'PRESENCIAL', 'WHATSAPP', 'EMAIL')),
    CONSTRAINT ck_chamados_status CHECK (status IN ('ABERTO', 'EM_TRIAGEM', 'EM_ATENDIMENTO', 'AGUARDANDO', 'RESOLVIDO', 'REABERTO', 'FECHADO', 'CANCELADO', 'MESCLADO')),
    CONSTRAINT ck_chamados_severidade CHECK (severidade BETWEEN 1 AND 3),
    CONSTRAINT ck_chamados_urgencia CHECK (urgencia BETWEEN 1 AND 3),
    CONSTRAINT ck_chamados_impacto CHECK (impacto_operacional BETWEEN 1 AND 3),
    CONSTRAINT ck_chamados_prioridade_sugerida CHECK (prioridade_sugerida IN ('ALTA', 'MEDIA', 'BAIXA')),
    CONSTRAINT ck_chamados_prioridade_final CHECK (prioridade_final IN ('ALTA', 'MEDIA', 'BAIXA')),
    CONSTRAINT ck_chamados_motivo_espera CHECK (motivo_espera IN ('FORNECEDOR', 'SOLICITANTE', 'PECA_EQUIPAMENTO')),
    CONSTRAINT ck_chamados_origem_dado CHECK (origem_dado IN ('REAL', 'TESTE', 'SIMULADO')),
    CONSTRAINT ck_chamados_espera CHECK ((status = 'AGUARDANDO') = (motivo_espera IS NOT NULL)),
    CONSTRAINT ck_chamados_mesclado CHECK ((status = 'MESCLADO') = (chamado_principal_id IS NOT NULL)),
    CONSTRAINT ck_chamados_principal_diferente CHECK (chamado_principal_id <> id),
    CONSTRAINT ck_chamados_cancelamento CHECK (status <> 'CANCELADO' OR motivo_cancelamento IS NOT NULL),
    CONSTRAINT ck_chamados_resolucao CHECK (status NOT IN ('RESOLVIDO', 'FECHADO') OR resolucao IS NOT NULL),
    CONSTRAINT ck_chamados_justificativa CHECK (prioridade_final IS NULL OR prioridade_final = prioridade_sugerida OR justificativa_prioridade IS NOT NULL)
);

CREATE INDEX idx_chamados_unidade ON chamados (unidade_id);
CREATE INDEX idx_chamados_status ON chamados (status);
CREATE INDEX idx_chamados_responsavel ON chamados (responsavel_id);
CREATE INDEX idx_chamados_aberto_em ON chamados (aberto_em);
CREATE INDEX idx_chamados_categoria ON chamados (categoria_id);

CREATE TABLE registros_contato (
    id                    UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    chamado_id            UUID          NOT NULL REFERENCES chamados (id),
    chamado_original_id   UUID          REFERENCES chamados (id),
    solicitante_id        UUID          NOT NULL REFERENCES solicitantes (id),
    canal                 VARCHAR(20)   NOT NULL,
    direcao               VARCHAR(10)   NOT NULL,
    identificador_externo VARCHAR(255),
    conteudo              VARCHAR(2000),
    ocorrido_em           TIMESTAMPTZ   NOT NULL,
    registrado_por_id     UUID          REFERENCES usuarios (id),
    origem_dado           VARCHAR(10)   NOT NULL,
    CONSTRAINT uq_registros_contato_externo UNIQUE (canal, identificador_externo),
    CONSTRAINT ck_registros_contato_canal CHECK (canal IN ('LIGACAO', 'PRESENCIAL', 'WHATSAPP', 'EMAIL')),
    CONSTRAINT ck_registros_contato_direcao CHECK (direcao IN ('RECEBIDO', 'ENVIADO')),
    CONSTRAINT ck_registros_contato_origem_dado CHECK (origem_dado IN ('REAL', 'TESTE', 'SIMULADO'))
);

CREATE INDEX idx_registros_contato_chamado ON registros_contato (chamado_id);
