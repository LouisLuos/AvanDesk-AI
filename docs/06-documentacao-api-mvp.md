# 📄 Documentação da API - MVP (Nexo)

> **Projeto:** Nexo - Plataforma de Gerenciamento de Chamados (Rede Lúmen Diagnósticos)  
> **Referência:** Histórias de Usuário HU01 a HU07, HU09 a HU14

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
* `atribuir_a_mim` (opcional, padrão `false`): com `true`, quem registra assume o chamado na mesma operação (HU12, ver seção 8).

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

## 5. Consultar Detalhe, Status, Responsável e Histórico do Chamado (HU02 & HU05)

Recupera os detalhes completos do chamado, incluindo status atual (dentre os 9 estados do domínio), motivo de espera (quando em `AGUARDANDO`), responsável atual (`null` quando não atribuído), a última resolução registrada (HU06), registros de contato, escalonamentos para fornecedor (HU07) e linha do tempo cronológica imutável (`eventos_historico`) em UTC.

* **Resolução (HU06):** `resolucao`, `causa_raiz` e `possivel_recorrencia` trazem a última resolução registrada e continuam preenchidos se o chamado for reaberto; `resolvido_em` volta a `null` na reabertura. Cada resolução fica também no evento `CHAMADO_RESOLVIDO` do histórico.
* **Escalonamentos (HU07):** lista em ordem de envio, vazia quando o chamado nunca foi escalado. Cada item traz `id`, `fornecedor`, `protocolo_externo`, `observacao`, `enviado_em`, `enviado_por` (nome), `responsavel` (`{id, nome}`: o responsável interno no momento do envio), `retorno`, `retorno_em`, `retorno_registrado_por` (nome) e `aberto` (`true` enquanto o fornecedor não respondeu). Exemplo na seção 12.

* **Endpoint:** `GET /chamados/{id}`
* **Autenticação:** Bearer Token (Analista de Suporte ou Liderança de TI)

### Response: `200 OK`

```json
{
  "status_code": 200,
  "message": "Detalhes do chamado recuperados",
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "codigo": "LUM-2026-000123",
    "status": "ABERTO",
    "motivo_espera": null,
    "descricao": "Impressora de etiquetas da coleta parou de imprimir. Há pacientes aguardando na recepção.",
    "canal_origem": "LIGACAO",
    "unidade": {
      "id": "a1f0c3e2-0001-4000-8000-000000000001",
      "nome": "U.03 Boa Viagem"
    },
    "categoria": {
      "id": "c4e0a000-0002-4000-8000-000000000002",
      "nome": "Equipamento de apoio"
    },
    "sistema_equipamento": {
      "id": "5e5a0000-0003-4000-8000-000000000003",
      "nome": "Impressora de etiquetas"
    },
    "solicitante": {
      "nome": "Mariana Alves",
      "contato": "+5581988771234"
    },
    "responsavel": null,
    "prioridade": null,
    "aberto_em": "2026-10-07T14:30:00Z",
    "primeira_resposta_em": null,
    "resolvido_em": null,
    "resolucao": null,
    "causa_raiz": null,
    "possivel_recorrencia": false,
    "origem_dado": "TESTE",
    "registros_contato": [
      {
        "id": "8f14e45f-ceea-467a-9a38-1b2c3d4e5f60",
        "canal": "LIGACAO",
        "direcao": "RECEBIDO",
        "conteudo": "Impressora de etiquetas da coleta parou de imprimir. Há pacientes aguardando na recepção.",
        "ocorrido_em": "2026-10-07T14:30:00Z",
        "solicitante": {
          "nome": "Mariana Alves",
          "contato": "+5581988771234"
        },
        "registrado_por": "Analista Dev Um"
      }
    ],
    "escalonamentos": [],
    "historico": [
      {
        "id_evento": "uuid-evento-1",
        "tipo": "CHAMADO_ABERTO",
        "data": "2026-10-07T14:30:00Z",
        "autor": "Analista Dev Um",
        "detalhes": {
          "canal": "LIGACAO"
        }
      },
      {
        "id_evento": "uuid-evento-2",
        "tipo": "REGISTRO_DE_CONTATO_RECEBIDO",
        "data": "2026-10-07T14:30:00Z",
        "autor": "Analista Dev Um",
        "detalhes": {
          "registro_contato_id": "8f14e45f-ceea-467a-9a38-1b2c3d4e5f60"
        }
      }
    ]
  },
  "errors": null
}
```

