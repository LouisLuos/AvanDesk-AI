ALTER TABLE solicitantes
    ADD CONSTRAINT ck_solicitantes_email_obrigatorio CHECK (email IS NOT NULL) NOT VALID;

CREATE TABLE notificacoes_email (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    chamado_id           UUID         NOT NULL REFERENCES chamados (id),
    modelo               VARCHAR(20)  NOT NULL,
    status_anterior      VARCHAR(20),
    status_novo          VARCHAR(20)  NOT NULL,
    motivo_espera        VARCHAR(30),
    situacao             VARCHAR(12)  NOT NULL DEFAULT 'PENDENTE',
    tentativas           SMALLINT     NOT NULL DEFAULT 0,
    ultimo_erro          VARCHAR(500),
    proxima_tentativa_em TIMESTAMPTZ  DEFAULT now(),
    enviada_em           TIMESTAMPTZ,
    criada_em            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    origem_dado          VARCHAR(10)  NOT NULL,
    CONSTRAINT ck_notificacoes_email_modelo CHECK (modelo IN ('ABERTURA', 'MUDANCA_STATUS')),
    CONSTRAINT ck_notificacoes_email_status_anterior CHECK (status_anterior IN (
        'ABERTO', 'EM_TRIAGEM', 'EM_ATENDIMENTO', 'AGUARDANDO', 'RESOLVIDO', 'REABERTO', 'FECHADO', 'CANCELADO', 'MESCLADO')),
    CONSTRAINT ck_notificacoes_email_status_novo CHECK (status_novo IN (
        'ABERTO', 'EM_TRIAGEM', 'EM_ATENDIMENTO', 'AGUARDANDO', 'RESOLVIDO', 'REABERTO', 'FECHADO', 'CANCELADO', 'MESCLADO')),
    CONSTRAINT ck_notificacoes_email_motivo_espera CHECK (motivo_espera IN ('FORNECEDOR', 'SOLICITANTE', 'PECA_EQUIPAMENTO')),
    CONSTRAINT ck_notificacoes_email_situacao CHECK (situacao IN ('PENDENTE', 'ENVIADA', 'FALHOU', 'ESGOTADA')),
    CONSTRAINT ck_notificacoes_email_origem_dado CHECK (origem_dado IN ('REAL', 'TESTE', 'SIMULADO')),
    CONSTRAINT ck_notificacoes_email_tentativas CHECK (tentativas >= 0),
    CONSTRAINT ck_notificacoes_email_anterior_na_mudanca CHECK ((modelo = 'MUDANCA_STATUS') = (status_anterior IS NOT NULL)),
    CONSTRAINT ck_notificacoes_email_espera CHECK ((status_novo = 'AGUARDANDO') = (motivo_espera IS NOT NULL)),
    CONSTRAINT ck_notificacoes_email_enviada CHECK ((situacao = 'ENVIADA') = (enviada_em IS NOT NULL)),
    CONSTRAINT ck_notificacoes_email_agendada CHECK ((situacao IN ('PENDENTE', 'FALHOU')) = (proxima_tentativa_em IS NOT NULL))
);

CREATE INDEX idx_notificacoes_email_chamado ON notificacoes_email (chamado_id, criada_em);

CREATE INDEX idx_notificacoes_email_reenvio
    ON notificacoes_email (proxima_tentativa_em)
    WHERE situacao IN ('PENDENTE', 'FALHOU');
