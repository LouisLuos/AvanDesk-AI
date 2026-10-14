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

### US-001 — Abertura de Chamado com Validação Guiada (HU01)

> **Como** solicitante ou analista, **eu quero** abrir um chamado preenchendo um formulário guiado que valida os dados em tempo real, **para que** o problema seja registrado de forma estruturada com identificador único e rastreável.

**Prioridade:** Must Have · **Story Points:** 8 · **Sprint:** 1

**Requisitos relacionados:** RF-001, RF-002, RF-010, RF-031, RF-034, RF-036

#### Critérios de Aceite

- [ ] O formulário exibe os campos: título (obrigatório), descrição (obrigatório), categoria (obrigatório, dropdown), unidade física da Rede Lúmen (obrigatório, dropdown), canal de origem e anexos (opcional).
- [ ] O chamado recebe um identificador único e legível no padrão `LUM-AAAA-NNNNNN` (ex.: `LUM-2026-000123`).
- [ ] Todo chamado está associado a exatamente uma unidade física da Rede Lúmen.
- [ ] O registro **não exige nem permite informações clínicas completas ou resultados de exames de pacientes** (conformidade LGPD).
- [ ] Campos obrigatórios não preenchidos são destacados com mensagem de erro descritiva.
- [ ] A validação ocorre em tempo real (on blur / on change) e também no submit.
- [ ] Após submissão bem-sucedida, o usuário é redirecionado para a tela de detalhes com mensagem de confirmação.

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
| T-003 | Criar endpoint `POST /api/tickets` com validação server-side e geração de código `LUM` | Backend | 3h |
| T-004 | Criar migration e entidades `Ticket` e `Unidade` no banco de dados | Backend | 2h |
| T-005 | Implementar service de criação de ticket com regras de negócio e validação LGPD | Backend | 2h |
| T-006 | Escrever testes unitários para o service de criação | Teste | 2h |
| T-007 | Escrever testes E2E para o fluxo de abertura de chamado | Teste | 3h |
| T-008 | Criar feedback visual (loading, sucesso, erro) no formulário | Frontend | 1h |

---

### US-002 — Listagem e Visualização de Chamados (HU02)

> **Como** analista, **eu quero** visualizar uma lista de chamados com filtros e acessar os detalhes de cada um, **para que** eu possa compreender o histórico completo e continuar o atendimento sem ruído.

**Prioridade:** Must Have · **Story Points:** 5 · **Sprint:** 1

**Requisitos relacionados:** RF-003, RF-004, RF-006, RF-007, RF-032

#### Critérios de Aceite

- [ ] A lista exibe: código `LUM`, título, status, prioridade, categoria, unidade, data de abertura e analista atribuído.
- [ ] É possível filtrar por: status, prioridade, categoria, unidade e intervalo de datas.
- [ ] A lista é paginada (20 itens por página, configurável).
- [ ] Clicar em um chamado abre a tela de detalhes com timeline cronológica reunindo múltiplos contatos e eventos de histórico.
- [ ] As evidências anexadas permanecem ocultas por padrão na tela, exigindo clique para revelação com registro de auditoria (`AnexoVisualizado`).
- [ ] Um analista diferente daquele que iniciou o atendimento consegue visualizar e compreender todo o histórico.

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
| T-010 | Criar endpoint `GET /api/tickets/:id` com histórico completo de interações | Backend | 2h |
| T-011 | Implementar log de auditoria ao visualizar evidências anexadas | Backend | 2h |
| T-012 | Criar componente Angular de tabela de chamados com ordenação e paginação | Frontend | 4h |
| T-013 | Criar componente Angular de filtros (dropdowns reativos + debounce) | Frontend | 3h |
| T-014 | Criar tela de detalhes do chamado com timeline cronológica | Frontend | 4h |
| T-015 | Implementar componente de exibição segura de anexos (oculto por padrão) | Frontend | 2h |
| T-016 | Escrever testes unitários para listagem e timeline | Teste | 2h |

---

### US-010 — Identificação de Relatos Duplicados e Mesclagem (HU03)

> **Como** analista de suporte, **eu quero** vincular múltiplos relatos ao mesmo chamado e mesclar ocorrências duplicadas, **para que** a equipe evite tratar o mesmo problema como incidentes independentes.

