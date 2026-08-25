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
| RF-001 | O sistema deve permitir que o solicitante abra um novo chamado preenchendo título, descrição, categoria e anexos opcionais. | **M** | Formulário guiado com validação em tempo real. |
| RF-002 | O sistema deve validar a completude dos dados do chamado antes da submissão, indicando campos obrigatórios não preenchidos. | **M** | Validação client-side + server-side. |
| RF-003 | O sistema deve exibir uma lista de chamados com filtros por status, prioridade, categoria e data. | **M** | Paginação e ordenação. |
| RF-004 | O sistema deve permitir a visualização detalhada de um chamado, incluindo histórico de interações. | **M** | Timeline cronológica de eventos. |
| RF-005 | O sistema deve permitir a edição de dados do chamado enquanto ele estiver nos status `ABERTO` ou `PENDENTE`. | **M** | — |
| RF-006 | O sistema deve permitir que analistas adicionem comentários internos (visíveis apenas para a equipe) e comentários públicos (visíveis ao solicitante). | **S** | — |
| RF-007 | O sistema deve permitir a anexação de arquivos (imagens, logs, documentos) ao chamado. | **S** | Limite de tamanho configurável. |
| RF-008 | O sistema deve permitir a reabertura de chamados fechados dentro de um período configurável. | **C** | Default: 7 dias. |

---

## 2. Módulo: Triagem Inteligente (IA)

| ID | Requisito | Prioridade | Observações |
|----|-----------|:----------:|-------------|
| RF-009 | O sistema deve analisar automaticamente o conteúdo do chamado após sua criação e sugerir um nível de prioridade (P1–P4). | **M** | Integração com serviço de IA (provável Azure OpenAI). |
| RF-010 | O sistema deve identificar lacunas de contexto no chamado e listar as informações faltantes ao solicitante. | **M** | Prompt guiado para o solicitante complementar. |
| RF-011 | O sistema deve apresentar a justificativa da prioridade sugerida pela IA, incluindo o nível de confiança. | **M** | Transparência da decisão para o analista. |
| RF-012 | O sistema deve permitir que o analista aceite, ajuste ou rejeite a prioridade sugerida pela IA. | **M** | Registro da decisão final com justificativa. |
| RF-013 | O sistema deve gerar insights contextuais para o analista com base no conteúdo do chamado e histórico de chamados similares. | **S** | Análise de similaridade semântica. |
| RF-014 | O sistema deve sugerir artigos da base de conhecimento relacionados ao chamado. | **C** | Requer base de conhecimento populada. |

---

## 3. Módulo: Workflow & SLA

| ID | Requisito | Prioridade | Observações |
|----|-----------|:----------:|-------------|
| RF-015 | O sistema deve gerenciar o fluxo de estados do chamado: `ABERTO → EM_TRIAGEM → EM_ANDAMENTO → PENDENTE → RESOLVIDO → FECHADO`. | **M** | Transições validadas por regras de negócio. |
| RF-016 | O sistema deve atribuir automaticamente chamados a analistas com base em categoria e disponibilidade. | **S** | Algoritmo round-robin ou baseado em carga. |
| RF-017 | O sistema deve permitir a reatribuição manual de chamados entre analistas. | **M** | Registro em histórico. |
| RF-018 | O sistema deve calcular e monitorar o SLA de cada chamado com base em sua prioridade. | **S** | Tabela de SLA por prioridade configurável. |
| RF-019 | O sistema deve notificar analistas e gestores quando o SLA estiver próximo de ser violado. | **S** | Thresholds configuráveis (ex.: 75%, 90%). |
| RF-020 | O sistema deve escalonar automaticamente chamados para o nível superior quando o SLA for violado. | **C** | N1 → N2 → N3. |

---

## 4. Módulo: Identidade & Acesso

| ID | Requisito | Prioridade | Observações |
|----|-----------|:----------:|-------------|
| RF-021 | O sistema deve permitir autenticação de usuários com e-mail e senha. | **M** | Senhas com hash bcrypt/argon2. |
| RF-022 | O sistema deve suportar autenticação via provedor corporativo (SSO/OAuth 2.0). | **C** | Integração com Azure AD (provável). |
| RF-023 | O sistema deve implementar controle de acesso baseado em perfis (RBAC): Solicitante, Analista e Gestor. | **M** | Permissões granulares por perfil. |
| RF-024 | O sistema deve registrar todas as ações significativas em log de auditoria. | **S** | Quem, o quê, quando. |

---

## 5. Módulo: Dashboard & Relatórios

| ID | Requisito | Prioridade | Observações |
|----|-----------|:----------:|-------------|
| RF-025 | O sistema deve exibir um dashboard com métricas-chave: chamados abertos, tempo médio de resolução, taxa de SLA cumprido, distribuição por categoria. | **S** | Gráficos interativos. |
| RF-026 | O sistema deve permitir a exportação de relatórios em formato CSV/PDF. | **C** | — |
| RF-027 | O sistema deve exibir métricas de acurácia da triagem de IA (% de sugestões aceitas). | **C** | Feedback loop para melhoria do modelo. |

---

## 6. Módulo: Notificações

| ID | Requisito | Prioridade | Observações |
|----|-----------|:----------:|-------------|
| RF-028 | O sistema deve enviar notificações por e-mail ao solicitante sobre mudanças de status do chamado. | **S** | Templates configuráveis. |
| RF-029 | O sistema deve enviar notificações in-app para analistas sobre novos chamados atribuídos. | **M** | Real-time (WebSocket ou SSE). |
| RF-030 | O sistema deve permitir que usuários configurem suas preferências de notificação. | **C** | — |

---

## Rastreabilidade: Requisitos → User Stories

| Requisito | User Story |
|-----------|------------|
| RF-001, RF-002, RF-010 | US-001 |
| RF-003, RF-004 | US-002 |
| RF-009, RF-011, RF-012 | US-003 |
| RF-013, RF-014 | US-004 |
| RF-015, RF-017 | US-005 |
| RF-018, RF-019, RF-020 | US-006 |
| RF-021, RF-023 | US-007 |
| RF-025 | US-008 |
| RF-028, RF-029 | US-009 |

> 📎 Ver detalhes completos em [04-historias-de-usuario.md](./04-historias-de-usuario.md)