### Erros

* `404 Not Found`: quando o `id` informado não corresponde a nenhum chamado registrado (`"Chamado não encontrado."`).

---

## 6. Listar e Buscar Chamados na Fila (HU14)

Lista os chamados de forma paginada, **do mais recente para o mais antigo** (`aberto_em DESC`, com `id` para desempatar). A ordem é fixa: o cliente não escolhe o campo. Todos os filtros são opcionais e **se somam** (cada filtro informado restringe mais o resultado). A resposta também traz os totais por status para os cartões do topo da tela.

* **Endpoint:** `GET /chamados` (ex.: `GET /chamados?meus=true&status=EM_ATENDIMENTO`)
* **Autenticação:** Bearer Token (Analista de Suporte ou Liderança de TI)

### Query Parameters

| Parâmetro | Tipo | Padrão | Descrição |
| :--- | :--- | :---: | :--- |
| `page` | `integer` | `0` | Número da página, começando em 0. |
| `size` | `integer` | `20` | Itens por página, de 1 a 100. |
| `status` | `string` (repetível) | — | Um ou mais status. Para vários, repita: `status=ABERTO&status=EM_TRIAGEM`. |
| `unidade_id` | `uuid` | — | Chamados de uma unidade. |
| `prioridade` | `string` | — | Prioridade **final**: `ALTA`, `MEDIA` ou `BAIXA`. |
| `canal_origem` | `string` | — | `LIGACAO`, `PRESENCIAL`, `WHATSAPP` ou `EMAIL`. |
| `responsavel_id` | `uuid` | — | Chamados atribuídos a um analista. |
| `sem_responsavel` | `boolean` | `false` | Só chamados sem responsável (`responsavel_id IS NULL`), para triagem. |
| `sem_prioridade` | `boolean` | `false` | Só chamados ainda sem prioridade final (aguardando triagem). |
| `meus` | `boolean` | `false` | Só chamados do usuário autenticado. O servidor identifica o usuário pelo token; o cliente não envia o id. Junto com um `responsavel_id` de outra pessoa, o resultado é vazio. |
| `codigo` | `string` | — | Código exato (`LUM-AAAA-NNNNNN`), sem diferenciar maiúsculas. |

### Response: `200 OK`

```json
{
  "status_code": 200,
  "message": "Chamados recuperados com sucesso",
  "data": {
    "itens": [
      {
        "id": "7f1c2b9e-5d3a-4c1e-9b2f-0a8d6e4c3b21",
        "codigo": "LUM-2026-000162",
        "titulo": "Impressora de etiquetas da coleta parada",
        "unidade": { "id": "a1f0c3e2-0001-4000-8000-000000000001", "nome": "U.03 Boa Viagem" },
        "categoria": { "id": "c4e0a000-0002-4000-8000-000000000002", "nome": "Equipamento de apoio" },
        "canal_origem": "LIGACAO",
        "status": "ABERTO",
        "prioridade": null,
        "responsavel": null,
        "aberto_em": "2026-10-29T12:00:00Z",
        "atualizado_em": "2026-10-29T12:30:00Z"
      }
    ],
    "pagina": 0,
    "tamanho": 20,
    "total_itens": 31,
    "total_paginas": 2,
    "totais_por_status": {
      "ABERTO": 12, "EM_TRIAGEM": 5, "EM_ATENDIMENTO": 9, "AGUARDANDO": 4, "RESOLVIDO": 1,
      "REABERTO": 0, "FECHADO": 0, "CANCELADO": 0, "MESCLADO": 0
    }
  },
  "errors": null
}
```

* **`titulo`:** primeira linha da descrição, com até 80 caracteres (cortada com `…`).
* **`prioridade`:** prioridade final; `null` enquanto não houver triagem.
* **`responsavel`:** `null` quando o chamado não tem responsável ("Não atribuído").
* **`atualizado_em`:** data do último evento do histórico; sem eventos, a data de abertura.
* **`totais_por_status`:** contagem com os mesmos filtros, **menos o de status**, para os cartões continuarem mostrando todos os status. Os 9 status sempre aparecem, mesmo com zero.
* **SLA:** ainda não vem na resposta. O cálculo de prazo é de outra HU; a tela exibe "—".

### Erros

