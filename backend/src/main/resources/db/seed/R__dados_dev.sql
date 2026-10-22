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
