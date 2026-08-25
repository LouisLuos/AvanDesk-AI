# 📖 Histórias de Usuário & Tasks — AvanDesk-AI

## Padrão Adotado

### INVEST
Cada User Story segue os critérios **INVEST**:

| Critério | Descrição |
|----------|-----------|
| **I**ndependent | Pode ser desenvolvida sem dependência direta de outras stories. |
| **N**egotiable | Detalhes podem ser negociados durante o refinamento. |
| **V**aluable | Entrega valor perceptível ao usuário ou ao negócio. |
| **E**stimable | É possível estimar o esforço necessário. |
| **S**mall | Pequena o suficiente para ser concluída em uma sprint. |
| **T**estable | Possui critérios de aceite verificáveis. |

### Formato
```
Como [persona], eu quero [ação], para que [benefício].
```

### Estimativas
Story Points em escala Fibonacci: **1, 2, 3, 5, 8, 13**.

---

## 🎫 Epic 1: Gestão de Chamados

---

### US-001 — Abertura de Chamado com Validação Guiada

> **Como** solicitante, **eu quero** abrir um chamado preenchendo um formulário guiado que valida os dados em tempo real, **para que** eu forneça todas as informações necessárias para uma triagem eficiente.

**Prioridade:** Must Have · **Story Points:** 8 · **Sprint:** 1

**Requisitos relacionados:** RF-001, RF-002, RF-010

#### Critérios de Aceite

- [ ] O formulário exibe os campos: título (obrigatório), descrição (obrigatório), categoria (obrigatório, dropdown), anexos (opcional).
- [ ] Campos obrigatórios não preenchidos são destacados em vermelho com mensagem de erro descritiva.
- [ ] A validação ocorre em tempo real (on blur / on change) e também no submit.
- [ ] Após submissão bem-sucedida, o usuário é redirecionado para a tela de detalhes do chamado com mensagem de confirmação.
- [ ] Dados incompletos impedem o envio do formulário.

#### Checklist INVEST

- [x] **Independent:** Funciona isoladamente, não depende de triagem IA.
- [x] **Negotiable:** Campos e validações podem ser ajustados no refinamento.
- [x] **Valuable:** Sem abertura de chamados, o sistema não tem função.
- [x] **Estimable:** Formulário + validação + API = esforço estimável.
- [x] **Small:** Cabe em uma sprint.
- [x] **Testable:** Critérios de aceite verificáveis com testes E2E.

#### Tasks

| ID | Task | Tipo | Estimativa |
|----|------|:----:|:----------:|
| T-001 | Criar componente Angular do formulário de abertura de chamado | Frontend | 3h |
| T-002 | Implementar validação reativa (Reactive Forms + validators customizados) | Frontend | 2h |
| T-003 | Criar endpoint `POST /api/tickets` com validação server-side | Backend | 3h |
| T-004 | Criar migration e entidade `Ticket` no banco de dados | Backend | 2h |
| T-005 | Implementar service de criação de ticket com regras de negócio | Backend | 2h |
| T-006 | Escrever testes unitários para o service de criação | Teste | 2h |
| T-007 | Escrever testes E2E para o fluxo de abertura de chamado | Teste | 3h |
| T-008 | Criar feedback visual (loading, sucesso, erro) no formulário | Frontend | 1h |

---

### US-002 — Listagem e Visualização de Chamados

> **Como** analista, **eu quero** visualizar uma lista de chamados com filtros e acessar os detalhes de cada um, **para que** eu possa identificar rapidamente os chamados que preciso atender.

**Prioridade:** Must Have · **Story Points:** 5 · **Sprint:** 1

**Requisitos relacionados:** RF-003, RF-004

#### Critérios de Aceite

- [ ] A lista exibe: ID, título, status, prioridade, categoria, data de abertura e analista atribuído.
- [ ] É possível filtrar por: status, prioridade, categoria e intervalo de datas.
- [ ] A lista é paginada (20 itens por página, configurável).
- [ ] Clicar em um chamado abre a tela de detalhes com o histórico completo (timeline).
- [ ] A lista atualiza sem reload ao aplicar/remover filtros.