* `400 Bad Request`: `page` negativo, `size` fora de 1 a 100 (`"O parâmetro 'size' deve estar entre 1 e 100."`) ou valor inválido em um filtro (ex.: `status=XPTO`, `unidade_id` que não é UUID).
* `401 Unauthorized`: sem token ou com token inválido.

---

## 7. Assumir ou Reatribuir o Chamado (HU12)

Define o analista responsável pelo chamado. Para **assumir**, o analista manda o próprio id (o login devolve em `usuario.id`); para **reatribuir**, manda o id de outro analista.

* **Endpoint:** `PATCH /chamados/{id}/responsavel`
* **Autenticação:** Bearer Token (Analista de Suporte ou Liderança de TI)

### Request Body (`application/json`)

```json
{ "responsavel_id": "00000000-0000-0000-0000-0000000000a2" }
```

**Regras:**

* Só um usuário ativo com perfil `ANALISTA_SUPORTE` pode ser responsável (RF-016).
* Grava o evento `CHAMADO_ATRIBUIDO` com o autor do token. Os `detalhes` trazem `responsavel_id` e `responsavel_nome` e, na reatribuição, `responsavel_anterior_id` e `responsavel_anterior_nome`.
* A primeira ação sobre o chamado preenche `primeira_resposta_em` (M03); as seguintes não mudam o valor.
* Atribuir não muda o status.

### Response: `200 OK`

Mesmo formato do `GET /chamados/{id}` (seção 5), com `"message": "Responsável atribuído com sucesso"`, o `responsavel` novo e o evento no fim do `historico`.

### Erros

| Caso | Status | `errors` / `message` |
| :--- | :---: | :--- |
| Corpo sem `responsavel_id` | `400` | "Campo 'responsavel_id' é obrigatório." |
| Chamado ou usuário inexistente | `404` | "Chamado não encontrado." / "Usuário não encontrado." |
| Usuário de outro perfil | `422` | "Só analistas de suporte podem ser responsáveis por chamados." |
| Analista inativo | `422` | "Analista inativo não pode receber chamados." |
| Mesmo responsável | `422` | "O chamado já está com este responsável." |
| Chamado `FECHADO`, `CANCELADO` ou `MESCLADO` | `422` | "Não é possível atribuir um chamado com status CANCELADO." |
| Sem token | `401` | — |

O `422` usa `"message": "Operação não permitida"` e o motivo em `errors`.

---

## 8. Alterar o Status do Chamado (HU12 e HU06)

Move o chamado pelo ciclo de vida (diagrama v3 da seção 6 da análise de domínio). Transições fora do ciclo são recusadas com mensagem explicativa. A reabertura e a retomada vieram com a HU06.

* **Endpoint:** `PATCH /chamados/{id}/status`
* **Autenticação:** Bearer Token (Analista de Suporte ou Liderança de TI)

### Request Body (`application/json`)

```json
{ "status": "AGUARDANDO", "motivo_espera": "SOLICITANTE" }
```

```json
{ "status": "CANCELADO", "motivo_cancelamento": "Aberto por engano." }
```

```json
{ "status": "REABERTO", "motivo_reabertura": "Solicitante ligou: a impressora voltou a falhar." }
```

### Transições aceitas

| De | Para | Exige | Evento |
| :--- | :--- | :--- | :--- |
| `ABERTO` | `EM_TRIAGEM` | — | `TRIAGEM_INICIADA` |
| `EM_TRIAGEM` | `EM_ATENDIMENTO` | responsável definido | `CHAMADO_TRIADO` |
| `EM_ATENDIMENTO` | `AGUARDANDO` | `motivo_espera` (`SOLICITANTE` ou `PECA_EQUIPAMENTO`) | `CHAMADO_AGUARDANDO` |
| `AGUARDANDO` | `EM_ATENDIMENTO` | motivo diferente de `FORNECEDOR` | `RETORNO_RECEBIDO` |
| `ABERTO`, `EM_TRIAGEM` | `CANCELADO` | `motivo_cancelamento` (até 500 caracteres) | `CHAMADO_CANCELADO` |
| `RESOLVIDO` | `REABERTO` | `motivo_reabertura` (até 500 caracteres), até 7 dias depois de `resolvido_em` | `CHAMADO_REABERTO` |
| `REABERTO` | `EM_ATENDIMENTO` | responsável definido | `ATENDIMENTO_RETOMADO` |

**Regras:**

