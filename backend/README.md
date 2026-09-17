# AvanDesk API — Backend

> API REST do sistema de suporte AvanDesk-AI  
> **Stack:** Java 21 · Spring Boot 4.0 · PostgreSQL 16 · Flyway · Spring Security

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

## Endpoints Disponíveis

| Endpoint | Descrição |
|----------|-----------|
| `GET /health` | Health check da API |
| `GET /swagger-ui` | Swagger UI (documentação interativa) |
| `GET /api-docs` | OpenAPI spec (JSON) |
