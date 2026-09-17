# AvanDesk-AI — Rede Lúmen Diagnósticos

> Plataforma inteligente para registro, triagem e acompanhamento de chamados de suporte técnico em saúde e medicina diagnóstica.

Projeto desenvolvido no âmbito da **Residência Tecnológica Avanade 2026.2**, em parceria com o **Porto Digital**, a **CESAR School** e o programa **Embarque Digital**.

---

## 👥 Equipe — Squad 01

| Nome | Cargo / Papel |
|------|---------------|
| Luiz Henrique Souza | Tech Lead / Fullstack |
| Aguinaldo Anselmo da Costa Neto | Desenvolvedor Fullstack |
| Sarah Cyrne Ferreira | Líder — Product & Delivery Lead / UX-UI & Requisitos |
| Elis Maidi Tenório Chaprão | Backend & Banco de Dados |
| Marcos Antônio Taveira Fraga | QA, DevOps & Qualidade |
| Pedro Vinicius Silva de Souza | Frontend |

---

## 🏥 Contexto do Cliente: Rede Lúmen Diagnósticos

A **Rede Lúmen Diagnósticos** é uma rede fictícia de saúde e medicina diagnóstica (referência de mercado: *Dasa*) que realiza exames laboratoriais e de imagem:

* **Escala Operacional:** 14 unidades distribuídas em Pernambuco (sendo duas unidades operando 24 horas).
* **Público Atendido:** Pacientes particulares, convênios médicos, empresas e instituições hospitalares.
* **Corpo Colaborador:** Aproximadamente 900 profissionais (recepção, técnicos de laboratório, enfermagem, médicos, áreas administrativas e gestores).
* **Equipe de TI & Suporte:** Time enxuto de 7 analistas de suporte (com atendimento remoto e deslocamento presencial volante), responsável por sistemas corporativos, agendamento, recepção/autorização, impressoras de etiquetas de coleta, estações de trabalho e infraestrutura de rede.

---

## ⚠️ Desafios e Dores Operacionais

1. **Canais Fragmentados e Falta de Rastreabilidade:** Chamados chegam de forma caótica via grupos de WhatsApp, ligações, e-mails, conversas paralelas e planilhas isoladas, sem identificador único (`Ticket ID`), gerando retrabalho e perda de histórico.
2. **"Tudo é Urgente" vs. Impacto Assistencial Real:** Gestores de unidade tendem a classificar qualquer ocorrência local como prioridade máxima. O suporte precisa de inteligência para arbitrar a fila diferenciando um bloqueio na coleta/exame de paciente de um problema administrativo secundário.
3. **Falta de Visibilidade Operacional:** Dificuldade em identificar rapidamente quais analistas estão alocados em cada incidente, quais problemas dependem de fornecedores externos de sistemas e quais unidades possuem maior recorrência de falhas.
4. **Privacidade e LGPD na Saúde:** Nas solicitações de suporte, funcionários frequentemente anexam capturas de tela com nomes, CPFs ou dados médicos. O sistema de suporte **não deve armazenar resultados de exames nem prontuários clínicos**, registrando estritamente os dados técnicos necessários para a resolução.
5. **Estações Compartilhadas:** Múltiplos profissionais compartilham computadores nas recepções e laboratórios com ritmos de trabalho intensos, exigindo sessões seguras e formulários rápidos de preenchimento.

---

## 💡 Como o AvanDesk-AI Transforma essa Realidade

O **AvanDesk-AI** atua como o hub central inteligente de atendimento da Rede Lúmen Diagnósticos:

* 🏷️ **Identificador Único e Centralização:** Cada chamado recebe um protocolo exclusivo, agrupando históricos de mensagens, anexos técnicos e status de resolução em um só lugar.
* 🤖 **Triagem Inteligente com IA:** Algoritmos analisam a descrição do ticket e calculam o nível de criticidade com base no impacto real na cadeia assistencial (ex: impressora de etiquetas da triagem laboratoral parada tem prioridade superior a impressora administrativa de escritório).
* 📝 **Entrada de Dados Guiada:** Sugere campos indispensáveis no momento da abertura (ex: identificação da unidade, setor, equipamento afetado e horário), reduzindo o vaivém de mensagens.
* ⏱️ **Gestão de SLA e Fornecedores:** Alertas automáticos para chamados próximos ao vencimento e acompanhamento transparente de incidentes encaminhados a parceiros externos (ex: sistemas laboratoriais de terceiros).
* 🔒 **Segurança e Conformidade com LGPD:** Validação de anexos e orientações ativas para evitar a exposição indevida de dados clínicos de pacientes em tíquetes de suporte.