* Todo evento traz `status_anterior` nos `detalhes`; `CHAMADO_AGUARDANDO` traz também `motivo_espera`, `CHAMADO_CANCELADO` traz `motivo_cancelamento`, e `CHAMADO_REABERTO`, `motivo_reabertura`.
* Ao sair de `AGUARDANDO`, `motivo_espera` volta a `null`.
* **Espera por fornecedor (HU07):** não passa por aqui. O chamado entra em `AGUARDANDO` com motivo `FORNECEDOR` só pelo escalonamento e sai só pelo registro do retorno (seção 12). Assim todo chamado que aguarda fornecedor tem um escalonamento aberto, com fornecedor e data de envio, e o tempo em terceiros (M05) não perde nenhum período. **Mudou na HU07:** antes este endpoint aceitava `FORNECEDOR`.
* **Reabertura (RN20):** no MVP quem reabre é o analista, quando o solicitante avisa que o problema voltou. Depois de 7 dias da resolução, um novo relato vira um novo chamado. Ao reabrir, `resolvido_em` volta a `null`; a resolução anterior continua no chamado e no histórico. O responsável não muda.
* `RESOLVIDO`, `FECHADO` e `MESCLADO` têm operação própria (seção 9, HU15 e HU03) e não são definidos por aqui.
* Campos que não servem para a transição pedida são ignorados.

### Response: `200 OK`

Mesmo formato do `GET /chamados/{id}` (seção 5), com `"message": "Status do chamado atualizado com sucesso"`.

### Erros

| Caso | Status | `errors` / `message` |
| :--- | :---: | :--- |
| Sem `status`, valor inexistente ou motivo com mais de 500 caracteres | `400` | — |
| Chamado inexistente | `404` | "Chamado não encontrado." |
| Fora do ciclo de vida | `422` | "Não é possível ir de ABERTO para EM_ATENDIMENTO." |
| Mesmo status | `422` | "O chamado já está com status EM_ATENDIMENTO." |
| Chamado encerrado | `422` | "Não é possível alterar o status de um chamado CANCELADO." |
| Status com operação própria | `422` | "O status RESOLVIDO tem operação própria e não pode ser definido por aqui." |
| `EM_ATENDIMENTO` sem responsável | `422` | "Atribua um responsável antes de iniciar o atendimento." |
| `AGUARDANDO` sem motivo | `422` | "Informe o motivo_espera para colocar o chamado em AGUARDANDO." |
| `AGUARDANDO` com motivo `FORNECEDOR` | `422` | "Para aguardar fornecedor, escale o chamado para o fornecedor." |
| Sair da espera por fornecedor | `422` | "Para voltar ao atendimento, registre o retorno do fornecedor." |
| `CANCELADO` sem motivo | `422` | "Informe o motivo_cancelamento para cancelar o chamado." |
| `REABERTO` sem motivo | `422` | "Informe o motivo_reabertura para reabrir o chamado." |
| Reabertura depois de 7 dias | `422` | "O prazo de 7 dias para reabrir terminou. Registre um novo chamado." |
| Retomada sem responsável | `422` | "Atribua um responsável antes de retomar o atendimento." |
| Sem token | `401` | — |

> **Registro com "atribuir a mim":** o `POST /chamados` (seção 4) aceita o campo opcional `"atribuir_a_mim": true`. Quem registra vira o responsável e o chamado passa por `ABERTO`, `EM_TRIAGEM` e `EM_ATENDIMENTO` na mesma operação, com os eventos `TRIAGEM_INICIADA`, `CHAMADO_ATRIBUIDO` e `CHAMADO_TRIADO`. Se quem registra não é analista ativo, responde `422`.

---

## 9. Registrar a Resolução do Chamado (HU06)

Registra como o chamado foi resolvido e, quando identificada, a causa raiz (RF-012). O chamado vai para `RESOLVIDO`.

* **Endpoint:** `PATCH /chamados/{id}/resolucao`
* **Autenticação:** Bearer Token (Analista de Suporte ou Liderança de TI)

### Request Body (`application/json`)

```json
{
  "resolucao": "Cabo de rede da impressora estava solto no rack; recolocado e impressão testada com a recepção.",
  "causa_raiz": "Cabo sem trava no patch panel da coleta.",
  "possivel_recorrencia": true
}
```

