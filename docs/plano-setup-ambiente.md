# 🛠️ Plano de Setup — Ambiente de Desenvolvimento

> Projeto: AvanDesk-AI — Rede Lúmen Diagnósticos  
> Documento de referência para o setup inicial do Frontend, Backend e Banco de Dados.  
> Data de criação: 14 de Setembro de 2026

---

## 1. Stack Técnica Confirmada

| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| **Frontend** | Angular + TypeScript + SCSS | Angular 21 (LTS até mai/2027) |
| **Backend** | Java + Spring Boot (Maven) | Java 21 + Spring Boot 4.1.x |
| **Banco de Dados** | PostgreSQL | 16+ |
| **Migrations** | Flyway | 11.x (`spring-boot-starter-flyway`) |
| **ORM** | Spring Data JPA / Hibernate | Gerenciado pelo Spring Boot BOM |
| **Auth** | JWT + Spring Security | Gerenciado pelo Spring Boot BOM |
| **Docs API** | SpringDoc OpenAPI / Swagger | Gerenciado pelo Spring Boot BOM |
| **Containers** | Docker + Docker Compose | Ambiente local |
| **Deploy** | Vercel (front) · Render (back) · Supabase (DB) | Produção/Staging |
| **CI/CD** | GitHub Actions | Repositório GitHub |

---

## 2. Pré-requisitos na Máquina de Cada Desenvolvedor

Antes de iniciar, garantir que todos os devs tenham instalado:

