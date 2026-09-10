# 🛠️ Definição Técnica Inicial & Ambiente do Projeto

**Squad:** Squad 1 — Rede Lúmen Diagnósticos  
**Projeto:** AvanDesk-AI  
**Programa:** Residência Tecnológica Avanade 2026.2 (Porto Digital / CESAR School / Embarque Digital)  
**Tech Lead:** Luiz Henrique Souza  
**Data:** 10 de Setembro de 2026  

---

## 1. Contexto e Objetivo do Projeto

### 1.1 Contexto
* **Cenário atual:** A **Rede Lúmen Diagnósticos** conta com 14 unidades laboratoriais em Pernambuco (sendo 2 delas com operação ininterrupta 24 horas), reunindo um corpo de aproximadamente 900 colaboradores e uma equipe enxuta de apenas 7 analistas de TI e suporte.
* **Problema enfrentado:** Atualmente, as solicitações de suporte chegam de forma desordenada e fragmentada por múltiplos canais — grupos informais de WhatsApp, ramais telefônicos, e-mails diretos, conversas de corredor e planilhas paralelas.
* **Impactos operacionais:**
  1. Ausência de um identificador único (`Ticket ID`), gerando perda de histórico e retrabalho.
  2. Fenômeno de "tudo é urgente", onde gestores locais cobram prioridade máxima sem critério objetivo, dificultando a triagem de incidentes com real impacto na cadeia assistencial (ex.: parada de impressora de etiquetas na coleta vs. lentidão em micro administrativo).
  3. Riscos de conformidade com a LGPD pelo envio inadvertido de telas com dados identificáveis e exames de pacientes.
  4. Desafio de estações de trabalho compartilhadas nas recepções e laboratórios com ritmos intensos.

### 1.2 Objetivo Geral
Desenvolver o **AvanDesk-AI**, uma plataforma centralizada e inteligente de governança de chamados de suporte técnico, projetada para integrar o fluxo de atendimento em um único hub operacional. A solução utilizará Inteligência Artificial para:
* Guiar o registro de tickets e identificar lacunas de contexto essenciais.
* Realizar triagem automatizada e arbitrar a prioridade de atendimento com base no impacto assistencial e operacional.
* Apoiar os 7 analistas com histórico, direcionamento e controle rigoroso de prazos de SLA por criticidade, em conformidade com as diretrizes de privacidade e LGPD.

---

## 2. Organização do Repositório & Governança

### 2.1 Modelo de Repositório
- [x] **Monorepositório** (Frontend, Backend e documentação no mesmo repositório)
- [ ] Multirrepositório (Repositórios separados por componente)

**Justificativa da escolha:**  
O time de desenvolvimento é enxuto e o projeto está sendo concebido do zero com um prazo determinado de 5 meses para entrega do MVP funcional. Adotar um monorepositório neste ciclo traz ganhos determinantes:
1. **Redução drástica de overhead operacional:** Gerenciamento centralizado de issues, branches, pull requests e releases em um único ponto focal.
2. **Sincronia atômica entre camadas:** Permite que alterações de contrato (DTOs/APIs no backend) e seu respectivo consumo na interface (Angular) sejam entregues, testadas e revisadas no mesmo Pull Request vinculado à User Story.
3. **CI/CD unificado:** Simplificação do pipeline de integração contínua e garantia de que a documentação técnica permaneça viva e versionada junto ao código-fonte.
Caso surja a necessidade futura de isolamento de um serviço especializado (ex.: um worker assíncrono de IA dedicado), o desacoplamento poderá ser planejado organicamente sem antecipar complexidades desnecessárias.

### 2.2 Estrutura Inicial de Diretórios

```text
├── .github/
│   └── pull_request_template.md
├── backend/
├── frontend/
├── docs/
├── .gitignore
└── README.md
```