| Campo | Obrigatório | Regra |
| :--- | :---: | :--- |
| `resolucao` | sim | 10 a 2000 caracteres (`ck_chamados_resolucao`) |
| `causa_raiz` | não | até 1000 caracteres; em branco vira `null` |
| `possivel_recorrencia` | não | padrão `false`; marca causa que pode se repetir (RN09), independente da causa raiz |

**Regras:**

* Só a partir de `EM_ATENDIMENTO` ou `AGUARDANDO`. Vindo de `AGUARDANDO`, `motivo_espera` volta a `null`.
* Se o chamado aguarda fornecedor, primeiro registre o retorno (seção 12), depois a resolução (HU07).
* Grava `resolvido_em` (UTC) e o evento `CHAMADO_RESOLVIDO` com o autor do token e os `detalhes` `status_anterior`, `resolucao`, `causa_raiz` (quando houver) e `possivel_recorrencia`. O texto vai no evento porque uma nova resolução, depois de reabrir, sobrescreve os campos do chamado.
* A causa raiz é registrada junto com a resolução. Relacionar chamados à mesma causa (RF-014) não faz parte do MVP.
* Para reabrir, use o `PATCH /chamados/{id}/status` (seção 8).

### Response: `200 OK`

Mesmo formato do `GET /chamados/{id}` (seção 5), com `"message": "Chamado resolvido com sucesso"`, `status` `RESOLVIDO` e os campos da resolução preenchidos.

### Erros

| Caso | Status | `errors` / `message` |
| :--- | :---: | :--- |
| Sem `resolucao` | `400` | "Campo 'resolucao' é obrigatório." |
| `resolucao` fora de 10 a 2000 caracteres | `400` | "Campo 'resolucao' deve ter entre 10 e 2000 caracteres." |
| `causa_raiz` com mais de 1000 caracteres | `400` | "Campo 'causa_raiz' deve ter no máximo 1000 caracteres." |
| Chamado inexistente | `404` | "Chamado não encontrado." |
| Status que não vai para `RESOLVIDO` | `422` | "Não é possível ir de ABERTO para RESOLVIDO." |
| Já resolvido | `422` | "O chamado já está com status RESOLVIDO." |
| Chamado aguardando o retorno do fornecedor | `422` | "Registre o retorno do fornecedor antes de resolver o chamado." |
| Chamado encerrado | `422` | "Não é possível alterar o status de um chamado CANCELADO." |
| Sem token | `401` | — |

---

## 10. Registrar Novo Contato em Chamado Existente (HU13)

Inclui uma nova ligação, mensagem, e-mail ou visita num chamado que já existe, para que as interações fiquem concentradas no mesmo atendimento (RF-006).

* **Endpoint:** `POST /chamados/{id}/contatos`
* **Autenticação:** Bearer Token (Analista de Suporte ou Liderança de TI)

### Request Body (`application/json`)

```json
{
  "canal": "WHATSAPP",
  "direcao": "RECEBIDO",
  "conteudo": "Gestora da unidade avisou que a impressora voltou a falhar.",
  "ocorrido_em": "2026-11-03T13:10:00Z",
  "solicitante": { "nome": "Paula Lima", "contato": "+5581987650000" },
  "identificador_externo": "wamid.HBgM..."
}
```

Mínimo:

```json
{ "canal": "LIGACAO", "conteudo": "Solicitante ligou cobrando retorno." }
```

| Campo | Obrigatório | Regra |
| :--- | :---: | :--- |
| `canal` | sim | `LIGACAO`, `PRESENCIAL`, `WHATSAPP` ou `EMAIL` |
| `conteudo` | sim | até 2000 caracteres |
| `direcao` | não | `RECEBIDO` (padrão) ou `ENVIADO` |
| `ocorrido_em` | não | padrão: agora; não pode estar no futuro (1 min de tolerância) nem antes do minuto da abertura |
| `solicitante` | não | `{nome?, contato}`, mesma regra do registro (RN13); padrão: o solicitante do chamado |
| `identificador_externo` | não | até 255 caracteres; único por canal (RN16) |

**Regras:**

* O `solicitante` informado não troca o solicitante do chamado.
* Aceita contato em qualquer status ativo, inclusive `RESOLVIDO`, e não muda o status.
* Grava o evento `REGISTRO_DE_CONTATO_RECEBIDO` (também para `ENVIADO`) com o autor do token e os `detalhes` `registro_contato_id`, `canal` e `direcao`.
* Um contato `ENVIADO` preenche `primeira_resposta_em` se ainda estiver vazio (M03).

