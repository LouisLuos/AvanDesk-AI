# ✅ Requisitos Funcionais — AvanDesk-AI

## Legenda de Prioridade (MoSCoW)

| Sigla | Significado | Descrição |
|-------|-------------|-----------|
| **M** | Must Have | Essencial para o MVP — sem isso o sistema não funciona |
| **S** | Should Have | Importante, mas o sistema opera sem isso no primeiro release |
| **C** | Could Have | Desejável, agrega valor mas pode ser adiado |
| **W** | Won't Have (por ora) | Fora do escopo atual, candidato a releases futuros |

---

## 1. Módulo: Gestão de Chamados

| ID | Requisito | Prioridade | Observações |
|----|-----------|:----------:|-------------|
| RF-001 | O sistema deve permitir que o solicitante abra um novo chamado preenchendo título, descrição, categoria, unidade e anexos opcionais. | **M** | Formulário guiado com validação em tempo real e associação obrigatória a uma unidade da Rede Lúmen. |
| RF-002 | O sistema deve validar a completude dos dados do chamado antes da submissão, indicando campos obrigatórios não preenchidos. | **M** | Validação client-side + server-side. |
| RF-003 | O sistema deve exibir uma lista de chamados com filtros por status, prioridade, categoria, unidade e data. | **M** | Paginação e ordenação. |
| RF-004 | O sistema deve permitir a visualização detalhada de um chamado, incluindo histórico cronológico de interações. | **M** | Timeline de contatos, trocas de status e eventos. |
| RF-005 | O sistema deve permitir a edição de dados do chamado enquanto ele estiver nos status `ABERTO` ou `PENDENTE`. | **M** | — |
| RF-006 | O sistema deve permitir que analistas adicionem comentários internos (visíveis apenas para a equipe) e notas técnicas ao chamado. | **S** | Histórico compartilhado entre analistas. |
| RF-007 | O sistema deve permitir a anexação de arquivos (imagens, logs, documentos) ao chamado com tratamento de evidência sensível. | **S** | Evidências ocultas por padrão na tela com log de auditoria de visualização. |
| RF-008 | O sistema deve permitir a reabertura de chamados fechados dentro de um período configurável (default: 7 dias). | **C** | Reabertura contestada pelo solicitante pelo canal de origem. |
| RF-031 | O sistema deve gerar um identificador exclusivo e legível para cada chamado no padrão `LUM-AAAA-NNNNNN` (ex.: `LUM-2026-000123`). | **M** | Código informado ao solicitante para acompanhamento. |
| RF-032 | O sistema deve permitir a agregação de múltiplos Registros de Contato a um mesmo chamado. | **M** | Evita múltiplos chamados independentes para o mesmo incidente. |
| RF-033 | O sistema deve permitir a **Mesclagem** de chamados duplicados, onde o secundário transita para `MESCLADO` e referencia o chamado principal. | **M** | Contatos e evidências migram para o chamado principal. |
| RF-034 | O sistema **não deve exigir nem armazenar prontuários ou resultados de exames de pacientes** em nenhum campo. | **M** | Restrição mandatória de segurança da informação e LGPD. |

---

## 2. Módulo: Triagem Inteligente (IA)

| ID | Requisito | Prioridade | Observações |
|----|-----------|:----------:|-------------|
| RF-009 | O sistema deve analisar automaticamente o conteúdo do chamado após sua criação e sugerir um nível de prioridade (P1–P4). | **M** | Integração com serviço de IA (Azure OpenAI) e Matriz 3D (Severidade + Urgência + Impacto). |
| RF-010 | O sistema deve identificar lacunas de contexto no chamado e listar as informações faltantes ao solicitante. | **M** | Prompt guiado para o solicitante complementar dados contextuais. |
| RF-011 | O sistema deve apresentar a justificativa da prioridade sugerida pela IA, incluindo o nível de confiança e critérios da Matriz 3D. | **M** | Transparência da decisão para o analista de suporte. |
| RF-012 | O sistema deve permitir que o analista aceite, ajuste ou rejeite a prioridade sugerida pela IA (com justificativa técnica obrigatória se divergir). | **M** | Arbitragem humana final da prioridade pelo analista. |
| RF-013 | O sistema deve gerar insights contextuais para o analista com base no conteúdo do chamado e histórico de chamados similares. | **S** | Análise de similaridade semântica. |
| RF-014 | O sistema deve sugerir artigos da base de conhecimento relacionados ao chamado. | **C** | Requer base de conhecimento populada. |

---

## 3. Módulo: Workflow & SLA