---

## 🛠️ Tecnologias & Arquitetura

| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| **Frontend** | Angular + TypeScript + Tailwind CSS | Angular 22 · Tailwind CSS v4 |
| **Backend** | Java + Spring Boot (Maven) | Java 21 · Spring Boot 4.0.2 |
| **Banco de Dados** | PostgreSQL | 16+ |
| **Migrations** | Flyway | 11.x (`spring-boot-starter-flyway`) |
| **ORM** | Spring Data JPA / Hibernate | Gerenciado pelo Spring Boot BOM |
| **Auth** | JWT + Spring Security | Gerenciado pelo Spring Boot BOM |
| **Docs API** | SpringDoc OpenAPI / Swagger | 3.1.0 |
| **Containers** | Docker + Docker Compose | Ambiente local |
| **CI/CD** | GitHub Actions | — |
| **Deploy** | Vercel (front) · Render (back) · Supabase (DB) | Produção / Staging |

### Justificativa da Stack

| Aspecto | Decisão |
|---------|---------|
| **Frontend (Angular)** | Domínio prévio do dev frontend, sinergia com POO/TypeScript, formulários reativos nativos para abertura de chamados |
| **Backend (Java + Spring Boot)** | Base sólida da equipe em Java/OOP, ecossistema maduro e alinhado à Avanade, produtividade com Spring Data JPA e Bean Validation |
| **Banco (PostgreSQL)** | Familiaridade da equipe, integridade relacional ACID essencial para SLAs e auditoria de chamados |
| **Auth (JWT + Spring Security)** | Solução stateless, simples e direta para plataforma interna, sem overhead de auth social |
| **Testes** | JUnit 5 + Mockito (backend), Vitest (frontend) — abordagem didática e progressiva |
| **Deploy** | Custo zero para MVP — experiência prévia do Tech Lead com Vercel/Render |

---

## 📋 Pré-requisitos

Antes de começar, certifique-se de ter instalado:

