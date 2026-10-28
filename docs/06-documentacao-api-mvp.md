# 📄 Documentação da API - MVP (Nexo)

> **Projeto:** Nexo - Plataforma de Gerenciamento de Chamados (Rede Lúmen Diagnósticos)  
> **Referência:** Histórias de Usuário HU01 a HU04, HU09, HU10 e HU11

Esta documentação descreve os endpoints da API RESTful projetada para atender aos fluxos do MVP. O design foi concebido com base nas regras de negócio hospitalares da Rede Lúmen: chamados não devem transitar dados clínicos sensíveis (LGPD / RN01), devem estar associados a uma unidade válida e a auditoria do histórico é estritamente imutável.

**Base URL:** `/api/v1`

---

## Padrão de Resposta (`ApiResponse`)

Todas as requisições da API retornam um contrato padronizado (envelope JSON em `snake_case`) para garantir consistência no consumo pelo front-end e integrações:

```json
{
  "status_code": 200,
  "message": "Mensagem descritiva da operação",
  "data": { ... },
  "errors": null
}
```

---

## 1. Autenticação e Sessão (HU09)

A API utiliza arquitetura **Stateless** (`SessionCreationPolicy.STATELESS`) com **Bearer Token JWT** assinado via `HMAC-SHA256` e validade padrão de 1 hora (`RNF-007`).

### 1.1. Autenticar no Sistema (Login)

Autentica um analista de suporte ou liderança de TI com e-mail institucional e senha (validada via `BCrypt`), devolvendo o token JWT e os dados do usuário autenticado.

* **Endpoint:** `POST /auth/login` (também responde em `/api/v1/auth/login`)
* **Autenticação:** Nenhuma (Pública)

#### Request Body (`application/json`)

```json
{
  "email": "analista1@lumen.local",
  "senha": "nexo-dev-2026"
}
```

#### Response: `200 OK`

```json
{
  "status_code": 200,
  "message": "Autenticação realizada com sucesso",
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "usuario": {
      "id": "00000000-0000-0000-0000-0000000000a1",
      "nome": "Analista Dev Um",
      "email": "analista1@lumen.local",
      "perfil": "ANALISTA_SUPORTE"
    }
  },
  "errors": null
}
```

#### Erros

* `400 Bad Request`: e-mail ausente/malformado ou senha em branco.
* `401 Unauthorized`: credenciais inválidas ou usuário inativo (`ativo = false`). Retorna sempre mensagem genérica contra enumeração de contas (`RNF-010`):

```json
{
  "status_code": 401,
  "message": "E-mail ou senha inválidos.",
  "data": null,
  "errors": null
}
```

### 1.2. Encerrar Sessão (Logout)

Encerra o contexto de segurança da requisição atual e confirma o encerramento da sessão para que o cliente descarte o token JWT armazenado localmente.

* **Endpoint:** `POST /auth/logout` (também responde em `/api/v1/auth/logout`)
* **Autenticação:** Pública / Bearer Token

#### Response: `200 OK`

```json
{
  "status_code": 200,
  "message": "Logout realizado com sucesso",
  "data": null,
  "errors": null
}
```

---

## 2. Gestão de Unidades Diagnósticas (HU11)

Mantém o cadastro das unidades laboratoriais, hospitalares e administrativas da Rede Lúmen. Unidades desativadas (`ativo = false`) preservam seu histórico em chamados antigos, mas não podem receber novos chamados.

### 2.1. Listar Unidades

Lista todas as unidades cadastradas em ordem alfabética de nome, permitindo filtrar opcionalmente pela situação (`ativo`).

* **Endpoint:** `GET /unidades` (ex.: `GET /unidades?ativo=true`)
* **Autenticação:** Bearer Token

#### Query Parameters

| Parâmetro | Tipo | Obrigatório | Descrição |
| :--- | :--- | :---: | :--- |
| `ativo` | `boolean` | Não | Quando informado (`true` ou `false`), filtra apenas unidades ativas ou inativas. |

#### Response: `200 OK`