### Response: `201 Created`

Mesmo formato do `GET /chamados/{id}` (seção 5), com `"message": "Contato registrado com sucesso"`, o contato novo em `registros_contato` e o evento no fim do `historico`.

### Erros

| Caso | Status | `errors` / `message` |
| :--- | :---: | :--- |
| Sem `canal` ou `conteudo` | `400` | "Campo 'canal' é obrigatório." / "Campo 'conteudo' é obrigatório." |
| `canal` ou `direcao` inexistente | `400` | "Requisição inválida" |
| Contato do solicitante inválido | `400` | mesma mensagem do registro (seção 4) |
| `ocorrido_em` no futuro | `400` | "O campo 'ocorrido_em' não pode estar no futuro." |
| `ocorrido_em` antes da abertura | `400` | "O campo 'ocorrido_em' não pode ser anterior à abertura do chamado." |
| Identificador externo repetido no canal | `400` | "Este contato já foi registrado (identificador externo repetido no canal WHATSAPP)." |
| Chamado inexistente | `404` | "Chamado não encontrado." |
| Chamado `FECHADO` ou `CANCELADO` | `422` | "Não é possível registrar contato em um chamado CANCELADO." |
| Chamado `MESCLADO` | `422` | "Este chamado foi mesclado ao LUM-2026-000100. Registre o contato no chamado principal." |
| Sem token | `401` | — |

---

## 11. Gerenciar Analistas (HU10)

Cadastro, edição, desativação e reativação dos analistas de suporte (RF-023). A listagem atende qualquer usuário autenticado, porque alimenta a atribuição de chamados; as alterações são só da Liderança de TI (`@PreAuthorize("hasRole('LIDERANCA_TI')")`).

| Endpoint | Quem pode | Resposta |
| :--- | :--- | :--- |
| `GET /analistas?ativo=true\|false` | qualquer usuário autenticado | `200` com a lista em ordem alfabética |
| `POST /analistas` | Liderança de TI | `201` com `analista` e `senha_temporaria` |
| `PUT /analistas/{id}` | Liderança de TI | `200` com o analista |
| `PATCH /analistas/{id}/desativar` | Liderança de TI | `200` com o analista (`ativo: false`) |
| `PATCH /analistas/{id}/reativar` | Liderança de TI | `200` com o analista (`ativo: true`) |

### Request Body de cadastro e edição (`application/json`)

```json
{ "nome": "Ana Souza", "email": "ana.souza@lumen.local" }
```

### Response do cadastro: `201 Created`

```json
{
  "status_code": 201,
  "message": "Analista cadastrado com sucesso",
  "data": {
    "analista": {
      "id": "6b1f0f8e-3c1a-4f7e-9a51-0d2c8f4b7a10",
      "nome": "Ana Souza",
      "email": "ana.souza@lumen.local",
      "ativo": true,
      "criado_em": "2026-11-03T23:40:12.345Z"
    },
    "senha_temporaria": "Kp7m-Qx4t-Rz9w"
  },
  "errors": null
}
```

**Regras:**

* O perfil é sempre `ANALISTA_SUPORTE`; contas de liderança não são criadas por aqui.
* O e-mail é gravado em minúsculas, precisa ser de um domínio da propriedade `nexo.usuarios.dominios-email` (dev: `lumen.local`; produção: variável `NEXO_DOMINIOS_EMAIL`, separada por vírgula) e não pode repetir entre usuários.
* A senha temporária é gerada pelo servidor (12 caracteres sem ambíguos) e só aparece na resposta do cadastro; o banco guarda apenas o hash BCrypt.
* A desativação é lógica: o analista continua como autor no histórico, deixa de aparecer em `GET /analistas?ativo=true` e não pode receber chamados (`PATCH /chamados/{id}/responsavel` responde `422`).
* A desativação vale na hora: o filtro JWT busca o usuário a cada requisição e só autentica quem está ativo, então um token ainda válido passa a receber `401`. E-mail e perfil também vêm do banco, não do token.

### Erros

| Caso | Status | `errors` / `message` |
| :--- | :---: | :--- |
| Sem `nome` ou `email` | `400` | "Campo 'nome' é obrigatório." / "Campo 'email' é obrigatório." |
| E-mail fora do domínio corporativo | `400` | "Use um e-mail corporativo (@lumen.local)." |
| Perfil diferente de Liderança de TI | `403` | "Acesso negado para o perfil do usuário." |
| Analista inexistente (ou o id não é de um analista) | `404` | "Analista não encontrado." |
| E-mail já usado por outro usuário | `409` | "Já existe um usuário com o e-mail 'ana.souza@lumen.local'." |
| Sem token ou usuário desativado | `401` | — |

