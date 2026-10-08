# 📄 Documentação da API - MVP (Chamados)

> **Projeto:** Nexo - Plataforma de Gerenciamento de Chamados (Rede Lúmen Diagnósticos)
> **Referência:** Histórias de Usuário HU01 a HU04, HU09 e HU10

Esta documentação descreve os primeiros endpoints da API RESTful projetada para atender aos fluxos iniciais do MVP. O design foi concebido com base nas regras de negócio de que chamados não devem transitar dados clínicos sensíveis, devem exigir a unidade e que a prioridade é definida por critérios estruturados (Severidade, Urgência, Impacto).

**Base URL:** `/api/v1`

---

## Padrão de Resposta (ApiResponse)

Todas as requisições da API retornam um contrato padronizado (wrapper) para garantir consistência no consumo pelo front-end e integrações, contendo:

```json
{
  "status_code": 200, // Código HTTP refletido no payload
  "message": "Mensagem descritiva da operação",
  "data": { ... }, // O objeto retornado (null se não houver dados)
  "errors": null // Lista de erros, se houver, ex: ["Campo 'unidade_id' é obrigatório"]
}
```

---

## 1. Registrar um Chamado (HU01)

Cria um novo chamado estruturado a partir de uma solicitação recebida por um dos canais de entrada.

**Endpoint:** `POST /chamados`
**Autenticação:** Bearer Token (Analista ou Sistema Integrador)

### Request Body (application/json)

```json
{
  "unidade_id": "uuid-da-unidade-obrigatoria",
  "canal_origem": "LIGACAO", // LIGACAO, PRESENCIAL, WHATSAPP, EMAIL
  "solicitante": {
    "nome": "João da Silva",
    "contato": "+5581999999999" // E.164 ou E-mail
  },
  "descricao": "Impressora da recepção travou e não puxa papel. Apita vermelho."
}
```

> **Aviso de Negócio (RN01):** O frontend deve garantir a exibição de um alerta para evitar o envio de dados clínicos ou exames completos no campo `descricao`.

### Response: `201 Created`

```json
{
  "status_code": 201,
  "message": "Chamado registrado com sucesso",
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "codigo": "LUM-2026-000123",
    "status": "ABERTO",
    "data_criacao": "2026-10-07T14:30:00Z"
  },
  "errors": null
}
```

---

## 2. Consultar o Chamado e Histórico (HU02)

Recupera os detalhes completos do chamado, incluindo a linha do tempo cronológica com todos os eventos e registros de contato.

**Endpoint:** `GET /chamados/{id}`
**Autenticação:** Bearer Token (Analista)

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
      "id": "uuid-unidade",
      "nome": "Unidade Boa Viagem"
    },
    "responsavel": null,
    "prioridade": null,
    "historico": [
      {
        "id_evento": "uuid-evento-1",
        "tipo": "CRIACAO",
        "data": "2026-10-07T14:30:00Z",
        "autor": "Maria Analista",
        "detalhes": {
          "descricao": "Chamado aberto via Ligação."
        }
      }
    ]
  },
  "errors": null
}
```

---

## 3. Adicionar Atualização ao Histórico (HU02)

Permite ao analista incluir novas anotações, investigações ou respostas no histórico do chamado.

**Endpoint:** `POST /chamados/{id}/atualizacoes`
**Autenticação:** Bearer Token (Analista)

### Request Body (application/json)

```json
{
  "descricao": "Entrei em contato com a unidade, pediram para aguardar o fim do plantão para reiniciar o equipamento.",
  "visibilidade": "INTERNA" // INTERNA ou PUBLICA (se for enviar notificação)
}
```

### Response: `201 Created`

```json
{
  "status_code": 201,
  "message": "Atualização adicionada ao histórico",
  "data": {
    "id_evento": "uuid-evento-2",
    "tipo": "ATUALIZACAO",
    "data": "2026-10-07T15:00:00Z"
  },
  "errors": null
}
```

---

## 4. Vincular Relato / Identificar Duplicidade (HU03)

Associa um novo registro de contato a um chamado em andamento.

**Endpoint:** `POST /chamados/{id}/vinculos`
**Autenticação:** Bearer Token (Analista)

### Request Body (application/json)

```json
{
  "registro_contato_id": "uuid-do-registro-duplicado",
  "motivo": "Mesmo problema reportado pela enfermeira do turno da tarde."
}
```

### Response: `200 OK`

```json
{
  "status_code": 200,
  "message": "Relato vinculado com sucesso",
  "data": {
    "id_evento": "uuid-evento-3"
  },
  "errors": null
}
```

---

## 5. Definir a Prioridade do Chamado (HU04)

Registra a triagem estruturada baseada nos eixos de matriz de decisão.

**Endpoint:** `PATCH /chamados/{id}/prioridade`
**Autenticação:** Bearer Token (Analista / Liderança de TI)

### Request Body (application/json)

```json
{
  "severidade": 2, // 1 (Baixo), 2 (Médio), 3 (Alto)
  "urgencia": 3,
  "impacto_operacional": 1
}
```

### Response: `200 OK`

```json
{
  "status_code": 200,
  "message": "Prioridade definida com sucesso",
  "data": {
    "status": "EM_ATENDIMENTO",
    "prioridade_calculada": "ALTA",
    "id_evento": "uuid-evento-4"
  },
  "errors": null
}
```

---

## 6. Autenticar no Sistema (Login - HU09)

Autentica um usuário, devolvendo o token de acesso.

**Endpoint:** `POST /auth/login`
**Autenticação:** Nenhuma (Pública)

### Request Body (application/json)

```json
{
  "email": "analista@lumendiagnosticos.com.br",
  "senha": "senha-segura-aqui"
}
```

### Response: `200 OK`

```json
{
  "status_code": 200,
  "message": "Autenticação realizada com sucesso",
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "usuario": {
      "id": "uuid-do-usuario",
      "nome": "João Analista",
      "email": "analista@lumendiagnosticos.com.br",
      "perfil": "ANALISTA_SUPORTE"
    }
  },
  "errors": null
}
```

---

## 7. Cadastrar Novo Analista (Register - HU10)

Cadastra novos analistas de suporte no sistema.

**Endpoint:** `POST /analistas`
**Autenticação:** Bearer Token (Liderança de TI)

### Request Body (application/json)

```json
{
  "nome": "Carlos Silva",
  "email": "carlos.silva@lumendiagnosticos.com.br",
  "senha": "senha-inicial-segura",
  "perfil": "ANALISTA_SUPORTE"
}
```

### Response: `201 Created`

```json
{
  "status_code": 201,
  "message": "Analista cadastrado com sucesso",
  "data": {
    "id": "uuid-do-novo-analista",
    "nome": "Carlos Silva",
    "email": "carlos.silva@lumendiagnosticos.com.br",
    "perfil": "ANALISTA_SUPORTE",
    "ativo": true,
    "data_criacao": "2026-10-07T16:00:00Z"
  },
  "errors": null
}
```