**Prioridade:** Must Have · **Story Points:** 5 · **Sprint:** 2

**Requisitos relacionados:** RF-032, RF-033

#### Critérios de Aceite

- [ ] É possível associar mais de um Registro de Contato a um mesmo chamado existente.
- [ ] A associação de relatos não apaga o registro original do contato.
- [ ] É possível executar a operação de mesclagem de chamados duplicados: o secundário vai para `MESCLADO` e referencia o principal.
- [ ] No chamado principal, os contatos e evidências do chamado secundário são consolidados na timeline.

#### Checklist INVEST

- [x] **Independent:** Opera sobre chamados já cadastrados.
- [x] **Negotiable:** Mecanismo de busca de duplicados pode ser refinado.
- [x] **Valuable:** Evita retrabalho e fragmentação da equipe.
- [x] **Estimable:** Operação transacional relacional.
- [x] **Small:** Cabe em uma sprint.
- [x] **Testable:** Validação de vínculo e preservação de registros.

#### Tasks

| ID | Task | Tipo | Estimativa |
|----|------|:----:|:----------:|
| T-017 | Criar endpoint `POST /api/tickets/:id/merge` com controle transacional | Backend | 3h |
| T-018 | Implementar agregação de registros de contato no chamado principal | Backend | 2h |
| T-019 | Criar modal Angular para seleção e mesclagem de chamado duplicado | Frontend | 3h |
| T-020 | Escrever testes unitários e de integração para mesclagem | Teste | 2h |

---

## 🤖 Epic 2: Triagem Inteligente (IA)

---

### US-003 — Triagem Automática com IA (HU04)

> **Como** analista, **eu quero** que o sistema analise automaticamente o chamado com IA e sugira a prioridade com base na Matriz 3D (Severidade, Urgência e Impacto), **para que** a triagem seja ágil e baseada em critérios técnicos objetivos.

**Prioridade:** Must Have · **Story Points:** 13 · **Sprint:** 2

**Requisitos relacionados:** RF-009, RF-011, RF-012

#### Critérios de Aceite

- [ ] A triagem por IA é disparada automaticamente após a criação do ticket.
- [ ] O modelo avalia separadamente notas de Severidade (1-3), Urgência (1-3) e Impacto Operacional (1-3) sugerindo o nível de prioridade (P1 a P4).
- [ ] A IA apresenta uma justificativa textual em linguagem natural explicando o raciocínio.
- [ ] O analista pode aceitar a prioridade sugerida com um clique ou alterá-la manualmente (exigindo justificativa caso discorde).
- [ ] Solicitantes ou gestores não podem definir diretamente a prioridade final.
- [ ] Se a chamada à API de IA falhar, o ticket permanece criado com status `ABERTO` e triagem marcada para retry sem travar o usuário.

#### Checklist INVEST

- [x] **Independent:** Funciona sobre qualquer ticket criado.
- [x] **Negotiable:** Prompt da IA e thresholds da Matriz 3D são ajustáveis.
- [x] **Valuable:** Core differentiator do AvanDesk-AI.
- [x] **Estimable:** Integração com LLM + parser de resposta estruturada.
- [x] **Small:** Story pontuada em 13, dividida em tasks claras.
- [x] **Testable:** Testável com mocks da API de IA e validação de schema.

#### Tasks

| ID | Task | Tipo | Estimativa |
|----|------|:----:|:----------:|
| T-021 | Criar client de integração com Azure OpenAI Service (Spring AI ou REST client) | Backend | 4h |
| T-022 | Implementar engenharia de prompt para análise de severidade, urgência e impacto | Backend | 3h |
| T-023 | Criar endpoint `POST /api/tickets/:id/triage` para disparo e re-triagem | Backend | 2h |
| T-024 | Criar endpoint `PATCH /api/tickets/:id/priority` para confirmação/ajuste pelo analista | Backend | 2h |
| T-025 | Implementar fila de retry para resiliência a falhas da API de IA | Backend | 3h |
| T-026 | Criar componente Angular do card de triagem IA com justificativa e botões de ação | Frontend | 4h |
| T-027 | Implementar feedback de loading durante o processamento da IA | Frontend | 1h |
| T-028 | Escrever testes com mock da API de IA para cenários de sucesso, erro e timeout | Teste | 3h |