### Chamados em andamento por responsável

* **Endpoint:** `GET /chamados/totais-por-responsavel` (qualquer usuário autenticado)
* **Response:** `200` com `[{ "responsavel_id": "…", "total": 6 }]`. Conta tudo o que não está `RESOLVIDO`, `FECHADO`, `CANCELADO` ou `MESCLADO`; responsáveis sem chamados não aparecem. A tela de analistas cruza com `GET /analistas` para a coluna "Chamados ativos".

---

## 12. Escalar Chamado para Fornecedor Externo (HU07)

Encaminha o chamado a um fornecedor externo mantendo o analista interno como responsável (RF-017, RN07) e registra o retorno do fornecedor, que encerra o escalonamento (RF-018). Um chamado pode ser escalado mais de uma vez, mas tem no máximo um escalonamento aberto por vez.

| Endpoint | Resposta |
| :--- | :--- |
| `POST /chamados/{id}/escalonamentos` | `201` com o detalhe do chamado (seção 5) |
| `PATCH /chamados/{id}/escalonamentos/{escalonamentoId}/retorno` | `200` com o detalhe do chamado (seção 5) |

* **Autenticação:** Bearer Token (Analista de Suporte ou Liderança de TI)

### 12.1. Escalar para o fornecedor

* **Endpoint:** `POST /chamados/{id}/escalonamentos`

#### Request Body (`application/json`)

```json
{
  "fornecedor": "Philips Healthcare",
  "protocolo_externo": "PHC-2026-88123",
  "observacao": "Enviado o log de erro da impressora de etiquetas.",
  "enviado_em": "2026-11-04T14:00:00Z"
}
```

Mínimo:

```json
{ "fornecedor": "Philips Healthcare" }
```

| Campo | Obrigatório | Regra |
| :--- | :---: | :--- |
| `fornecedor` | sim | até 120 caracteres; texto livre (cadastro de fornecedores é decisão em aberto, seção 8 do backlog) |
| `protocolo_externo` | não | até 100 caracteres; número do atendimento no fornecedor |
| `observacao` | não | até 1000 caracteres; o que foi pedido ao fornecedor |
| `enviado_em` | não | padrão: agora; não pode estar no futuro (1 min de tolerância, gravado como agora), antes do minuto da abertura nem antes do retorno do escalonamento anterior |

**Regras:**

* Só a partir de `EM_ATENDIMENTO`. O chamado vai para `AGUARDANDO` com `motivo_espera` `FORNECEDOR`, e a espera começa em `enviado_em`. O responsável não muda (RN07).
* O escalonamento guarda o responsável interno do momento do envio (`responsavel`) e quem enviou (`enviado_por`). Se o chamado for reatribuído depois, o escalonamento mantém o nome da época.
* Grava o evento `CHAMADO_ESCALADO` com o autor do token e os `detalhes` `status_anterior`, `escalonamento_id`, `fornecedor` e `protocolo_externo` (quando houver).
* A espera por fornecedor não pausa o relógio do SLA (RN07): o período entre envio e retorno conta como tempo em terceiros (M05).

#### Response: `201 Created`

Mesmo formato do `GET /chamados/{id}` (seção 5), com `"message": "Chamado escalado para o fornecedor"`, `status` `AGUARDANDO`, `motivo_espera` `FORNECEDOR`, o evento no fim do `historico` e o escalonamento aberto no fim de `escalonamentos`:

```json
{
  "escalonamentos": [
    {
      "id": "9e148216-f4d8-4487-b918-41d091a19a7b",
      "fornecedor": "Philips Healthcare",
      "protocolo_externo": "PHC-2026-88123",
      "observacao": "Enviado o log de erro da impressora de etiquetas.",
      "enviado_em": "2026-11-04T14:00:00Z",
      "enviado_por": "Analista Dev Um",
      "responsavel": { "id": "00000000-0000-0000-0000-0000000000a1", "nome": "Analista Dev Um" },
      "retorno": null,
      "retorno_em": null,
      "retorno_registrado_por": null,
      "aberto": true
    }
  ]
}
```

