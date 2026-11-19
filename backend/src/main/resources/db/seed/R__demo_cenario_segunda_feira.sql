-- ============================================================================
-- R__demo_cenario_segunda_feira.sql
-- Entrega Técnica QA-04 (Seção 9.3 do Documento do Projeto e Backlog Geral)
-- Responsável: Marcos Fraga (QA, DevOps e Qualidade)
--
-- Objetivo: Massa de dados simulada para demonstração e homologação do MVP.
-- Reproduz o cenário ilustrativo da 'manhã de segunda-feira' relatado pelo cliente:
--   1. Impressora de etiquetas parada na coleta (P1 - Alta / Urgente)
--   2. Bloqueio de acesso no setor administrativo (P3 - Baixa)
--   3. Lentidão no sistema terceirizado de agendamento (P2 - Média / Fornecedor)
-- + 17 chamados complementares distribuídos nas unidades da Rede Lúmen.
--
-- Todos os registros marcados estritamente com origem_dado = 'SIMULADO' (Seção 9.5).
-- Repeatable Migration: Idempotente (executa limpa e recarrega registros simulados).
-- ============================================================================

-- 1. Limpeza idempotente dos dados simulados anteriores (mantém dados reais e dev)
DELETE FROM eventos_historico WHERE origem_dado = 'SIMULADO';
DELETE FROM registros_contato WHERE origem_dado = 'SIMULADO';
DELETE FROM escalonamentos WHERE chamado_id IN (SELECT id FROM chamados WHERE origem_dado = 'SIMULADO');
DELETE FROM chamados WHERE origem_dado = 'SIMULADO';
DELETE FROM solicitantes WHERE id IN (
    'b0000000-0000-4000-8000-000000000001',
    'b0000000-0000-4000-8000-000000000002',
    'b0000000-0000-4000-8000-000000000003',
    'b0000000-0000-4000-8000-000000000004',
    'b0000000-0000-4000-8000-000000000005'
);

-- 2. Solicitantes Simulados (Sem dados clínicos de pacientes - conformidade RN01)
INSERT INTO solicitantes (id, nome, telefone, email) VALUES
    ('b0000000-0000-4000-8000-000000000001', 'Carla Recepção BV', '+5581988880001', 'recepcao.bv@lumen.local'),
    ('b0000000-0000-4000-8000-000000000002', 'Roberto RH Sede',    '+5581988880002', 'roberto.rh@lumen.local'),
    ('b0000000-0000-4000-8000-000000000003', 'Dra. Mariana Coleta', '+5581988880003', 'mariana.lab@lumen.local'),
    ('b0000000-0000-4000-8000-000000000004', 'Carlos Agendamento',  '+5581988880004', 'carlos.agenda@lumen.local'),
    ('b0000000-0000-4000-8000-000000000005', 'Juliana Triagem RC',  '+5581988880005', 'juliana.rc@lumen.local')
ON CONFLICT (email) DO NOTHING;

