# Nexo API — Backend

> API REST do sistema de suporte Nexo (Rede Lúmen Diagnósticos)  
> **Stack:** Java 21 · Spring Boot 4.0.2 · PostgreSQL 16 · Flyway · Spring Security

---

## Dependências Principais

| Dependência | Propósito |
|---|---|
| Spring Web (MVC) | Controllers REST, servidor Tomcat embarcado |
| Spring Data JPA | Repositórios e mapeamento ORM com Hibernate |
| Spring Security | Autenticação JWT e autorização RBAC |
| Validation | Jakarta Bean Validation (`@NotNull`, `@Size`, etc.) |
| Flyway Migration | Controle de versão do schema do banco |
| PostgreSQL Driver | Driver JDBC para conectar no PostgreSQL |
| Spring Boot DevTools | Hot reload automático em desenvolvimento |
| Lombok | Redução de boilerplate (`@Getter`, `@Builder`, etc.) |
| SpringDoc OpenAPI | Swagger UI e documentação interativa da API |

## Comandos

```bash
# Compilar
./mvnw clean compile

# Rodar testes
./mvnw test

# Iniciar aplicação (banco Docker precisa estar rodando!)
./mvnw spring-boot:run
```

## Login temporário (até a HU09)

Até a autenticação JWT da HU09, a API usa login básico (HTTP Basic) com os usuários da tabela `usuarios`. Em desenvolvimento, o seed `db/seed/R__dados_dev.sql` cria três usuários de teste, todos com a senha `nexo-dev-2026`:

| E-mail | Perfil |
|---|---|
| `analista1@lumen.local` | `ANALISTA_SUPORTE` |
| `analista2@lumen.local` | `ANALISTA_SUPORTE` |
| `lideranca@lumen.local` | `LIDERANCA_TI` |

Esses usuários existem só em desenvolvimento: o perfil `prod` não carrega a pasta `db/seed`.

```bash
curl -u analista1@lumen.local:nexo-dev-2026 http://localhost:8080/api/v1/unidades
```

## Endpoints Disponíveis

| Endpoint | Descrição |
|----------|-----------|
| `GET /health` | Health check da API |
| `GET /swagger-ui` | Swagger UI (documentação interativa) |
| `GET /api-docs` | OpenAPI spec (JSON) |