#### Erros

| Caso | Status | `errors` / `message` |
| :--- | :---: | :--- |
| Sem `fornecedor` | `400` | "Campo 'fornecedor' é obrigatório." |
| Campo acima do tamanho | `400` | "Campo 'protocolo_externo' deve ter no máximo 100 caracteres." |
| `enviado_em` no futuro | `400` | "O campo 'enviado_em' não pode estar no futuro." |
| `enviado_em` antes da abertura | `400` | "O campo 'enviado_em' não pode ser anterior à abertura do chamado." |
| `enviado_em` antes do retorno anterior | `400` | "O campo 'enviado_em' não pode ser anterior ao retorno do escalonamento anterior." |
| Chamado inexistente | `404` | "Chamado não encontrado." |
| Fora de `EM_ATENDIMENTO` | `422` | "Só é possível escalar um chamado EM_ATENDIMENTO (status atual: ABERTO)." |
| Chamado sem responsável | `422` | "Atribua um responsável antes de escalar o chamado." |
| Escalonamento anterior ainda sem retorno | `422` | "O chamado já tem um escalonamento sem retorno. Registre o retorno antes de escalar de novo." |
| Sem token | `401` | — |

### 12.2. Registrar o retorno do fornecedor

* **Endpoint:** `PATCH /chamados/{id}/escalonamentos/{escalonamentoId}/retorno`
* `escalonamentoId` é o `id` do item em `escalonamentos` no detalhe do chamado.

#### Request Body (`application/json`)

```json
{ "retorno": "Fornecedor trocou a placa e liberou o equipamento.", "retorno_em": "2026-11-05T10:30:00Z" }
```

| Campo | Obrigatório | Regra |
| :--- | :---: | :--- |
| `retorno` | sim | até 2000 caracteres; o que o fornecedor respondeu ou fez |
| `retorno_em` | não | padrão: agora; não pode estar no futuro (1 min de tolerância, gravado como agora) nem antes de `enviado_em` |

**Regras:**

* Fecha o escalonamento (`retorno`, `retorno_em`, `retorno_registrado_por` preenchidos e `aberto: false`) e devolve o chamado para `EM_ATENDIMENTO`, com `motivo_espera` em `null`.
* Grava o evento `RETORNO_RECEBIDO` com o autor do token e os `detalhes` `status_anterior`, `escalonamento_id` e `fornecedor`. É o mesmo tipo de evento da volta das outras esperas (seção 8); o `escalonamento_id` diferencia.
* Depois do retorno, o chamado pode ser escalado de novo ou resolvido (seção 9).

#### Response: `200 OK`

Mesmo formato do `GET /chamados/{id}` (seção 5), com `"message": "Retorno do fornecedor registrado"`, `status` `EM_ATENDIMENTO` e o escalonamento fechado.

#### Erros

| Caso | Status | `errors` / `message` |
| :--- | :---: | :--- |
| Sem `retorno` | `400` | "Campo 'retorno' é obrigatório." |
| `retorno` com mais de 2000 caracteres | `400` | "Campo 'retorno' deve ter no máximo 2000 caracteres." |
| `retorno_em` no futuro | `400` | "O campo 'retorno_em' não pode estar no futuro." |
| `retorno_em` antes do envio | `400` | "O campo 'retorno_em' não pode ser anterior ao envio ao fornecedor." |
| Chamado inexistente | `404` | "Chamado não encontrado." |
| Escalonamento inexistente ou de outro chamado | `404` | "Escalonamento não encontrado neste chamado." |
| Retorno já registrado | `422` | "O retorno deste escalonamento já foi registrado." |
| Chamado que não aguarda fornecedor | `422` | "O chamado não está aguardando fornecedor (status atual: EM_ATENDIMENTO)." |
| Sem token | `401` | — |

> **Chamados antigos:** os que já aguardavam fornecedor antes da HU07 ganharam, na migration V8, um escalonamento aberto com fornecedor "Não informado", para poderem sair da espera pelo retorno.

---

## 13. Evolução do Ciclo de Atendimento (HU03, HU04 — Planejados)

* **`POST /chamados/{id}/vinculos`** (`HU03`): associa um registro de contato duplicado a um chamado principal em andamento.
* **`PATCH /chamados/{id}/prioridade`** (`HU04`): registra a triagem estruturada pela Matriz 3D (`severidade`, `urgencia`, `impacto_operacional`).