```json
{
  "status_code": 200,
  "message": "Unidades recuperadas com sucesso",
  "data": [
    {
      "id": "a1f0c3e2-0001-4000-8000-000000000001",
      "nome": "U.03 Boa Viagem",
      "identificacao": "LUM-BVG",
      "localizacao": "Rua Boa Viagem, 123",
      "atendimento_24h": false,
      "ativo": true
    }
  ],
  "errors": null
}
```

### 2.2. Buscar Unidade por ID

Recupera os dados cadastrais de uma unidade específica.

* **Endpoint:** `GET /unidades/{id}`
* **Autenticação:** Bearer Token
* **Respostas:** `200 OK` com `UnidadeResponse` ou `404 Not Found` (`"Unidade não encontrada."`).

### 2.3. Cadastrar Unidade

Registra uma nova unidade ativa na plataforma. O código de `identificacao` é normalizado para letras maiúsculas e deve ser único.

* **Endpoint:** `POST /unidades`
* **Autenticação:** Bearer Token (Liderança de TI)

#### Request Body (`application/json`)

```json
{
  "nome": "U.03 Boa Viagem",
  "identificacao": "LUM-BVG",
  "localizacao": "Rua Boa Viagem, 123",
  "atendimento_24h": false
}
```

#### Response: `201 Created`

```json
{
  "status_code": 201,
  "message": "Unidade cadastrada com sucesso",
  "data": {
    "id": "a1f0c3e2-0001-4000-8000-000000000001",
    "nome": "U.03 Boa Viagem",
    "identificacao": "LUM-BVG",
    "localizacao": "Rua Boa Viagem, 123",
    "atendimento_24h": false,
    "ativo": true
  },
  "errors": null
}
```

#### Erros

* `400 Bad Request`: campos obrigatórios ausentes (`nome`, `identificacao`), formato inválido na identificação (aceita apenas letras, números e hífen, máx. 20 caracteres) ou identificação duplicada (`"Já existe uma unidade com a identificação 'LUM-BVG'."`).

### 2.4. Editar Unidade

Atualiza os dados cadastrais de uma unidade existente.

* **Endpoint:** `PUT /unidades/{id}`
* **Autenticação:** Bearer Token (Liderança de TI)
* **Request Body:** mesmo formato do `POST /unidades`
* **Respostas:** `200 OK` (`"Unidade atualizada com sucesso"`), `400 Bad Request` ou `404 Not Found`.

### 2.5. Desativar Unidade (Exclusão Lógica)

Desativa logicamente uma unidade (`ativo = false`), impedindo sua seleção na abertura de novos chamados sem afetar chamados históricos.

* **Endpoint:** `PATCH /unidades/{id}/desativar`
* **Autenticação:** Bearer Token (Liderança de TI)
* **Respostas:** `200 OK` (`"Unidade desativada com sucesso"`) ou `404 Not Found`.

---

## 3. Catálogo do Formulário de Chamados (HU01)

Fornece as listas de opções ativas para classificação técnica no formulário de abertura de chamado, ordenadas alfabeticamente por `nome`.

### 3.1. Listar Categorias Ativas

* **Endpoint:** `GET /categorias`
* **Autenticação:** Bearer Token

#### Response: `200 OK`

```json
{
  "status_code": 200,
  "message": "Categorias recuperadas com sucesso",
  "data": [
    { "id": "c4e0a000-0001-4000-8000-000000000001", "nome": "Acesso e senhas" },
    { "id": "c4e0a000-0002-4000-8000-000000000002", "nome": "Equipamento de apoio" }
  ],
  "errors": null
}
```

### 3.2. Listar Sistemas e Equipamentos Ativos

* **Endpoint:** `GET /sistemas-equipamentos`
* **Autenticação:** Bearer Token

#### Response: `200 OK`

```json
{
  "status_code": 200,
  "message": "Sistemas/equipamentos recuperados com sucesso",
  "data": [
    { "id": "5e5a0000-0001-4000-8000-000000000001", "nome": "Computador da recepção" },
    { "id": "5e5a0000-0003-4000-8000-000000000003", "nome": "Impressora de etiquetas" }
  ],
  "errors": null
}
```

---

## 4. Registrar um Chamado (HU01)