#### Checklist INVEST

- [x] **Independent:** Funciona com dados existentes, sem depender de IA.
- [x] **Negotiable:** Colunas e filtros podem ser ajustados.
- [x] **Valuable:** Permite ao analista gerenciar sua fila de trabalho.
- [x] **Estimable:** CRUD + filtros = escopo claro.
- [x] **Small:** Cabe em uma sprint.
- [x] **Testable:** Filtros e paginação são testáveis com dados mock.

#### Tasks

| ID | Task | Tipo | Estimativa |
|----|------|:----:|:----------:|
| T-009 | Criar endpoint `GET /api/tickets` com paginação e filtros | Backend | 3h |
| T-010 | Criar componente Angular de listagem com tabela e filtros | Frontend | 4h |
| T-011 | Criar componente Angular de detalhes do chamado com timeline | Frontend | 3h |
| T-012 | Criar endpoint `GET /api/tickets/:id` com histórico | Backend | 2h |
| T-013 | Escrever testes unitários para endpoints de listagem e detalhes | Teste | 2h |
| T-014 | Implementar ordenação por colunas na tabela | Frontend | 1h |

---

## 🧠 Epic 2: Triagem Inteligente

---

### US-003 — Triagem Automática com IA

> **Como** analista, **eu quero** que o sistema classifique automaticamente a prioridade do chamado com IA e apresente a justificativa, **para que** eu possa tomar decisões de triagem mais rápidas e fundamentadas.

**Prioridade:** Must Have · **Story Points:** 13 · **Sprint:** 2

**Requisitos relacionados:** RF-009, RF-011, RF-012

#### Critérios de Aceite

- [ ] Ao criar um ticket, a triagem é disparada automaticamente em background.
- [ ] O resultado da triagem exibe: prioridade sugerida (P1–P4), nível de confiança (%) e justificativa textual.
- [ ] O analista pode aceitar a sugestão com um clique ou ajustar manualmente selecionando outra prioridade com justificativa.
- [ ] Em caso de falha da API de IA, o ticket é criado com status `EM_TRIAGEM` e a triagem é enfileirada para retry (máx. 3 tentativas).
- [ ] O tempo de processamento da triagem não excede 10 segundos (p95).

#### Checklist INVEST

- [x] **Independent:** Pode ser desenvolvida após US-001 (que cria o ticket).
- [x] **Negotiable:** Modelo de IA, formato do prompt e campos analisados são negociáveis.
- [x] **Valuable:** Diferencial competitivo — reduz tempo de triagem de minutos para segundos.
- [x] **Estimable:** Integração com API externa + lógica de retry = complexidade mapeável.
- [x] **Small:** A 13 pontos, é a maior story mas ainda cabe em uma sprint.
- [x] **Testable:** Resultado da IA, retry, e aceite/rejeição são testáveis.

#### Tasks

| ID | Task | Tipo | Estimativa |
|----|------|:----:|:----------:|
| T-015 | Projetar e documentar o prompt de triagem para a API de IA | IA/Design | 3h |
| T-016 | Criar adapter/client para a API de IA (Azure OpenAI) com tipagem | Backend | 4h |
| T-017 | Implementar service de triagem com disparo assíncrono após criação do ticket | Backend | 3h |
| T-018 | Implementar mecanismo de retry com backoff exponencial para falhas da API | Backend | 2h |
| T-019 | Criar entidade `TriagemIA` e migration no banco de dados | Backend | 1h |
| T-020 | Criar componente Angular para exibição do resultado da triagem (card com prioridade, confiança, justificativa) | Frontend | 3h |
| T-021 | Implementar ação de aceite/rejeição da sugestão de IA com feedback visual | Frontend | 2h |
| T-022 | Escrever testes unitários para service de triagem (mock da API) | Teste | 3h |
| T-023 | Escrever testes de integração para o fluxo completo de triagem | Teste | 3h |