| Ferramenta | Versão | Download |
|------------|--------|----------|
| **JDK** | 21 (Eclipse Temurin) | [adoptium.net](https://adoptium.net/) |
| **Node.js** | 22+ (LTS) | [nodejs.org](https://nodejs.org/) |
| **Docker Desktop** | 27+ | [docker.com](https://www.docker.com/products/docker-desktop/) |
| **Maven** | 3.9+ (ou usar `mvnw`) | incluso no projeto |
| **Angular CLI** | 21 | `npm install -g @angular/cli@21` |
| **Git** | 2.x+ | [git-scm.com](https://git-scm.com/) |

### Verificação rápida

```bash
java -version       # → openjdk 21.x.x
node -v             # → v22.x.x
npm -v              # → 10.x.x
docker --version    # → Docker 27.x.x
mvn -v              # → Maven 3.9.x (opcional se usar mvnw)
ng version          # → Angular CLI 21.x.x
git --version       # → git 2.x.x
```

---

## 🚀 Configuração do Ambiente & Comandos de Execução

### 1. Clonar o repositório e configurar variáveis

```bash
git clone https://github.com/LouisLuos/AvanDesk-AI.git
cd AvanDesk-AI

# Copiar o arquivo de variáveis de ambiente
cp .env.example .env
```

### 2. Subir o Banco de Dados (PostgreSQL via Docker)

```bash
# Subir o banco em background
docker compose up -d

# Verificar se está rodando e saudável
docker compose ps

# Conectar no banco via terminal (opcional, para debug)
docker exec -it avandesk-db psql -U avandesk -d avandesk_db

# Parar o banco (dados persistem no volume)
docker compose down

# Parar e APAGAR todos os dados (reset total)
docker compose down -v
```

### 3. Iniciar o Backend

```bash
cd backend

# Compilar o projeto
./mvnw clean compile

# Rodar os testes
./mvnw test

# Iniciar a aplicação (banco Docker precisa estar rodando!)
./mvnw spring-boot:run
# API disponível em http://localhost:8080
# Health check em http://localhost:8080/health
# Swagger UI em http://localhost:8080/swagger-ui
```

### 4. Iniciar o Frontend

```bash
cd frontend

# Instalar dependências
npm install

# Rodar o servidor de desenvolvimento
ng serve
# Aplicação disponível em http://localhost:4200

# Rodar os testes
ng test --watch=false
```

---

## 🔐 Variáveis de Ambiente

Todas as variáveis estão documentadas no arquivo [`.env.example`](./.env.example). Copie-o para `.env` e ajuste conforme necessário:

| Variável | Descrição | Valor padrão (dev) |
|----------|-----------|-------------------|
| `POSTGRES_DB` | Nome do banco de dados | `avandesk_db` |
| `POSTGRES_USER` | Usuário do PostgreSQL | `avandesk` |
| `POSTGRES_PASSWORD` | Senha do PostgreSQL | `avandesk_dev` |
| `DATABASE_URL` | URL JDBC de conexão | `jdbc:postgresql://localhost:5433/avandesk_db` |
| `SPRING_PROFILES_ACTIVE` | Perfil ativo do Spring Boot | `dev` |
| `SERVER_PORT` | Porta da API backend | `8080` |
| `JWT_SECRET` | Chave secreta para assinatura JWT | *(definir em produção)* |
| `JWT_EXPIRATION` | Tempo de expiração do token (ms) | `86400000` (24h) |

> ⚠️ **LGPD & Segurança:** Nunca commitar o arquivo `.env` com credenciais reais. O `.gitignore` já exclui este arquivo.

---

## 📁 Estrutura do Repositório

```text
AvanDesk-AI/
├── .github/
│   ├── workflows/
│   │   └── ci.yml                          ← Pipeline CI/CD (GitHub Actions)
│   ├── convencoes-git-workflow-lumen.md    ← Convenções de Git Flow e Commits
│   └── pull_request_template.md           ← Template padrão de Pull Request
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/avandesk/api/
│   │   │   │   ├── controller/
│   │   │   │   │   └── HealthController.java
│   │   │   │   └── ApiApplication.java
│   │   │   └── resources/
│   │   │       ├── db/migration/
│   │   │       │   └── V1__init_schema.sql
│   │   │       ├── application.yml
│   │   │       └── application-prod.yml
│   │   └── test/
│   ├── mvnw / mvnw.cmd
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── app/
│   │   │   ├── core/                      ← Serviços singleton, interceptors, guards
│   │   │   ├── shared/                    ← Componentes, pipes, diretivas reutilizáveis
│   │   │   ├── features/                  ← Módulos de feature (lazy-loaded)
│   │   │   ├── layouts/                   ← Layouts (main, auth)
│   │   │   └── models/                    ← Interfaces TypeScript e DTOs
│   │   ├── environments/
│   │   └── styles.css                     ← Tailwind CSS entry point
│   ├── .postcssrc.json
│   ├── proxy.conf.json
│   └── package.json
├── docs/
│   ├── 01-analise-de-dominio.md
│   ├── 02-requisitos-funcionais.md
│   ├── 03-requisitos-nao-funcionais.md
│   ├── 04-historias-de-usuario.md
│   ├── 05-proximos-passos.md
│   ├── definicao-tecnica-ambiente.md
│   └── plano-setup-ambiente.md
├── .env.example
├── .gitignore
├── docker-compose.yml
└── README.md
```

---

## 📚 Documentação e Padrões de Engenharia

* 📑 [Convenções de Git Flow, Branches e Commits](./.github/convencoes-git-workflow-lumen.md)
* 📋 [Template Oficial de Pull Request](./.github/pull_request_template.md)
* 🛠️ [Plano de Setup do Ambiente](./docs/plano-setup-ambiente.md)
* ⚙️ [Definição Técnica & Ambiente](./docs/definicao-tecnica-ambiente.md)
* 📊 [Análise de Domínio](./docs/01-analise-de-dominio.md)
* ⚙️ [Requisitos Funcionais](./docs/02-requisitos-funcionais.md)
* 🛡️ [Requisitos Não Funcionais](./docs/03-requisitos-nao-funcionais.md)
* 📖 [Histórias de Usuário & Tasks](./docs/04-historias-de-usuario.md)
* 🧭 [Próximos Passos & Roadmap](./docs/05-proximos-passos.md)