---

### US-004 — Insights Contextuais e Base de Conhecimento

> **Como** analista, **eu quero** receber insights contextuais gerados por IA sobre o chamado, **para que** eu tenha pontos de partida para investigação rápida.

**Prioridade:** Should Have · **Story Points:** 8 · **Sprint:** 3

**Requisitos relacionados:** RF-013, RF-014

#### Critérios de Aceite

- [ ] A tela de detalhes do chamado exibe um painel de "Insights da IA".
- [ ] Os insights incluem: hipóteses sobre causas prováveis e sugestões de validação técnica.
- [ ] Quando houver chamados similares anteriores, o sistema exibe links para eles com a solução aplicada.

#### Tasks

| ID | Task | Tipo | Estimativa |
|----|------|:----:|:----------:|
| T-029 | Implementar prompt especializado para geração de insights e hipóteses | Backend | 3h |
| T-030 | Criar endpoint `GET /api/tickets/:id/insights` | Backend | 2h |
| T-031 | Criar componente Angular de exibição de insights contextuais | Frontend | 3h |
| T-032 | Escrever testes para serviço de insights | Teste | 2h |

---

## 📋 Epic 3: Workflow, SLA & Escalonamento

---

### US-005 — Fluxo de Status, Atribuição e Resolução (HU05 & HU06)

> **Como** analista, **eu quero** atualizar o status do chamado, registrar a resolução com causa raiz e atribuir responsáveis, **para que** o ciclo de vida seja controlado com transparência e histórico.

**Prioridade:** Must Have · **Story Points:** 5 · **Sprint:** 2

**Requisitos relacionados:** RF-005, RF-015, RF-017, RF-035

#### Critérios de Aceite

- [ ] As transições de status seguem a máquina de estados: `ABERTO → EM_TRIAGEM → EM_ANDAMENTO → PENDENTE → RESOLVIDO → FECHADO` (com suporte a `MESCLADO` e `CANCELADO`).
- [ ] Toda mudança de status é registrada no histórico com timestamp e usuário.
- [ ] Ao transitar para `RESOLVIDO`, o analista registra a solução aplicada e, quando identificada, a causa raiz técnica.
- [ ] Um chamado resolvido só pode transitar para `REABERTO` (em até 7 dias) ou `FECHADO`.
- [ ] A reatribuição exige seleção de um analista ativo e registra o evento na timeline.

#### Checklist INVEST

- [x] **Independent:** Funciona com tickets existentes.
- [x] **Negotiable:** Regras de transição podem ser ajustadas.
- [x] **Valuable:** Sem workflow, o atendimento é caótico.
- [x] **Estimable:** State machine + UI = esforço mensurável.
- [x] **Small:** Cabe em uma sprint.
- [x] **Testable:** Transições válidas e inválidas são facilmente testáveis.

#### Tasks

| ID | Task | Tipo | Estimativa |
|----|------|:----:|:----------:|
| T-033 | Implementar state machine para transições de status do ticket | Backend | 3h |
| T-034 | Criar endpoint `PATCH /api/tickets/:id/status` com validação de transição | Backend | 2h |
| T-035 | Criar endpoint `POST /api/tickets/:id/resolver` com solução e causa raiz | Backend | 2h |
| T-036 | Criar endpoint `PATCH /api/tickets/:id/assign` para reatribuição | Backend | 1h |
| T-037 | Criar componente Angular de alteração de status e modal de resolução | Frontend | 3h |
| T-038 | Criar componente Angular de reatribuição com autocomplete de analistas | Frontend | 2h |
| T-039 | Escrever testes unitários para state machine e transições de status | Teste | 2h |

---

### US-011 — Escalonamento para Fornecedor Externo (HU07)

> **Como** analista de suporte, **eu quero** registrar o encaminhamento de um chamado para um fornecedor externo mantendo o acompanhamento interno, **para que** o atendimento não perca rastreabilidade perante a unidade solicitante.

**Prioridade:** Should Have · **Story Points:** 5 · **Sprint:** 3

