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
| RF-001 | O sistema deve permitir que o analista de suporte registre uma solicitação recebida por um canal existente como um novo chamado. | **M** | Deve indicar unidade (obrigatória), solicitante e canal de origem. |
| RF-002 | O sistema deve validar a completude dos dados do chamado antes da submissão, indicando campos obrigatórios não preenchidos. | **M** | Validação client-side + server-side. |
| RF-003 | O sistema deve exibir uma lista de chamados com filtros por status, prioridade, unidade e responsável. | **M** | Paginação, ordenação e busca por identificador único. |
| RF-004 | O sistema deve permitir a visualização detalhada de um chamado, incluindo todo seu histórico e registros de contato. | **M** | Timeline cronológica de eventos. |
| RF-005 | O sistema deve permitir a edição de dados do chamado durante seu ciclo de vida. | **M** | Apenas campos permitidos, mantendo auditoria. |
| RF-006 | O sistema deve permitir a adição de novos registros de contato a um chamado existente. | **M** | Informando canal, data e quem relatou. |
| RF-007 | O sistema deve permitir a anexação de arquivos (imagens, logs, documentos) ao chamado. | **S** | Limite de tamanho configurável. Tratamento para dados sensíveis. |
| RF-008 | O sistema deve alertar ou bloquear a inclusão de dados clínicos completos e resultados de exames. | **S** | Restrição de negócio para evitar exposição de dados sensíveis. |

---

## 2. Módulo: Triagem e Estruturação

| ID | Requisito | Prioridade | Observações |
|----|-----------|:----------:|-------------|
| RF-009 | O sistema deve permitir que o analista avalie a severidade, urgência e impacto operacional de um chamado. | **M** | Variáveis separadas para definição de prioridade. |
| RF-010 | O sistema deve calcular ou determinar a prioridade final com base na combinação de severidade, urgência e impacto. | **M** | Restrito a perfis autorizados (Analista/Liderança). |
| RF-011 | O sistema deve permitir associar diferentes relatos (múltiplos registros de contato) ao mesmo chamado. | **M** | Identificação manual de duplicidades/relacionamentos. |
| RF-012 | O sistema deve permitir que o analista registre a resolução do chamado. | **M** | Com opção de indicar a causa raiz, se identificada. |
| RF-013 | O sistema deve verificar os elementos mínimos de rastreabilidade no momento do encerramento do chamado. | **M** | Problema, unidade, responsável, contatos e resolução. |
| RF-014 | O sistema deve permitir relacionar chamados a uma mesma causa raiz para identificar recorrências. | **S** | — |

---

## 3. Módulo: Workflow e Escalonamento

| ID | Requisito | Prioridade | Observações |
|----|-----------|:----------:|-------------|
| RF-015 | O sistema deve gerenciar o fluxo de estados do chamado (ciclo de vida). | **M** | Transições validadas (ex.: Resolvido → Reaberto). |
| RF-016 | O sistema deve permitir que o analista assuma um chamado ou o atribua/reatribua a outro analista ativo. | **M** | Registro em histórico. |
| RF-017 | O sistema deve permitir o escalonamento de um chamado para um fornecedor externo. | **M** | Mantendo o analista interno como responsável pelo acompanhamento. |
| RF-018 | O sistema deve permitir registrar o retorno do fornecedor externo e encerrar o escalonamento. | **M** | — |
| RF-019 | O sistema deve calcular e monitorar o SLA (formal) de cada chamado. | **W** | SLA formal está fora do MVP. |
| RF-020 | O sistema deve realizar escalonamento e triagem automáticos por IA. | **W** | IA fora do MVP. |

---

## 4. Módulo: Identidade e Configuração

| ID | Requisito | Prioridade | Observações |
|----|-----------|:----------:|-------------|
| RF-021 | O sistema deve permitir autenticação de usuários com e-mail e senha. | **M** | Senhas com hash seguro. |
| RF-022 | O sistema deve implementar controle de acesso baseado em perfis: Analista de Suporte e Liderança de TI. | **M** | Permissões restritas por perfil. |
| RF-023 | O sistema deve permitir o gerenciamento (cadastro, edição, desativação) de Analistas de Suporte. | **M** | Restrito à Liderança de TI (ou Administrador). |
| RF-024 | O sistema deve permitir o gerenciamento (cadastro, edição, desativação) das Unidades da rede. | **M** | Restrito à Liderança de TI (ou Administrador). |
| RF-025 | O sistema deve suportar autenticação via provedor corporativo (SSO/OAuth 2.0). | **C** | Integração com Azure AD (provável). |

---

## 5. Módulo: Visão Operacional (Dashboard)

| ID | Requisito | Prioridade | Observações |
|----|-----------|:----------:|-------------|
| RF-026 | O sistema deve exibir um painel operacional com os chamados em aberto e responsáveis. | **M** | Restrito à Liderança de TI. |
| RF-027 | O sistema deve exibir informações agregadas por unidade e problemas recorrentes. | **S** | — |
| RF-028 | O sistema deve calcular e exibir a métrica de "% de chamados com histórico completo e rastreável". | **M** | Métrica de sucesso do MVP. |

---

## 6. Módulo: Notificações

| ID | Requisito | Prioridade | Observações |
|----|-----------|:----------:|-------------|
| RF-029 | O sistema deve enviar notificações in-app para analistas sobre novos chamados atribuídos ou atualizações. | **S** | Real-time. |
| RF-030 | O sistema deve permitir que usuários configurem suas preferências de notificação. | **C** | — |

---

## Rastreabilidade: Requisitos → User Stories (MVP)

| Requisito | User Story |
|-----------|------------|
| RF-001, RF-002, RF-008 | HU01 - Registrar um chamado |
| RF-004, RF-005 | HU02 - Consultar e manter histórico |
| RF-011 | HU03 - Identificar relatos duplicados |
| RF-009, RF-010 | HU04 - Definir prioridade |
| RF-015 | HU05 - Acompanhar status |
| RF-012, RF-014 | HU06 - Registrar resolução e causa raiz |
| RF-017, RF-018 | HU07 - Escalar chamado para fornecedor |
| RF-026, RF-027 | HU08 - Visualizar a situação dos chamados |
| RF-021, RF-022 | HU09 - Autenticar no sistema |
| RF-023 | HU10 - Gerenciar analistas |
| RF-024 | HU11 - Cadastrar unidades |
| RF-015, RF-016 | HU12 - Atribuir responsável e atualizar status |
| RF-006 | HU13 - Registrar novo contato |
| RF-003 | HU14 - Listar e buscar chamados |
| RF-013, RF-028 | HU15 - Encerrar chamado com verificação |

> 📎 Ver detalhes completos em [Nexo - Historias de Usuario do MVP.md](./Nexo - Historias de Usuario do MVP.md)
