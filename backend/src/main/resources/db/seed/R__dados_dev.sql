-- =============================================
-- R__dados_dev.sql
-- Dados so de desenvolvimento. Nunca carregados em prod
-- (application-prod.yml nao inclui db/seed).
-- Repeatable: o Flyway roda de novo quando o conteudo muda.
-- Senha de dev dos usuarios: ver backend/README.md
-- =============================================

INSERT INTO usuarios (id, nome, email, senha_hash, perfil) VALUES
    ('00000000-0000-0000-0000-0000000000a1', 'Analista Dev Um',   'analista1@lumen.local', '$2a$10$wP24GU5mL0ysN//Ga3xCOusMZmd/oxAXXqlRX4XQtK7LUhWPLYAa.', 'ANALISTA_SUPORTE'),
    ('00000000-0000-0000-0000-0000000000a2', 'Analista Dev Dois', 'analista2@lumen.local', '$2a$10$wP24GU5mL0ysN//Ga3xCOusMZmd/oxAXXqlRX4XQtK7LUhWPLYAa.', 'ANALISTA_SUPORTE'),
    ('00000000-0000-0000-0000-0000000000a3', 'Lideranca Dev',     'lideranca@lumen.local', '$2a$10$wP24GU5mL0ysN//Ga3xCOusMZmd/oxAXXqlRX4XQtK7LUhWPLYAa.', 'LIDERANCA_TI')
ON CONFLICT (id) DO NOTHING;

-- Unidades iniciais de desenvolvimento para abertura de chamados (HU11 / HU01)
INSERT INTO unidades (id, nome, identificacao, localizacao, atendimento_24h, ativo) VALUES
    ('a1f0c3e2-0001-4000-8000-000000000001', 'U.03 Boa Viagem',     'LUM-BVG', 'Rua Boa Viagem, 123',          FALSE, TRUE),
    ('a1f0c3e2-0002-4000-8000-000000000002', 'U.07 Recife Centro',  'LUM-RCT', 'Av Agamenon Magalhaes, 400',   TRUE,  TRUE),
    ('a1f0c3e2-0003-4000-8000-000000000003', 'U.11 Boa Vista',      'LUM-BVT', 'Rua da Aurora, 50',            FALSE, TRUE),
    ('a1f0c3e2-0004-4000-8000-000000000004', 'U.14 Jaboatão',       'LUM-JBT', 'Av Barreto de Menezes, 200',   FALSE, TRUE),
    ('a1f0c3e2-0006-4000-8000-000000000006', 'Sede Administrativa', 'LUM-SED', 'Rua do Bom Jesus, 10',         FALSE, TRUE),
    ('a1f0c3e2-0005-4000-8000-000000000005', 'U.20 Caruaru',        'LUM-CRU', NULL,                           FALSE, FALSE)
ON CONFLICT (identificacao) DO NOTHING;

-- Listas provisorias, iguais as do codigo do front, ate a Sarah e o Pedro revisarem
INSERT INTO categorias (nome) VALUES
    ('Equipamento de apoio'),
    ('Sistema hospitalar'),
    ('Infraestrutura / Rede'),
    ('Acesso e senhas'),
    ('Telefonia e comunicação'),
    ('Outro')
ON CONFLICT (nome) DO NOTHING;

INSERT INTO sistemas_equipamentos (nome) VALUES
    ('Impressora de etiquetas'),
    ('Impressora comum'),
    ('Computador da recepção'),
    ('Sistema de agendamento'),
    ('Portal de resultados'),
    ('Rede / Wi-Fi'),
    ('Telefone / ramal'),
    ('Outro')
ON CONFLICT (nome) DO NOTHING;