**Requisitos relacionados:** RF-020

#### Critérios de Aceite

- [ ] O analista pode selecionar um fornecedor parceiro cadastrado e registrar o protocolo externo de abertura.
- [ ] O chamado transita para `PENDENTE` com motivo documentado de dependência externa.
- [ ] O chamado permanece ativo e sob acompanhamento do analista interno responsável.
- [ ] O analista pode registrar o retorno e parecer do fornecedor, reativando o status para `EM_ANDAMENTO`.

#### Tasks

| ID | Task | Tipo | Estimativa |
|----|------|:----:|:----------:|
| T-040 | Criar migration e entidades para fornecedores externos e escalonamento | Backend | 2h |
| T-041 | Criar endpoints `POST /api/tickets/:id/escalar` e `POST /api/tickets/:id/retorno` | Backend | 3h |
| T-042 | Criar modal de escalonamento externo com seleção de parceiro e protocolo | Frontend | 3h |
| T-043 | Escrever testes para fluxo de escalonamento e retorno | Teste | 2h |

---

### US-006 — Monitoramento de Prazos e SLA

> **Como** gestor ou analista, **eu quero** que o sistema monitore os prazos de atendimento e alerte quando estiverem em risco, **para que** a equipe intervenha antes de atrasos na rotina diagnóstica.

**Prioridade:** Should Have · **Story Points:** 8 · **Sprint:** 3

**Requisitos relacionados:** RF-018, RF-019

#### Critérios de Aceite

- [ ] Prazos operacionais de atendimento são calculados com base na prioridade (referência interna de acompanhamento).
- [ ] O tempo restante é exibido no chamado como indicador visual (verde/amarelo/vermelho).
- [ ] Alertas visuais são disparados quando o prazo atinge 75% e 90% do tempo estimado.

#### Tasks

| ID | Task | Tipo | Estimativa |
|----|------|:----:|:----------:|
| T-044 | Criar tabela de configuração de prazos por prioridade | Backend | 2h |
| T-045 | Implementar scheduler para monitoramento contínuo de prazos de atendimento | Backend | 3h |
| T-046 | Criar componente Angular de indicador visual de tempo restante no chamado | Frontend | 2h |
| T-047 | Escrever testes para rotinas de monitoramento | Teste | 2h |

---

## 🔐 Epic 4: Autenticação & Acesso

---

### US-007 — Autenticação e Controle de Acesso

> **Como** colaborador ou analista, **eu quero** me autenticar com minhas credenciais e ter acesso às funcionalidades do meu perfil, **para que** a segurança e privacidade em saúde sejam preservadas.

**Prioridade:** Must Have · **Story Points:** 8 · **Sprint:** 1

**Requisitos relacionados:** RF-021, RF-023, RF-024

#### Critérios de Aceite

- [ ] O usuário pode fazer login com e-mail corporativo e senha.
- [ ] Após login bem-sucedido, um token JWT é retornado e armazenado com segurança.
- [ ] Rotas protegidas redirecionam para a tela de login se o token for inválido ou expirado.
- [ ] Solicitantes acessam apenas abertura e consulta de seus chamados.
- [ ] O perfil Gestor/Liderança possui acesso a métricas e chamados agregados, mas tem **acesso bloqueado a anexos de evidências técnicas sensíveis**.
- [ ] Tentativas de login inválidas exibem mensagem genérica.

#### Tasks

| ID | Task | Tipo | Estimativa |
|----|------|:----:|:----------:|
| T-048 | Criar entidade `Usuario` com perfis (SOLICITANTE, ANALISTA, GESTOR) e migration | Backend | 2h |
| T-049 | Implementar endpoint `POST /api/auth/login` com geração de JWT | Backend | 3h |
| T-050 | Implementar guards e autorização RBAC nas APIs | Backend | 3h |
| T-051 | Criar tela de login com formulário reativo e feedback visual | Frontend | 2h |
| T-052 | Implementar AuthGuard e interceptor HTTP para JWT no Angular | Frontend | 2h |
| T-053 | Escrever testes para autenticação e restrições de perfil | Teste | 2h |

---

## 📊 Epic 5: Dashboard & Visibilidade