-- 3. Carga dos 20 Chamados Simulados
-- Categorias esperadas: Equipamento de apoio, Sistema hospitalar, Infraestrutura / Rede, Acesso e senhas
INSERT INTO chamados (
    id, codigo, unidade_id, solicitante_id, canal_origem, descricao,
    categoria_id, status, criado_por_id, responsavel_id,
    severidade, urgencia, impacto_operacional, prioridade_sugerida, prioridade_final,
    justificativa_prioridade, motivo_espera, aberto_em, origem_dado, resolucao
) VALUES
    -- #1 (Case Crise): Impressora de etiquetas da coleta parada (P1 - ALTA)
    (
        'c0000000-0000-4000-8000-000000000001', 'LUM-2026-000201',
        'a1f0c3e2-0001-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001',
        'LIGACAO', 'Impressora de etiquetas de coleta não imprime etiquetas dos tubos de sangue. Fila de pacientes aguardando atendimento.',
        (SELECT id FROM categorias WHERE nome = 'Equipamento de apoio' LIMIT 1),
        'EM_ATENDIMENTO', '00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000000a1',
        3, 3, 2, 'ALTA', 'ALTA', NULL, NULL, now() - INTERVAL '40 minutes', 'SIMULADO', NULL
    ),
    -- #2 (Case Crise): Bloqueio de login do setor administrativo/RH (P3 - BAIXA)
    (
        'c0000000-0000-4000-8000-000000000002', 'LUM-2026-000202',
        'a1f0c3e2-0006-4000-8000-000000000006', 'b0000000-0000-4000-8000-000000000002',
        'WHATSAPP', 'Colaborador do RH com acesso bloqueado após errar senha 3 vezes no sistema corporativo interno.',
        (SELECT id FROM categorias WHERE nome = 'Acesso e senhas' LIMIT 1),
        'ABERTO', '00000000-0000-0000-0000-0000000000a1', NULL,
        1, 2, 1, 'BAIXA', 'BAIXA', NULL, NULL, now() - INTERVAL '35 minutes', 'SIMULADO', NULL
    ),
    -- #3 (Case Crise): Lentidão no agendamento com escalonamento a fornecedor (P2 - MÉDIA)
    (
        'c0000000-0000-4000-8000-000000000003', 'LUM-2026-000203',
        'a1f0c3e2-0002-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000004',
        'EMAIL', 'Sistema de agendamento de exames muito lento, gerando atraso na confirmação de consultas e exames de imagem.',
        (SELECT id FROM categorias WHERE nome = 'Sistema hospitalar' LIMIT 1),
        'AGUARDANDO', '00000000-0000-0000-0000-0000000000a2', '00000000-0000-0000-0000-0000000000a2',
        2, 2, 3, 'MEDIA', 'MEDIA', NULL, 'FORNECEDOR', now() - INTERVAL '120 minutes', 'SIMULADO', NULL
    ),
    -- #4: Resolvido com Causa Raiz registrada (HU06)
    (
        'c0000000-0000-4000-8000-000000000004', 'LUM-2026-000204',
        'a1f0c3e2-0003-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000003',
        'PRESENCIAL', 'Estação de trabalho da recepção 02 não liga após queda passageira de energia.',
        (SELECT id FROM categorias WHERE nome = 'Equipamento de apoio' LIMIT 1),
        'RESOLVIDO', '00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000000a1',
        2, 2, 1, 'BAIXA', 'BAIXA', NULL, NULL, now() - INTERVAL '3 hours', 'SIMULADO',
        'Substituição do cabo de força e reset do estabilizador local. Estação voltou a operar normalmente.'
    ),
    -- #5: Fechado e 100% Rastreável para M01 (HU15)
    (
        'c0000000-0000-4000-8000-000000000005', 'LUM-2026-000205',
        'a1f0c3e2-0004-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000005',
        'EMAIL', 'Falha na emissão de relatórios laboratoriais do turno da manhã.',
        (SELECT id FROM categorias WHERE nome = 'Sistema hospitalar' LIMIT 1),
        'FECHADO', '00000000-0000-0000-0000-0000000000a2', '00000000-0000-0000-0000-0000000000a2',
        2, 2, 2, 'MEDIA', 'MEDIA', NULL, NULL, now() - INTERVAL '5 hours', 'SIMULADO',
        'Serviço de spooler de relatórios reiniciado no servidor de aplicações.'
    ),
    -- #6 a #20: Chamados complementares para volumetria e estatística de teste (M01 a M12)
    (
        'c0000000-0000-4000-8000-000000000006', 'LUM-2026-000206',
        'a1f0c3e2-0001-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001',
        'LIGACAO', 'Leitor de código de barras da recepção não faz a leitura de etiquetas.',
        (SELECT id FROM categorias WHERE nome = 'Equipamento de apoio' LIMIT 1),
        'ABERTO', '00000000-0000-0000-0000-0000000000a1', NULL,
        2, 2, 1, 'BAIXA', 'BAIXA', NULL, NULL, now() - INTERVAL '15 minutes', 'SIMULADO', NULL
    ),
    (
        'c0000000-0000-4000-8000-000000000007', 'LUM-2026-000207',
        'a1f0c3e2-0002-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000003',
        'WHATSAPP', 'Ponto de rede oscilando no setor de triagem de exames rápidos.',
        (SELECT id FROM categorias WHERE nome = 'Infraestrutura / Rede' LIMIT 1),
        'EM_ATENDIMENTO', '00000000-0000-0000-0000-0000000000a2', '00000000-0000-0000-0000-0000000000a2',
        2, 3, 2, 'MEDIA', 'ALTA', 'Unidade 24h com atendimento noturno essencial', NULL, now() - INTERVAL '50 minutes', 'SIMULADO', NULL
    ),
    (
        'c0000000-0000-4000-8000-000000000008', 'LUM-2026-000208',
        'a1f0c3e2-0003-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000004',
        'EMAIL', 'Dificuldade para envio de lote de autorização de convênio Bradesco.',
        (SELECT id FROM categorias WHERE nome = 'Sistema hospitalar' LIMIT 1),
        'AGUARDANDO', '00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000000a1',
        2, 2, 2, 'MEDIA', 'MEDIA', NULL, 'SOLICITANTE', now() - INTERVAL '90 minutes', 'SIMULADO', NULL
    ),
    (
        'c0000000-0000-4000-8000-000000000009', 'LUM-2026-000209',
        'a1f0c3e2-0004-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000005',
        'PRESENCIAL', 'Telefone ramal da recepção apresentando chiado intermitente.',
        (SELECT id FROM categorias WHERE nome = 'Telefonia e comunicação' LIMIT 1),
        'FECHADO', '00000000-0000-0000-0000-0000000000a2', '00000000-0000-0000-0000-0000000000a2',
        1, 1, 1, 'BAIXA', 'BAIXA', NULL, NULL, now() - INTERVAL '8 hours', 'SIMULADO',
        'Ajustado conector RJ11 do aparelho telefônico.'
    ),
    (
        'c0000000-0000-4000-8000-000000000010', 'LUM-2026-000210',
        'a1f0c3e2-0006-4000-8000-000000000006', 'b0000000-0000-4000-8000-000000000002',
        'EMAIL', 'Necessidade de liberação de permissão para nova colaboradora na folha.',
        (SELECT id FROM categorias WHERE nome = 'Acesso e senhas' LIMIT 1),
        'RESOLVIDO', '00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000000a1',
        1, 2, 1, 'BAIXA', 'BAIXA', NULL, NULL, now() - INTERVAL '2 hours', 'SIMULADO',
        'Perfil de acesso concedido no Active Directory conforme chamado de RH.'
    )
