# 📚 Documentação do Projeto — Nexo

> Repositório central de documentação técnica e de produto da plataforma Nexo (Rede Lúmen Diagnósticos).

## Índice de Documentos

| # | Documento | Descrição |
|---|-----------|-----------|
| 0 | [Definição Técnica & Ambiente](./definicao-tecnica-ambiente.md) | Setup inicial, stack técnica, governança e justificativas |
| 0.1 | [Plano de Setup do Ambiente](./plano-setup-ambiente.md) | Guia prático de setup local (Docker, Spring Boot, Angular, PostgreSQL) |
| 1 | [Análise de Domínio](./01-analise-de-dominio-Nexo.md) | Domínio, linguagem ubíqua, atores, Matriz 3D e regras da Rede Lúmen |
| 2 | [Requisitos Funcionais](./02-requisitos-funcionais.md) | Funcionalidades do MVP em MoSCoW e rastreabilidade |
| 3 | [Requisitos Não Funcionais](./03-requisitos-nao-funcionais.md) | Atributos de qualidade (ISO/IEC 25010) e segurança em saúde |
| 4 | [Histórias de Usuário do MVP](./04-historias-de-usuario-Nexo.md) | Histórias de usuário do MVP (HU01 a HU15) com critérios de aceite |
| 5 | [Backlog Geral do MVP](./05-backlog-geral-mvp.md) | Backlog priorizado por levas e rastreabilidade da seção 9 |
| 6 | [Documentação da API](./06-documentacao-api-mvp.md) | Especificação dos endpoints REST e contratos da API |
| 7 | [Contrato OpenAPI (YAML)](./openapi-mvp.yaml) | Especificação OpenAPI 3.0 dos endpoints |
| 8 | [Convenções de Git Flow & Commits](../.github/convencoes-git-workflow-lumen.md) | Padrões de branches, commits e fluxo de Pull Request |

---

## Convenções

- **User Stories** seguem o padrão **INVEST** (Independent, Negotiable, Valuable, Estimable, Small, Testable).
- **Prioridades** usam a escala **MoSCoW** (Must, Should, Could, Won't).
- **Estimativas** usam **Story Points** (Fibonacci: 1, 2, 3, 5, 8, 13).
- **IDs** seguem o padrão `RF-XXX` (Requisitos Funcionais), `RNF-XXX` (Requisitos Não Funcionais), `US-XXX` (User Stories), `T-XXX` (Tasks).