---

### US-004 — Insights Contextuais para o Analista

> **Como** analista, **eu quero** receber insights contextuais gerados pela IA com base no conteúdo do chamado, **para que** eu possa investigar o problema de forma mais eficiente.

**Prioridade:** Should Have · **Story Points:** 8 · **Sprint:** 3

**Requisitos relacionados:** RF-013, RF-014

#### Critérios de Aceite

- [ ] Após a triagem, o sistema gera insights contextuais exibidos em cards na tela de detalhes do chamado.
- [ ] Os insights incluem: contexto relevante, sugestões de investigação e chamados similares (se houver).
- [ ] Cada insight indica seu tipo (CONTEXTO, SOLUÇÃO_SUGERIDA, HISTÓRICO_SIMILAR).
- [ ] O analista pode marcar insights como "útil" ou "não útil" para feedback.

#### Checklist INVEST

- [x] **Independent:** Funciona sobre o ticket já triado; pode ser entregue separadamente.
- [x] **Negotiable:** Tipos de insight e formato de exibição são negociáveis.
- [x] **Valuable:** Reduz tempo de investigação e melhora qualidade do atendimento.
- [x] **Estimable:** API call + componente de exibição = escopo estimável.
- [x] **Small:** Cabe em uma sprint.
- [x] **Testable:** Geração e exibição de insights são testáveis.

#### Tasks

| ID | Task | Tipo | Estimativa |
|----|------|:----:|:----------:|
| T-024 | Projetar prompt de geração de insights contextuais | IA/Design | 2h |
| T-025 | Implementar service de geração de insights (extensão do adapter de IA) | Backend | 3h |
| T-026 | Criar entidade `Insight` e migration | Backend | 1h |
| T-027 | Criar componente Angular de cards de insights na tela de detalhes | Frontend | 3h |
| T-028 | Implementar mecanismo de feedback (útil/não útil) nos insights | Frontend + Backend | 2h |
| T-029 | Escrever testes unitários para o service de insights | Teste | 2h |

---

## ⚙️ Epic 3: Workflow & Atribuição

---

### US-005 — Fluxo de Status e Atribuição de Chamados

> **Como** analista, **eu quero** gerenciar o status dos chamados e reatribuí-los quando necessário, **para que** o fluxo de atendimento seja organizado e rastreável.

**Prioridade:** Must Have · **Story Points:** 5 · **Sprint:** 2

**Requisitos relacionados:** RF-015, RF-017

#### Critérios de Aceite

- [ ] O status do chamado só pode transicionar conforme o workflow definido (ex.: `ABERTO → EM_TRIAGEM`, não `ABERTO → FECHADO` diretamente).
- [ ] Toda mudança de status é registrada no histórico do chamado com timestamp e usuário.
- [ ] A reatribuição exige seleção de um analista ativo e registra a mudança no histórico.
- [ ] O status atual é exibido com badge colorido na lista e nos detalhes do chamado.

#### Checklist INVEST

- [x] **Independent:** Funciona com tickets existentes.
- [x] **Negotiable:** Regras de transição podem ser ajustadas.
- [x] **Valuable:** Sem workflow, o processo de atendimento é caótico.
- [x] **Estimable:** State machine + UI = esforço mensurável.
- [x] **Small:** Cabe em uma sprint.
- [x] **Testable:** Transições válidas e inválidas são facilmente testáveis.

#### Tasks

| ID | Task | Tipo | Estimativa |
|----|------|:----:|:----------:|
| T-030 | Implementar state machine para transições de status do ticket | Backend | 3h |
| T-031 | Criar endpoint `PATCH /api/tickets/:id/status` com validação de transição | Backend | 2h |
| T-032 | Criar endpoint `PATCH /api/tickets/:id/assign` para reatribuição | Backend | 1h |
| T-033 | Criar entidade `HistoricoTicket` e migration | Backend | 1h |
| T-034 | Criar componente Angular de alteração de status com dropdown e confirmação | Frontend | 2h |
| T-035 | Criar componente Angular de reatribuição com autocomplete de analistas | Frontend | 2h |
| T-036 | Escrever testes unitários para state machine de status | Teste | 2h |