* **`.github/`**: Automações, workflows de CI/CD e template oficial de Pull Request.
* **`backend/`**: Código-fonte da API, regras de negócio, integrações e persistência em Java com Spring Boot.
* **`frontend/`**: Aplicação cliente web desenvolvida em Angular/TypeScript.
* **`docs/`**: Documentação de engenharia (domínio, requisitos funcionais, não funcionais, histórias de usuário, roadmap e convenções de Git).
* **`.gitignore`**: Regras unificadas de exclusão para o ecossistema de desenvolvimento.
* **`README.md`**: Apresentação executiva, contexto do cliente e guia de navegação do projeto.

---

### 2.3 Convenções de Versionamento e Colaboração

#### Padrão de Branches (Git Flow Simplificado)
* **`main`**: Código estável, homologado e sempre "pronto para demo/produção".
* **`develop`**: Branch base de integração contínua das features no dia a dia da squad.
* **Formato para branches de trabalho:** `[tipo]/[identificador-tarefa]-[descricao-curta]`
  * **Tipos permitidos:** `feat` | `fix` | `docs` | `style` | `refactor` | `test` | `chore` | `perf` (e `hotfix/*` para incidentes críticos direto sobre a `main`).
  * **Exemplos práticos:**
    * `feat/US-01-motor-priorizacao`
    * `fix/US-03-calculo-sla`
    * `docs/atualizar-instrucoes-setup`
    * `refactor/service-chamados`
    * `chore/atualizar-docker-compose`

#### Padrão de Commits (Conventional Commits)
Adoção do padrão **Conventional Commits** com tipos padronizados em inglês e mensagens obrigatoriamente em português no imperativo:
* `feat`: Nova funcionalidade para o usuário.
* `fix`: Correção de defeito/bug.
* `docs`: Alterações exclusivas em documentação.
* `style`: Formatação, indentação ou linter sem impacto na regra de negócio.
* `refactor`: Refatoração de código sem mudança de comportamento externo.
* `test`: Criação ou ajuste de baterias de testes.
* `chore`: Alterações em build, dependências, scripts ou infraestrutura.
* `perf`: Melhorias diretas de performance e tempo de resposta.

* **Estrutura:** `<tipo>(<escopo opcional>): <descrição no imperativo em português>`  
* **Exemplos:**
  * `feat(triagem): adiciona classificacao automatica de chamados por criticidade`
  * `fix(auth): corrige expiracao de sessao em computadores compartilhados`
  * `docs(readme): atualiza instrucoes de setup do docker-compose`

#### Fluxo de Pull Requests (PRs)
* **Critérios para aprovação:**
  1. **Resolução efetiva do problema:** O PR só será aprovado se resolver de forma comprovada o escopo da User Story/tarefa correspondente, satisfazendo todos os critérios de aceitação. PRs que não atendam serão devolvidos via solicitação de alterações (*Changes Requested*) com orientações claras do revisor.
  2. **Responsáveis pela revisão:** A revisão técnica é conduzida prioritariamente pelo **Tech Lead** ou por um **Desenvolvedor Fullstack**. Em caso de dúvidas ou impacto arquitetural crítico, o Tech Lead atua como autoridade final de aprovação.
  3. **Checklist obrigatório:** Código testado localmente, branch sincronizada com `develop`, conformidade com LGPD (sem dados sensíveis de pacientes ou credenciais expostas) e documentação atualizada.
  4. **Estratégia de merge:** Utilização de **Squash and Merge** para preservar um histórico linear, limpo e auditável na branch receptora.
* **Template de PR:** Padronizado e localizado em [`.github/pull_request_template.md`](../.github/pull_request_template.md), contendo:
  * *Descrição:* Explicação contextual do que foi alterado e qual dor foi resolvida.
  * *User Story / Tarefa Relacionada:* Vínculo rastreável (ex.: `US-01`, `Closes #12`).
  * *Tipo de Mudança:* Checkboxes dos 8 tipos convencionais.
  * *Como Testar:* Roteiro detalhado de passos para validação local pelo revisor.
  * *Checklist de Validação:* Critérios de qualidade, testes e conformidade.
  * *Screenshots / Evidências:* Capturas visuais ou logs comprovatórios.

