# 🔒 Requisitos Não Funcionais — AvanDesk-AI

## Categorias ISO/IEC 25010

Os requisitos não funcionais estão organizados segundo as características de qualidade da **ISO/IEC 25010 (SQuaRE)**.

---

## 1. Desempenho (Performance Efficiency)

| ID | Requisito | Métrica / Critério de Aceite | Prioridade |
|----|-----------|------------------------------|:----------:|
| RNF-001 | O tempo de resposta para operações CRUD de tickets deve ser inferior a **500ms** sob carga normal. | p95 < 500ms com até 100 usuários simultâneos. | **M** |
| RNF-002 | A triagem por IA deve retornar o resultado em no máximo **10 segundos** após a submissão do ticket. | p95 < 10s; feedback de progresso exibido ao usuário. | **M** |
| RNF-003 | O dashboard deve carregar em no máximo **2 segundos**. | Tempo até First Contentful Paint (FCP) < 2s. | **S** |
| RNF-004 | O sistema deve suportar no mínimo **100 usuários simultâneos** sem degradação perceptível. | Validado por teste de carga (ex.: k6, JMeter). | **S** |

---

## 2. Segurança (Security)

| ID | Requisito | Métrica / Critério de Aceite | Prioridade |
|----|-----------|------------------------------|:----------:|
| RNF-005 | Todas as senhas devem ser armazenadas com hash seguro (bcrypt ou argon2). | Nenhuma senha em texto plano no banco de dados. | **M** |
| RNF-006 | Toda comunicação entre cliente e servidor deve usar **HTTPS/TLS 1.2+**. | Certificado SSL válido em todos os ambientes. | **M** |
| RNF-007 | Tokens de autenticação (JWT) devem ter expiração configurável (default: 1 hora) com refresh token. | Validação de expiração em todas as requisições autenticadas. | **M** |
| RNF-008 | O sistema deve implementar proteção contra os **OWASP Top 10**: SQL Injection, XSS, CSRF, etc. | Scan de vulnerabilidades (OWASP ZAP ou equivalente) sem achados críticos. | **M** |
| RNF-009 | O sistema deve implementar rate limiting em endpoints públicos e de autenticação. | Máx. 10 tentativas de login por minuto por IP. | **S** |
| RNF-010 | Dados sensíveis em logs devem ser mascarados (e-mail, tokens, senhas). | Auditoria de logs confirma ausência de dados expostos. | **S** |
| RNF-027 | O sistema não deve armazenar nem permitir campos para dados clínicos ou resultados de exames de pacientes. | Validação de schema atesta conformidade com restrições LGPD da Rede Lúmen. | **M** |
| RNF-028 | Evidências anexadas devem permanecer ocultas por padrão na tela e registrar log de visualização (`AnexoVisualizado`). | Anexos só aparecem sob clique do analista com auditoria; bloqueados para Liderança. | **M** |

---

## 3. Confiabilidade (Reliability)

| ID | Requisito | Métrica / Critério de Aceite | Prioridade |
|----|-----------|------------------------------|:----------:|
| RNF-011 | O sistema deve ter disponibilidade mínima de **99,5%** (excluindo janelas de manutenção programada). | Monitoramento com alertas automáticos (com foco nas unidades 24h). | **S** |
| RNF-012 | Em caso de falha na API de IA, o ticket deve ser criado normalmente e a triagem enfileirada para retry. | Mecanismo de fila com retry exponencial (máx. 3 tentativas). | **M** |
| RNF-013 | O sistema deve realizar backup automático do banco de dados a cada **24 horas**. | Restore testado e documentado trimestralmente. | **S** |
| RNF-029 | O processamento de mensagens de canais externos (WhatsApp e e-mail) deve ser estritamente idempotente. | ID externo (`Message-ID` ou WhatsApp ID) impede duplicidade em retentativas. | **M** |
| RNF-030 | O sistema deve detectar e descartar automaticamente respostas de e-mail do tipo `Auto-Submitted`. | Descarte de mensagens RFC 3834 para evitar loops entre servidores. | **M** |

---

## 4. Usabilidade (Usability)

| ID | Requisito | Métrica / Critério de Aceite | Prioridade |
|----|-----------|------------------------------|:----------:|
| RNF-014 | A interface deve ser responsiva e funcional em dispositivos **desktop** (≥ 1024px) e **tablet** (≥ 768px). | Testes em Chrome, Firefox e Edge nas resoluções alvo. | **M** |
| RNF-015 | A interface deve seguir diretrizes de acessibilidade **WCAG 2.1 nível AA**. | Contraste mínimo 4.5:1; navegação por teclado; labels em formulários. | **S** |
| RNF-016 | O sistema deve fornecer feedback visual para todas as ações do usuário (loading, sucesso, erro). | Nenhuma ação sem feedback perceptível em até 200ms. | **M** |
| RNF-017 | A interface deve suportar os idiomas **português (pt-BR)** e **inglês (en-US)** (i18n). | Alternância de idioma sem reload de página. | **C** |
| RNF-031 | O sistema deve padronizar a identificação de solicitantes aceitando números de telefone no formato internacional **E.164** e e-mails em minúsculas. | Validação e formatação automática no momento da captura. | **M** |

---

## 5. Manutenibilidade (Maintainability)

| ID | Requisito | Métrica / Critério de Aceite | Prioridade |
|----|-----------|------------------------------|:----------:|
| RNF-018 | O código deve seguir padrões de linting configurados (ESLint para frontend, linter do backend). | CI/CD bloqueia merge com erros de lint. | **M** |
| RNF-019 | O projeto deve ter cobertura de testes unitários de no mínimo **70%** nas camadas de serviço e domínio. | Relatório de cobertura gerado no CI. | **S** |
| RNF-020 | O sistema deve utilizar variáveis de ambiente para todas as configurações sensíveis e de ambiente. | Nenhuma credencial hard-coded no repositório. | **M** |
| RNF-021 | O código deve seguir convenções de commit semântico (**Conventional Commits**). | `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:` em português imperativo. | **S** |

---

## 6. Portabilidade (Portability)

| ID | Requisito | Métrica / Critério de Aceite | Prioridade |
|----|-----------|------------------------------|:----------:|
| RNF-022 | O sistema deve ser containerizado com **Docker** e orquestrado com **Docker Compose**. | `docker-compose up` deve subir o ambiente completo em < 2 minutos. | **M** |
| RNF-023 | O sistema deve ser agnóstico a provedor de cloud, evitando vendor lock-in desnecessário. | Exceção: serviço de IA (Azure OpenAI) isolado em adapter pattern. | **S** |

---

## 7. Observabilidade (Operability)

| ID | Requisito | Métrica / Critério de Aceite | Prioridade |
|----|-----------|------------------------------|:----------:|
| RNF-024 | O sistema deve expor health checks em endpoint padronizado (`/health`). | Retorna status dos serviços dependentes (DB, IA). | **M** |
| RNF-025 | O sistema deve gerar logs estruturados (JSON) com correlation ID por requisição. | Rastreabilidade de requisição end-to-end. | **S** |
| RNF-026 | O sistema deve expor métricas para monitoramento (ex.: Prometheus-compatible). | Tempo de resposta, taxa de erros, uso de recursos. | **C** |

---

## Resumo por Prioridade

| Prioridade | Quantidade |
|:----------:|:----------:|
| **Must Have** | 17 |
| **Should Have** | 11 |
| **Could Have** | 3 |
| **Total** | **31** |