Cria um novo chamado estruturado a partir de uma solicitação recebida por um dos canais de entrada (`LIGACAO`, `PRESENCIAL`, `WHATSAPP`, `EMAIL`).

* **Endpoint:** `POST /chamados`
* **Autenticação:** Bearer Token (Analista de Suporte ou Liderança de TI)

### Request Body (`application/json`)

```json
{
  "unidade_id": "a1f0c3e2-0001-4000-8000-000000000001",
  "canal_origem": "LIGACAO",
  "solicitante": {
    "nome": "Mariana Alves",
    "contato": "+5581988771234"
  },
  "descricao": "Impressora de etiquetas da coleta parou de imprimir. Há pacientes aguardando na recepção.",
  "categoria_id": "c4e0a000-0002-4000-8000-000000000002",
  "sistema_equipamento_id": "5e5a0000-0003-4000-8000-000000000003",
  "registro_iniciado_em": "2026-10-07T14:29:15Z"
}
```

**Regras:**

* Obrigatórios: `unidade_id`, `canal_origem`, `solicitante.contato`, `descricao` (10 a 2000 caracteres) e `categoria_id`.
* `solicitante.contato`: com `@` é tratado como e-mail e gravado em minúsculas; sem `@` é telefone (10 a 13 dígitos) e convertido para E.164 (`+55` acrescentado quando ausente). Um contato já conhecido reaproveita o mesmo solicitante (`RN13`).
* Unidade, categoria e sistema/equipamento precisam existir (`404 Not Found` caso contrário) e estar ativos (`400 Bad Request` se inativos).
* `registro_iniciado_em` no futuro ou de mais de 2 h antes da abertura é descartado silenciosamente (gravado como `NULL`), sem recusar o chamado.
* O registro grava, numa única transação, o chamado, o registro de contato inicial e os eventos `CHAMADO_ABERTO` e `REGISTRO_DE_CONTATO_RECEBIDO` tendo como `autor_id` o usuário autenticado no token JWT.

> **Aviso de Negócio (RN01 / LGPD):** O frontend exibe um alerta fixo proibindo o envio de dados clínicos de pacientes ou resultados de exames no campo `descricao`.

### Response: `201 Created`

```json
{
  "status_code": 201,
  "message": "Chamado registrado com sucesso",
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "codigo": "LUM-2026-000123",
    "status": "ABERTO",
    "data_criacao": "2026-10-07T14:30:00Z",
    "origem_dado": "TESTE"
  },
  "errors": null
}
```

---

## 5. Consultar o Chamado e Histórico (HU02 — Planejado Sprint 2)

Recupera os detalhes completos do chamado, incluindo a linha do tempo cronológica com todos os eventos e registros de contato.

* **Endpoint:** `GET /chamados/{id}`
* **Autenticação:** Bearer Token (Analista)

### Response: `200 OK`

```json
{
  "status_code": 200,
  "message": "Detalhes do chamado recuperados",
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "codigo": "LUM-2026-000123",
    "status": "ABERTO",
    "unidade": {
      "id": "a1f0c3e2-0001-4000-8000-000000000001",
      "nome": "U.03 Boa Viagem"
    },
    "responsavel": null,
    "prioridade": null,
    "historico": [
      {
        "id_evento": "uuid-evento-1",
        "tipo": "CHAMADO_ABERTO",
        "data": "2026-10-07T14:30:00Z",
        "autor": "Analista Dev Um",
        "detalhes": {
          "canal": "LIGACAO"
        }
      }
    ]
  },
  "errors": null
}
```

---

## 6. Evolução do Ciclo de Atendimento (HU03, HU04, HU10 — Planejados)

* **`POST /chamados/{id}/atualizacoes`** (`HU02`/`HU13`): inclui novos registros de contato ou anotações no histórico do chamado.
* **`POST /chamados/{id}/vinculos`** (`HU03`): associa um registro de contato duplicado a um chamado principal em andamento.
* **`PATCH /chamados/{id}/prioridade`** (`HU04`): registra a triagem estruturada pela Matriz 3D (`severidade`, `urgencia`, `impacto_operacional`).
* **`POST /analistas`** (`HU10`): cadastro administrativo de analistas de suporte restrito à Liderança de TI.