📖 **Documento de Referência Completo:**  
👉 [Convenções de Git Flow, Branches, Commits e PR — AvanDesk-AI](https://github.com/LouisLuos/AvanDesk-AI/blob/main/docs/convencoes-git-workflow-lumen.md)

---

## 3. Definição da Stack Técnica

### 3.1 Frontend

* **Tecnologia selecionada:** **Angular (v17+) com TypeScript**
* **Alternativas consideradas:** Angular e React

#### Justificativas:
* **Domínio da equipe e facilidade:** Atualmente, a squad já possui conhecimento prévio em Angular, e o desenvolvedor responsável pelo frontend tem maior domínio e facilidade com o framework.
* **Curva de aprendizado reduzida:** A familiaridade do dev frontend com a tecnologia diminui consideravelmente a curva de aprendizado, eliminando riscos de atraso e dando maior velocidade de entrega no prazo de 5 meses.
* **Sinergia com as práticas do Java:** O Angular se encaixa perfeitamente com o mesmo estilo de programação e as melhores práticas do Java corporativo (Orientação a Objetos, tipagem estrita com TypeScript, injeção de dependências e organização estruturada em módulos, componentes e serviços).
* **Aderência ao produto:** Plataforma completa (*batteries-included*) com formulários reativos nativos (`ReactiveFormsModule`) e cliente HTTP integrado (`HttpClient`), ideal para formulários guiados de abertura de chamados e dashboards operacionais em tempo real.

---

### 3.2 Backend

* **Tecnologia / Linguagem / Framework:** **Java 17/21 com Spring Boot 3**
* **Alternativas consideradas:** C# com ASP.NET Core, Node.js com NestJS, Python com FastAPI

#### Justificativas:
* **Domínio da equipe:** A equipe tem base sólida em Programação Orientada a Objetos e no ecossistema Java. É também uma tecnologia madura e padrão de referência em soluções corporativas robustas na Avanade.
* **Produtividade e ecossistema:** `Spring Data JPA` (simplificação de persistência), `Jakarta Bean Validation` (validação de payloads) e `SpringDoc OpenAPI / Swagger` (documentação interativa automática).
* **Aderência ao produto:** Facilidade na modelagem do ciclo de vida dos chamados (máquina de estados), agendamento de tarefas (`@Scheduled`) para monitoramento de SLA e integração simples com serviços de IA via clientes HTTP tipados.

---

### 3.3 Banco de Dados

* **Tecnologia selecionada:** **PostgreSQL**
* **Tipo:** `[x] Relacional (SQL)` | `[ ] Não Relacional (NoSQL)`
* **ORM / Query Builder:** **Spring Data JPA / Hibernate** (com migrations via **Flyway**)

#### Justificativas:
* **Domínio da equipe:** A equipe já mexe bastante e tem ampla familiaridade prática com o PostgreSQL no dia a dia.
* **Aderência ao produto e integridade relacional:** O sistema de chamados do AvanDesk-AI é essencialmente relacional por natureza:
  * Precisamos relacionar quem criou o chamado, qual analista está responsável, a unidade física e o setor de ocorrência.
  * Relações explícitas de 1 para N (1:N): um usuário pode abrir múltiplos chamados ao longo do tempo, e um chamado pode ter múltiplos registros de histórico, comentários e logs de auditoria.
  * A consistência relacional (ACID) garante que não existam registros órfãos ou inconsistências na apuração de SLAs e métricas de atendimento.

---

### 3.4 Autenticação e Autorização

* **Estratégia / Ferramenta:** **JWT (JSON Web Token)** com **Spring Security**
* **Níveis de Acesso (RBAC — Role-Based Access Control):**
  1. `ANALISTA`: Profissional de suporte técnico que faz login na plataforma para visualizar a fila de triagem, assumir atendimentos, interagir e registrar soluções técnicas com seu nível de acesso.
  2. `ADMINISTRADOR (ADMIN PAI)`: Usuário gestor master ("admin pai") com nível elevado de acesso para gerenciar usuários, parametrizar unidades, configurar regras de SLA e supervisionar os indicadores globais da plataforma.
  3. `SOLICITANTE`: Colaboradores das unidades da Rede Lúmen (recepção, coleta, enfermagem) que registram ocorrências e acompanham o status de suas solicitações.

#### Justificativas:
* **O básico que funciona perfeitamente:** Sendo o AvanDesk-AI uma **plataforma interna corporativa**, não há necessidade de autenticações externas complexas (como login social via Google). O JWT entrega tudo o que é necessário de forma limpa, direta e sem overhead.
* **Segurança stateless:** O token com tempo de expiração e assinatura digital funciona com máxima eficiência em APIs REST, atendendo aos requisitos de segurança sem sobrecarregar a memória do servidor com sessões.
* **Facilidade de integração:** No frontend Angular, o token Bearer é transmitido automaticamente via `HttpInterceptor`, e as rotas são protegidas de acordo com as permissões de cada perfil.

---

### 3.5 Testes

* **Ferramentas / Frameworks:**
  * **Backend (Java):** **JUnit 5** e **Mockito** (Spring Boot Test).
  * **Frontend (Angular):** **Jasmine** e **Karma**.
* **Estratégia inicial:** Adotar a abordagem de testes **mais tranquila, simples e direta** para quem está aprendendo e começando a mexer com testes no ecossistema Java.
* **Foco prioritário:**
  * Testes unitários com asserções claras do JUnit 5 (`assertEquals`, `assertTrue`, `assertNotNull`) para validar as regras essenciais de negócio (criação de chamados, regras de priorização e validação de campos obrigatórios).
  * Uso básico do Mockito (`@Mock`, `@InjectMocks`) para isolar dependências em serviços de forma descomplicada, priorizando o aprendizado prático da equipe antes de avançar para cenários complexos.

---

### 3.6 Publicação e Hospedagem (DevOps / Deploy)

* **Frontend:** **Vercel**
* **Backend / API:** **Render**
* **Banco de Dados Gerenciado:** **Supabase** (PostgreSQL gerenciado)
* **CI/CD:** **GitHub Actions**

#### Justificativas:
* **Experiência prévia do Tech Lead:** O Tech Lead (Luiz Henrique) já possui vivência prática e já trabalhou com deploy de backend no **Render** e frontend na **Vercel**, garantindo agilidade imediata e facilidade de configuração sem perda de tempo em curva de aprendizado de infraestrutura.
* **Banco de Dados no Supabase:** Instância gerenciada e confiável de PostgreSQL na nuvem, com ótima interface de monitoramento e tier gratuito ideal para o ciclo de desenvolvimento.
* **CI/CD com GitHub Actions:** Ferramenta gratuita, nativa do GitHub e de simples automação, permitindo validar builds e testes a cada Pull Request antes de integrar na branch `develop`.
* **Custo zero e eficiência:** Arquitetura 100% gratuita para o desenvolvimento do MVP, eliminando custos de infraestrutura e viabilizando entregas contínuas durante os 5 meses de projeto.

---

## 4. Checklist de Conclusão da Semana 1

- [x] Repositório ou Organização do Squad criada no GitHub.
- [x] Todos os integrantes adicionados com permissões de leitura/escrita.
- [x] Decisão formalizada entre monorepositório vs. múltiplos repositórios.
- [x] README.md inicial publicado com contexto, dores do cliente e objetivos.
- [x] .gitignore apropriado adicionado de acordo com as stacks planejadas (Angular, Java/Spring, Node, OS, IDEs).
- [x] Estrutura inicial de pastas criada e versionada no Git (`backend/`, `frontend/`, `docs/`, `.github/`).
- [x] Convenções de branch, commit (Conventional Commits) e PR acordadas com o squad.
- [x] Stack técnica apresentada, justificada e alinhada com todos os membros.
- [x] Proposta revisada e pronta para apresentação na mentoria com a Avanade.