---

### US-008 — Dashboard de Métricas da Liderança (HU08)

> **Como** liderança de TI, **eu quero** visualizar um dashboard com métricas operacionais consolidadas por unidade, **para que** eu possa acompanhar a saúde do atendimento e gargalos operacionais da Rede Lúmen.

**Prioridade:** Should Have · **Story Points:** 8 · **Sprint:** 3

**Requisitos relacionados:** RF-025, RF-026, RF-027, RF-041

#### Critérios de Aceite

- [ ] O dashboard exibe: chamados abertos, tempo médio de resolução, distribuição por categoria e volume por unidade da Lúmen.
- [ ] O painel calcula e exibe a métrica principal: **% de chamados com histórico completo e rastreável**.
- [ ] Gráficos interativos permitem filtro por unidade física e período.
- [ ] O perfil de Liderança não tem permissão para visualizar anexos de evidências técnicas na plataforma.

#### Tasks

| ID | Task | Tipo | Estimativa |
|----|------|:----:|:----------:|
| T-054 | Criar endpoints de agregação e métricas por unidade no backend | Backend | 4h |
| T-055 | Integrar biblioteca de gráficos (Chart.js / ng2-charts) | Frontend | 2h |
| T-056 | Construir componente de dashboard com cards de KPIs e gráficos | Frontend | 4h |
| T-057 | Escrever testes para queries de agregação | Teste | 2h |

---

## 🔔 Epic 6: Notificações

---

### US-009 — Notificações de Eventos

> **Como** analista ou solicitante, **eu quero** receber notificações sobre atualizações no chamado, **para que** eu possa agir rapidamente e acompanhar o status.

**Prioridade:** Should Have · **Story Points:** 5 · **Sprint:** 3

**Requisitos relacionados:** RF-028, RF-029, RF-030

#### Critérios de Aceite

- [ ] Analistas recebem notificação in-app em tempo real ao receberem novos chamados atribuídos.
- [ ] Solicitantes recebem notificações por e-mail ou WhatsApp quando o status do chamado mudar.
- [ ] Notificações in-app podem ser marcadas como lidas pelo usuário.

#### Tasks

| ID | Task | Tipo | Estimativa |
|----|------|:----:|:----------:|
| T-058 | Implementar envio de e-mails para notificações de mudança de status | Backend | 3h |
| T-059 | Criar componente Angular de central de notificações com dropdown | Frontend | 3h |
| T-060 | Escrever testes para notificações | Teste | 2h |

---

## 📋 Resumo de User Stories por Sprint

### Sprint 1 — Fundação & Registro
| Story | Equivalência Lúmen | Pontos | Prioridade |
|-------|:------------------:|:------:|:----------:|
| US-007 — Autenticação e Controle de Acesso | — | 8 | Must |
| US-001 — Abertura de Chamado com Validação Guiada | **HU01** | 8 | Must |
| US-002 — Listagem e Visualização de Chamados | **HU02** | 5 | Must |
| **Total Sprint 1** | | **21** | |

### Sprint 2 — Inteligência, Workflow & Duplicidades
| Story | Equivalência Lúmen | Pontos | Prioridade |
|-------|:------------------:|:------:|:----------:|
| US-003 — Triagem Automática com IA & Matriz 3D | **HU04** | 13 | Must |
| US-005 — Fluxo de Status, Atribuição e Resolução | **HU05 / HU06** | 5 | Must |
| US-010 — Identificação de Relatos Duplicados e Mesclagem | **HU03** | 5 | Must |
| **Total Sprint 2** | | **23** | |

### Sprint 3 — Visibilidade, Operação & Escalonamento
| Story | Equivalência Lúmen | Pontos | Prioridade |
|-------|:------------------:|:------:|:----------:|
| US-004 — Insights Contextuais | — | 8 | Should |
| US-006 — Monitoramento de Prazos e SLA | — | 8 | Should |
| US-011 — Escalonamento para Fornecedor Externo | **HU07** | 5 | Should |
| US-008 — Dashboard de Métricas da Liderança | **HU08** | 8 | Should |
| US-009 — Notificações de Eventos | — | 5 | Should |
| **Total Sprint 3** | | **34** | |