---

### US-006 — Monitoramento de SLA

> **Como** gestor, **eu quero** que o sistema monitore o SLA dos chamados e alerte quando prazos estiverem em risco, **para que** eu possa intervir antes de violações.

**Prioridade:** Should Have · **Story Points:** 8 · **Sprint:** 3

**Requisitos relacionados:** RF-018, RF-019, RF-020

#### Critérios de Aceite

- [ ] Cada prioridade tem um SLA configurável (ex.: P1 = 2h resposta / 8h resolução).
- [ ] O tempo de SLA é exibido no chamado como contagem regressiva ou indicador visual (verde/amarelo/vermelho).
- [ ] Notificações são disparadas quando o SLA atinge 75% e 90% do tempo limite.
- [ ] Chamados com SLA violado são visualmente destacados na lista (badge vermelho).

#### Tasks

| ID | Task | Tipo | Estimativa |
|----|------|:----:|:----------:|
| T-037 | Criar tabela de configuração de SLA por prioridade | Backend | 2h |
| T-038 | Implementar job/scheduler para cálculo contínuo de SLA | Backend | 4h |
| T-039 | Implementar lógica de notificação em thresholds de SLA | Backend | 3h |
| T-040 | Criar componente Angular de indicador visual de SLA no chamado | Frontend | 2h |
| T-041 | Adicionar badge de SLA violado na listagem de chamados | Frontend | 1h |
| T-042 | Escrever testes para job de SLA e notificações | Teste | 3h |

---

## 🔐 Epic 4: Autenticação & Acesso

---

### US-007 — Autenticação e Controle de Acesso

> **Como** usuário do sistema, **eu quero** me autenticar com minhas credenciais e ter acesso apenas às funcionalidades do meu perfil, **para que** a segurança e a privacidade dos dados sejam garantidas.

**Prioridade:** Must Have · **Story Points:** 8 · **Sprint:** 1

**Requisitos relacionados:** RF-021, RF-023

#### Critérios de Aceite

- [ ] O usuário pode fazer login com e-mail e senha.
- [ ] Após login bem-sucedido, um token JWT é retornado e armazenado de forma segura no cliente.
- [ ] Rotas protegidas redirecionam para a tela de login se o token for inválido ou expirado.
- [ ] Solicitantes não acessam funcionalidades de analista (triagem, reatribuição).
- [ ] Analistas não acessam funcionalidades de gestor (configuração de SLA, relatórios gerenciais).
- [ ] Tentativas de login com credenciais inválidas exibem mensagem genérica (sem expor se o e-mail existe).

#### Tasks

| ID | Task | Tipo | Estimativa |
|----|------|:----:|:----------:|
| T-043 | Criar entidade `Usuario` com perfis (SOLICITANTE, ANALISTA, GESTOR) e migration | Backend | 2h |
| T-044 | Implementar endpoint `POST /api/auth/login` com geração de JWT | Backend | 3h |
| T-045 | Implementar middleware/guard de autenticação e autorização (RBAC) | Backend | 3h |
| T-046 | Criar tela de login com formulário e validação | Frontend | 2h |
| T-047 | Implementar AuthGuard no Angular para proteção de rotas | Frontend | 2h |
| T-048 | Implementar interceptor HTTP para injeção automática do token JWT | Frontend | 1h |
| T-049 | Escrever testes para autenticação e autorização | Teste | 3h |

---

## 📊 Epic 5: Dashboard & Visibilidade

---

### US-008 — Dashboard de Métricas

> **Como** gestor, **eu quero** visualizar um dashboard com métricas-chave do suporte, **para que** eu possa tomar decisões baseadas em dados sobre a operação.