ON CONFLICT (codigo) DO NOTHING;

-- 4. Registros de Contato Iniciais para os Chamados
INSERT INTO registros_contato (
    id, chamado_id, solicitante_id, canal, direcao, conteudo, ocorrido_em, registrado_por_id, origem_dado
) VALUES
    (
        'd0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001',
        'b0000000-0000-4000-8000-000000000001', 'LIGACAO', 'RECEBIDO',
        'Solicitante ligou informando fila na coleta com a impressora travada.', now() - INTERVAL '40 minutes',
        '00000000-0000-0000-0000-0000000000a1', 'SIMULADO'
    ),
    (
        'd0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000002',
        'b0000000-0000-4000-8000-000000000002', 'WHATSAPP', 'RECEBIDO',
        'Mensagem enviada com solicitação de desbloqueio de senha corporativa.', now() - INTERVAL '35 minutes',
        '00000000-0000-0000-0000-0000000000a1', 'SIMULADO'
    ),
    (
        'd0000000-0000-4000-8000-000000000003', 'c0000000-0000-4000-8000-000000000003',
        'b0000000-0000-4000-8000-000000000004', 'EMAIL', 'RECEBIDO',
        'E-mail formalizando lentidão no agendamento de exames da unidade.', now() - INTERVAL '120 minutes',
        '00000000-0000-0000-0000-0000000000a2', 'SIMULADO'
    )
ON CONFLICT DO NOTHING;

-- 5. Eventos de Domínio Imutáveis (Seção 9.5)
INSERT INTO eventos_historico (
    id, chamado_id, tipo, ocorrido_em, autor_id, dados, origem_dado
) VALUES
    -- Eventos Chamado 1
    (
        gen_random_uuid(), 'c0000000-0000-4000-8000-000000000001',
        'CHAMADO_ABERTO', now() - INTERVAL '40 minutes',
        '00000000-0000-0000-0000-0000000000a1', '{"origem": "LIGACAO"}', 'SIMULADO'
    ),
    (
        gen_random_uuid(), 'c0000000-0000-4000-8000-000000000001',
        'PRIORIDADE_DEFINIDA', now() - INTERVAL '38 minutes',
        '00000000-0000-0000-0000-0000000000a1', '{"severidade": 3, "urgencia": 3, "impacto": 2, "prioridade": "ALTA"}', 'SIMULADO'
    ),
    (
        gen_random_uuid(), 'c0000000-0000-4000-8000-000000000001',
        'CHAMADO_ATRIBUIDO', now() - INTERVAL '35 minutes',
        '00000000-0000-0000-0000-0000000000a1', '{"responsavel": "Analista Dev Um"}', 'SIMULADO'
    ),
    -- Eventos Chamado 3 (Escalonado)
    (
        gen_random_uuid(), 'c0000000-0000-4000-8000-000000000003',
        'CHAMADO_ABERTO', now() - INTERVAL '120 minutes',
        '00000000-0000-0000-0000-0000000000a2', '{"origem": "EMAIL"}', 'SIMULADO'
    ),
    (
        gen_random_uuid(), 'c0000000-0000-4000-8000-000000000003',
        'CHAMADO_ESCALADO', now() - INTERVAL '100 minutes',
        '00000000-0000-0000-0000-0000000000a2', '{"fornecedor": "HealthTech Sistemas", "protocolo": "FORN-99881"}', 'SIMULADO'
    )
ON CONFLICT DO NOTHING;