| ID | Requisito | Prioridade | Observações |
|----|-----------|:----------:|-------------|
| RF-015 | O sistema deve gerenciar o fluxo de estados do chamado: `ABERTO → EM_TRIAGEM → EM_ANDAMENTO → PENDENTE → RESOLVIDO → FECHADO`. | **M** | Transições validadas por regras de negócio (com suporte aos estados finais `MESCLADO` e `CANCELADO`). |
| RF-016 | O sistema deve atribuir automaticamente chamados a analistas com base em categoria e disponibilidade. | **S** | Algoritmo round-robin ou baseado em carga. |
| RF-017 | O sistema deve permitir a reatribuição manual de chamados entre analistas. | **M** | Registro em histórico. |
| RF-018 | O sistema deve calcular e monitorar os prazos de atendimento de cada chamado com base em sua prioridade. | **S** | Tabela de prazos operacionais como referência interna de acompanhamento. |
| RF-019 | O sistema deve notificar analistas e gestores quando o prazo de atendimento estiver próximo de ser atingido. | **S** | Thresholds configuráveis (ex.: 75%, 90%). |
| RF-020 | O sistema deve permitir o escalonamento para fornecedores externos quando a resolução depender de terceiros contratados. | **C** | Registro de protocolo externo, parceiro e acompanhamento pelo suporte interno. |
| RF-035 | O sistema deve permitir o registro da resolução aplicada e, quando identificada, da causa raiz da falha técnica. | **M** | Preservação de conhecimento para identificar problemas recorrentes. |

---

## 4. Módulo: Canais de Entrada

| ID | Requisito | Prioridade | Observações |
|----|-----------|:----------:|-------------|
| RF-036 | O sistema deve permitir o registro de chamados originados de atendimento presencial ou ligação telefônica pelo analista. | **M** | Entrada manual no sistema. |
| RF-037 | O sistema deve integrar-se com chatbot de WhatsApp para recebimento de relatos e criação de chamados. | **S** | Coleta guiada de identificação, unidade e relato sem transcrição de áudio. |
| RF-038 | O sistema deve integrar-se com leitor de e-mail para processamento automático de solicitações enviadas à caixa do suporte. | **S** | Identificação por e-mail corporativo. |
| RF-039 | O sistema deve detectar e descartar automaticamente respostas de e-mail do tipo `Auto-Submitted`, evitando a abertura indevida de tickets. | **M** | Prevenção de loops de correio eletrônico. |
| RF-040 | O sistema deve garantir a idempotência no recebimento de mensagens externas utilizando o identificador externo do canal. | **M** | Previne duplicidade de chamados por retentativas de envio. |

---

## 5. Módulo: Identidade & Acesso

| ID | Requisito | Prioridade | Observações |
|----|-----------|:----------:|-------------|
| RF-021 | O sistema deve permitir autenticação de usuários com e-mail e senha. | **M** | Senhas com hash bcrypt/argon2. |
| RF-022 | O sistema deve suportar autenticação via provedor corporativo (SSO/OAuth 2.0). | **C** | Integração com Azure AD (provável). |
| RF-023 | O sistema deve implementar controle de acesso baseado em perfis (RBAC): Solicitante, Analista e Gestor/Liderança. | **M** | Permissões granulares por perfil; perfil Gestor sem acesso a anexos de evidências técnicas. |
| RF-024 | O sistema deve registrar todas as ações significativas e visualizações de evidências em log de auditoria. | **S** | Quem, o quê, quando e IP. |

---

## 6. Módulo: Dashboard & Relatórios

| ID | Requisito | Prioridade | Observações |
|----|-----------|:----------:|-------------|
| RF-025 | O sistema deve exibir um dashboard com métricas-chave: chamados abertos, tempo médio de resolução, taxa de cumprimento de prazos, distribuição por categoria e unidade. | **S** | Gráficos interativos para a Liderança de TI. |
| RF-026 | O sistema deve permitir a exportação de relatórios em formato CSV/PDF. | **C** | — |
| RF-027 | O sistema deve exibir métricas de acurácia da triagem de IA (% de sugestões aceitas). | **C** | Feedback loop para melhoria do modelo. |
| RF-041 | O sistema deve calcular a métrica principal: **% de chamados encerrados com histórico completo e rastreável**. | **M** | Métrica de sucesso do produto. |

---

## 7. Módulo: Notificações

| ID | Requisito | Prioridade | Observações |
|----|-----------|:----------:|-------------|
| RF-028 | O sistema deve enviar notificações por e-mail ou WhatsApp ao solicitante sobre mudanças de status do chamado. | **S** | Preferencialmente pelo canal de origem do solicitante. |
| RF-029 | O sistema deve enviar notificações in-app para analistas sobre novos chamados atribuídos. | **M** | Real-time (WebSocket ou SSE). |
| RF-030 | O sistema deve permitir que usuários configurem suas preferências de notificação. | **C** | — |

---

## Rastreabilidade: Requisitos → User Stories

| Requisito | User Story |
|-----------|------------|
| RF-001, RF-002, RF-010, RF-031, RF-034, RF-036 | US-001 |
| RF-003, RF-004, RF-006, RF-007, RF-032 | US-002 |
| RF-009, RF-011, RF-012 | US-003 |
| RF-013, RF-014 | US-004 |
| RF-005, RF-015, RF-017, RF-033, RF-035 | US-005 |
| RF-018, RF-019, RF-020 | US-006 |
| RF-021, RF-023, RF-024 | US-007 |
| RF-025, RF-026, RF-027, RF-041 | US-008 |
| RF-028, RF-029, RF-030 | US-009 |
| RF-037, RF-038, RF-039, RF-040 | Adaptação de Canais de Entrada |

> 📎 Ver detalhes completos em [04-historias-de-usuario.md](./04-historias-de-usuario.md)