**Prioridade:** Should Have · **Story Points:** 8 · **Sprint:** 3

**Requisitos relacionados:** RF-025

#### Critérios de Aceite

- [ ] O dashboard exibe: total de chamados abertos, tempo médio de resolução, taxa de cumprimento de SLA e distribuição por categoria.
- [ ] Gráficos são interativos (hover para detalhes, clique para drill-down na lista filtrada).
- [ ] Dados são atualizados em tempo real ou com refresh automático (intervalo configurável).
- [ ] O dashboard carrega em menos de 2 segundos.

#### Tasks

| ID | Task | Tipo | Estimativa |
|----|------|:----:|:----------:|
| T-050 | Criar endpoints de agregação para métricas do dashboard | Backend | 4h |
| T-051 | Escolher e integrar biblioteca de gráficos (Chart.js ou ng2-charts) | Frontend | 2h |
| T-052 | Criar componente de dashboard com cards de KPIs e gráficos | Frontend | 5h |
| T-053 | Implementar refresh automático com intervalo configurável | Frontend | 1h |
| T-054 | Escrever testes para endpoints de métricas | Teste | 2h |

---

## 🔔 Epic 6: Notificações

---

### US-009 — Notificações de Eventos

> **Como** analista, **eu quero** receber notificações em tempo real quando um chamado for atribuído a mim ou quando houver atualizações relevantes, **para que** eu possa agir rapidamente.

**Prioridade:** Should Have · **Story Points:** 5 · **Sprint:** 3

**Requisitos relacionados:** RF-028, RF-029

#### Critérios de Aceite

- [ ] Analistas recebem notificação in-app ao receber um novo chamado.
- [ ] Solicitantes recebem e-mail ao mudar o status do chamado.
- [ ] Notificações in-app aparecem em tempo real (sem necessidade de refresh).
- [ ] O usuário pode marcar notificações como lidas.

#### Tasks

| ID | Task | Tipo | Estimativa |
|----|------|:----:|:----------:|
| T-055 | Implementar sistema de notificações in-app com WebSocket/SSE | Backend | 4h |
| T-056 | Criar componente Angular de centro de notificações (ícone bell + dropdown) | Frontend | 3h |
| T-057 | Implementar serviço de envio de e-mail (templates para mudança de status) | Backend | 3h |
| T-058 | Criar endpoint de marcação de notificação como lida | Backend | 1h |
| T-059 | Escrever testes para disparo e recebimento de notificações | Teste | 2h |

---

## 📋 Resumo de User Stories por Sprint

### Sprint 1 — Fundação

| Story | Pontos | Prioridade |
|-------|:------:|:----------:|
| US-007 — Autenticação e Controle de Acesso | 8 | Must |
| US-001 — Abertura de Chamado com Validação Guiada | 8 | Must |
| US-002 — Listagem e Visualização de Chamados | 5 | Must |
| **Total Sprint 1** | **21** | |

### Sprint 2 — Inteligência & Workflow

| Story | Pontos | Prioridade |
|-------|:------:|:----------:|
| US-003 — Triagem Automática com IA | 13 | Must |
| US-005 — Fluxo de Status e Atribuição | 5 | Must |
| **Total Sprint 2** | **18** | |

### Sprint 3 — Visibilidade & Operação

| Story | Pontos | Prioridade |
|-------|:------:|:----------:|
| US-004 — Insights Contextuais | 8 | Should |
| US-006 — Monitoramento de SLA | 8 | Should |
| US-008 — Dashboard de Métricas | 8 | Should |
| US-009 — Notificações de Eventos | 5 | Should |
| **Total Sprint 3** | **29** | |

---

## Velocity Estimada

| Métrica | Valor |
|---------|:-----:|
| **Total de Story Points** | **68** |
| **Sprints planejadas** | **3** |
| **Velocity média** | **~23 pts/sprint** |
| **Total de Tasks** | **59** |