- [ ] **JDK 21** (recomendado: Eclipse Temurin / Adoptium) — [Download](https://adoptium.net/)
- [ ] **Node.js 22+** (LTS) — [Download](https://nodejs.org/)
- [ ] **Docker Desktop** — [Download](https://www.docker.com/products/docker-desktop/)
- [ ] **Maven 3.9+** (ou usar o wrapper `mvnw` que vem no projeto gerado)
- [ ] **Angular CLI 21** — instalar com: `npm install -g @angular/cli@21`
- [ ] **Git** configurado com nome e e-mail (`git config --global user.name` / `user.email`)

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

## 3. Etapa 1 — Banco de Dados (PostgreSQL via Docker)

### 3.1 Criar o `docker-compose.yml` na raiz do projeto

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: avandesk-db
    restart: unless-stopped
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: avandesk_db
      POSTGRES_USER: avandesk
      POSTGRES_PASSWORD: avandesk_dev
    volumes:
      - avandesk_pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U avandesk -d avandesk_db"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  avandesk_pgdata:
    driver: local
```

### 3.2 Criar o `.env.example` na raiz do projeto

> Arquivo de referência com as variáveis de ambiente necessárias. Cada dev deve copiar para `.env` local (que já está no `.gitignore`).

```env
# ===== Banco de Dados (Docker Local) =====
POSTGRES_DB=avandesk_db
POSTGRES_USER=avandesk
POSTGRES_PASSWORD=avandesk_dev
DATABASE_URL=jdbc:postgresql://localhost:5432/avandesk_db

# ===== Banco de Dados (Supabase - Produção) =====
# SUPABASE_URL=
# SUPABASE_DB_URL=
# SUPABASE_ANON_KEY=

# ===== JWT =====
# JWT_SECRET=
# JWT_EXPIRATION=86400000

# ===== Spring Boot =====
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080
```

### 3.3 Comandos para gerenciar o banco

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

### 3.4 Validação

- [ ] `docker compose up -d` roda sem erros
- [ ] `docker compose ps` mostra status `healthy`
- [ ] Conexão via `psql` funciona (ou via DBeaver/pgAdmin)

---

## 4. Etapa 2 — Backend (Spring Boot 4.1 + Java 21)

### 4.1 Gerar o projeto via Spring Initializr

Acessar [start.spring.io](https://start.spring.io) e configurar:

| Campo | Valor |
|---|---|
| **Project** | Maven |
| **Language** | Java |
| **Spring Boot** | 4.1.1 (ou patch mais recente 4.1.x) |
| **Group** | `com.avandesk` |
| **Artifact** | `api` |
| **Name** | `AvanDesk API` |
| **Description** | API REST do sistema de suporte AvanDesk-AI |
| **Package name** | `com.avandesk.api` |
| **Packaging** | Jar |
| **Java** | 21 |

**Dependências para adicionar:**

| Dependência | Propósito |
|---|---|
| `Spring Web` | Controllers REST, servidor Tomcat embarcado |
| `Spring Data JPA` | Repositórios e mapeamento ORM com Hibernate |
| `Spring Security` | Autenticação JWT e autorização RBAC |
| `Validation` | Jakarta Bean Validation (`@NotNull`, `@Size`, etc.) |
| `Flyway Migration` | Controle de versão do schema do banco |
| `PostgreSQL Driver` | Driver JDBC para conectar no PostgreSQL |
| `Spring Boot DevTools` | Hot reload automático em desenvolvimento |
| `Lombok` | Redução de boilerplate (`@Getter`, `@Builder`, etc.) |

> **Nota:** `SpringDoc OpenAPI` (Swagger) não está no Initializr — adicionar manualmente no `pom.xml` após gerar o projeto.

### 4.2 Extrair o projeto gerado

Após clicar em **Generate**, extrair o conteúdo do `.zip` **dentro da pasta `backend/`** do repositório.

A estrutura resultante deve ser:

```text
backend/
├── src/
│   ├── main/
│   │   ├── java/com/avandesk/api/
│   │   │   └── ApiApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/         ← criar esta pasta
│   └── test/
│       └── java/com/avandesk/api/
│           └── ApiApplicationTests.java
├── .gitignore                         ← gerado pelo Initializr
├── mvnw / mvnw.cmd                    ← Maven Wrapper
├── pom.xml
└── README.md                          ← já existente, atualizar
```

### 4.3 Configurar o `application.yml`

Renomear `application.properties` para `application.yml` e configurar:

```yaml
spring:
  application:
    name: avandesk-api

  datasource:
    url: jdbc:postgresql://localhost:5432/avandesk_db
    username: avandesk
    password: avandesk_dev
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: none          # Flyway gerencia o schema, nunca o Hibernate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  flyway:
    enabled: true
    locations: classpath:db/migration
    schemas: public

server:
  port: 8080

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui
```

### 4.4 Criar perfil de produção `application-prod.yml`

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}

  jpa:
    show-sql: false

  flyway:
    enabled: true
```

### 4.5 Adicionar dependência do SpringDoc OpenAPI no `pom.xml`

Adicionar dentro de `<dependencies>`:

```xml
<!-- SpringDoc OpenAPI / Swagger UI -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.8</version>
</dependency>
```

E adicionar a dependência do Flyway para PostgreSQL (se não veio automaticamente):

```xml
<!-- Flyway PostgreSQL -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

### 4.6 Criar migration inicial (placeholder)

Criar o arquivo `backend/src/main/resources/db/migration/V1__init_schema.sql`:

```sql
-- =============================================
-- V1__init_schema.sql
-- Migration inicial do AvanDesk-AI
-- Schema sera populado nas User Stories de
-- autenticacao e abertura de chamados (US-007)
-- =============================================
```

### 4.7 Validação

```bash
cd backend

# Compilar o projeto (verifica dependências e código)
./mvnw clean compile

# Rodar os testes (contexto Spring deve subir)
./mvnw test

# Iniciar a aplicação (banco Docker precisa estar rodando!)
./mvnw spring-boot:run
```

- [ ] `./mvnw clean compile` — compila sem erros
- [ ] `./mvnw test` — testes padrão passam
- [ ] `./mvnw spring-boot:run` — API sobe na porta `8080`
- [ ] Flyway executa a migration V1 (verificar no log: `Successfully applied 1 migration`)
- [ ] Acessar `http://localhost:8080/swagger-ui` — Swagger UI carrega

---

## 5. Etapa 3 — Frontend (Angular 21)

### 5.1 Gerar o projeto via Angular CLI

Na raiz do repositório, rodar:

```bash
cd frontend

# Gerar o projeto Angular dentro da pasta frontend/
npx -p @angular/cli@21 ng new avandesk-frontend --directory ./ --routing --style scss --skip-git --skip-tests=false --ssr=false
```

| Flag | Motivo |
|---|---|
| `--directory ./` | Gera dentro de `frontend/` (pasta já existente) |
| `--routing` | Habilita módulo de rotas desde o início |
| `--style scss` | SCSS para estilização avançada |
| `--skip-git` | Não inicializa novo `.git` (já estamos no monorepositório) |
| `--skip-tests=false` | Mantém scaffolding de testes (Jasmine/Karma) |
| `--ssr=false` | Sem Server-Side Rendering (é uma SPA corporativa interna) |

### 5.2 Organizar estrutura modular de pastas

Após o scaffolding, criar a estrutura dentro de `frontend/src/app/`:

```bash
# Criar pastas da arquitetura modular
mkdir -p src/app/core/interceptors
mkdir -p src/app/core/guards
mkdir -p src/app/core/services
mkdir -p src/app/shared/components
mkdir -p src/app/shared/pipes
mkdir -p src/app/shared/directives
mkdir -p src/app/features
mkdir -p src/app/layouts
mkdir -p src/app/models
```

Estrutura resultante:

```text
frontend/src/app/
├── core/              → Serviços singleton, interceptors HTTP, guards de rota
│   ├── interceptors/  → HttpInterceptor para injetar token JWT
│   ├── guards/        → Proteção de rotas por papel (ANALISTA, ADMIN, SOLICITANTE)
│   └── services/      → AuthService, ApiService, etc.
├── shared/            → Componentes, pipes e diretivas reutilizáveis
│   ├── components/    → Botões, modais, cards, etc.
│   ├── pipes/         → Formatação de datas, status, etc.
│   └── directives/    → Diretivas customizadas
├── features/          → Módulos de feature (lazy-loaded)
│   ├── auth/          → Login, registro, reset de senha
│   ├── chamados/      → Abertura, listagem, detalhes de chamados
│   ├── dashboard/     → Painel gerencial com métricas
│   └── admin/         → Gestão de usuários, configurações
├── layouts/           → Layouts da aplicação
│   ├── main-layout/   → Layout principal (com sidebar e header)
│   └── auth-layout/   → Layout de autenticação (tela limpa)
└── models/            → Interfaces TypeScript e DTOs
    ├── usuario.model.ts
    ├── chamado.model.ts
    └── api-response.model.ts
```

### 5.3 Configurar ambientes (environments)

Criar os arquivos de ambiente:

**`frontend/src/environments/environment.ts`**
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'
};
```

**`frontend/src/environments/environment.prod.ts`**
```typescript
export const environment = {
  production: true,
  apiUrl: '${API_URL}'  // Injetado via variável de ambiente no deploy
};
```

### 5.4 Configurar proxy para desenvolvimento

Criar `frontend/proxy.conf.json` para redirecionar chamadas ao backend local:

```json
{
  "/api/*": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```

Atualizar o `angular.json` para usar o proxy no `ng serve`:

```json
"serve": {
  "options": {
    "proxyConfig": "proxy.conf.json"
  }
}
```

### 5.5 Validação

```bash
cd frontend

# Instalar dependências
npm install

# Rodar o servidor de desenvolvimento
ng serve
# ou: npm start

# Rodar os testes
ng test --watch=false
```

- [ ] `npm install` — instala sem erros
- [ ] `ng serve` — compila e sobe na porta `4200`
- [ ] Acessar `http://localhost:4200` — página padrão do Angular carrega
- [ ] `ng test --watch=false` — testes padrão passam

---

## 6. Etapa 4 — Atualizações de Documentação

### 6.1 Atualizar `README.md` da raiz

Substituir a seção "Tecnologias & Arquitetura" (atualmente com placeholder `⏳`) por:

```markdown
## 🛠️ Tecnologias & Arquitetura

| Camada | Tecnologia |
|--------|-----------|
| Frontend | Angular 21 · TypeScript · SCSS |
| Backend | Java 21 · Spring Boot 4.1 · Spring Security |
| Banco de Dados | PostgreSQL 16 · Flyway (migrations) |
| ORM | Spring Data JPA / Hibernate |
| Auth | JWT (JSON Web Token) + RBAC |
| Docs API | SpringDoc OpenAPI / Swagger |
| CI/CD | GitHub Actions |
| Deploy | Vercel (front) · Render (back) · Supabase (DB) |
```

Adicionar seção de Quickstart:

```markdown
## 🚀 Quickstart (Ambiente Local)

### Pré-requisitos
- JDK 21 (Temurin)
- Node.js 22+
- Docker Desktop
- Angular CLI 21 (`npm i -g @angular/cli@21`)

### 1. Banco de Dados
docker compose up -d

### 2. Backend
cd backend
./mvnw spring-boot:run

### 3. Frontend
cd frontend
npm install
ng serve
```

Atualizar a árvore de estrutura do repositório para refletir a nova organização.

### 6.2 Atualizar `backend/README.md`

Remover o placeholder `⏳ Aguardando definição` e documentar a stack definida (Java 21 + Spring Boot 4.1) com comandos para rodar.

### 6.3 Atualizar `docs/definicao-tecnica-ambiente.md`

Atualizar as referências de versão:
- "Spring Boot 3" → "Spring Boot 4.1"
- "Angular (v17+)" → "Angular 21"

### 6.4 Atualizar `.gitignore`

Adicionar regras específicas para o setup do monorepositório:

```gitignore
# Maven Wrapper (jar baixado localmente)
.mvn/wrapper/maven-wrapper.jar

# Docker override local
docker-compose.override.yml
```

---

## 7. Ordem de Execução e Commits

Seguindo as [convenções de commits do projeto](../.github/convencoes-git-workflow-lumen.md), a execução será feita nesta branch (`chore/setup-ambiente-desenvolvimento`) com commits atômicos:

| # | Escopo | Comando de Commit |
|---|--------|-------------------|
| 1 | Docker + Banco | `chore(infra): adiciona docker-compose com PostgreSQL e .env.example` |
| 2 | Backend | `chore(backend): inicializa projeto Spring Boot 4.1 com Java 21 e dependencias` |
| 3 | Backend config | `chore(backend): configura application.yml, Flyway e perfil de producao` |
| 4 | Frontend | `chore(frontend): inicializa projeto Angular 21 com estrutura modular` |
| 5 | Frontend config | `chore(frontend): configura ambientes, proxy e arquitetura de pastas` |
| 6 | Documentação | `docs(readme): atualiza stack tecnologica e instrucoes de quickstart` |
| 7 | Gitignore | `chore(gitignore): adiciona regras para Maven e Docker` |

### Fluxo após finalizar

```bash
# 1. Push da branch
git push origin chore/setup-ambiente-desenvolvimento

# 2. Abrir Pull Request → develop
#    Título: chore(setup): inicializa ambiente de desenvolvimento com Spring Boot 4.1, Angular 21 e PostgreSQL
#    Usar o template de PR em .github/pull_request_template.md

# 3. Após aprovação, fazer Squash and Merge na develop

# 4. Periodicamente, sincronizar develop → main via PR
```

---

## 8. Checklist Geral de Conclusão

- [ ] Docker Compose sobe PostgreSQL sem erros e com status `healthy`
- [ ] Backend compila (`./mvnw clean compile`)
- [ ] Backend sobe na porta 8080 (`./mvnw spring-boot:run`)
- [ ] Flyway executa migration V1 com sucesso
- [ ] Swagger UI acessível em `http://localhost:8080/swagger-ui`
- [ ] Frontend compila e serve em `http://localhost:4200`
- [ ] Testes do backend passam (`./mvnw test`)
- [ ] Testes do frontend passam (`ng test --watch=false`)
- [ ] README atualizado com stack e quickstart
- [ ] Estrutura de pastas modular criada no frontend
- [ ] Environments configurados (dev e prod)
- [ ] Proxy de desenvolvimento configurado
- [ ] `.gitignore` atualizado
- [ ] PR aberto seguindo o template do projeto

---

*Documento complementar ao [Plano de Próximos Passos](./05-proximos-passos.md) e à [Definição Técnica de Ambiente](./definicao-tecnica-ambiente.md).*
