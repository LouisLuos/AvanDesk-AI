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

## Estrutura Modular (Package-by-Feature)

A aplicação é organizada por **funcionalidades/módulos de domínio** (*Vertical Slice / Package-by-Feature*), agrupando controladores, serviços, regras e entidades dentro do seu respectivo contexto:

```text
com.avandesk.api/
├── AvanDeskApiApplication.java
├── core/                  # Infraestrutura transversal, config de segurança, tratamento de erros e DTOs globais
│   ├── config/            # SecurityConfig, NexoProperties
│   ├── dto/               # ApiResponse genérico
│   ├── exception/         # GlobalExceptionHandler, RecursoNaoEncontradoException, RegraNegocioException
│   └── health/            # HealthController
├── usuario/               # Gestão de usuários, autenticação JWT e segurança
│   ├── controller/        # AuthController
│   ├── dto/               # LoginRequest, LoginResponse, UsuarioAutenticadoResponse
│   ├── entity/            # Usuario, PerfilUsuario
│   ├── repository/        # UsuarioRepository
│   ├── security/          # JwtAuthenticationFilter
│   └── service/           # AuthService, JwtService, UsuarioDetailsService, UsuarioLogadoService
├── chamado/               # Ciclo de vida de chamados, eventos de histórico e solicitantes
│   ├── controller/        # ChamadoController
│   ├── dto/               # ChamadoRequest, ChamadoResponse, SolicitanteRequest
│   ├── entity/            # Chamado, EventoHistorico, Solicitante, Enums
│   ├── repository/        # ChamadoRepository, EventoHistoricoRepository, SolicitanteRepository
│   └── service/           # ChamadoService, EventoHistoricoService, SolicitanteService
├── unidade/               # Gestão de unidades de atendimento hospitalar/diagnóstico
│   ├── controller/        # UnidadeController
│   ├── dto/               # UnidadeRequest, UnidadeResponse
│   ├── entity/            # Unidade
│   ├── repository/        # UnidadeRepository
│   └── service/           # UnidadeService
└── catalogo/              # Catálogos de categorias e sistemas/equipamentos
    ├── controller/        # CategoriaController, SistemaEquipamentoController
    ├── dto/               # CategoriaResponse, SistemaEquipamentoResponse
    ├── entity/            # Categoria, SistemaEquipamento
    ├── repository/        # CategoriaRepository, SistemaEquipamentoRepository
    └── service/           # CategoriaService, SistemaEquipamentoService
```

## Comandos

```bash
# Compilar
./mvnw clean compile

# Rodar testes
./mvnw test

# Iniciar aplicação (banco Docker precisa estar rodando!)
./mvnw spring-boot:run
```

## Autenticação JWT (HU09)

A API utiliza autenticação stateless via **Bearer Token JWT** assinado com HMAC-SHA256 (validade de 1 hora, RNF-007). Em desenvolvimento, o seed `db/seed/R__dados_dev.sql` cria três usuários de teste, todos com a senha `nexo-dev-2026`:

| E-mail | Perfil |
|---|---|
| `analista1@lumen.local` | `ANALISTA_SUPORTE` |
| `analista2@lumen.local` | `ANALISTA_SUPORTE` |
| `lideranca@lumen.local` | `LIDERANCA_TI` |

Esses usuários existem só em desenvolvimento: o perfil `prod` não carrega a pasta `db/seed`.

### Fluxo de Login:
1. Obtenha o token de acesso:
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"analista1@lumen.local","senha":"nexo-dev-2026"}'
```

2. Utilize o Bearer token retornado para consumir as rotas protegidas:
```bash
curl -H "Authorization: Bearer <access_token>" http://localhost:8080/api/v1/unidades
```

## Endpoints Disponíveis

| Endpoint | Descrição |
|----------|-----------|
| `GET /health` | Health check da API |
| `GET /swagger-ui` | Swagger UI (documentação interativa) |
| `GET /api-docs` | OpenAPI spec (JSON) |
