# 🏛️ Architecture Decision Records (ADR) — Nexo

> Registro histórico e técnico das decisões arquiteturais relevantes da plataforma **Nexo** (Rede Lúmen Diagnósticos).

---

## O que é um ADR?

Um **Architecture Decision Record (ADR)** é um documento curto que captura uma decisão arquitetural importante tomada pela equipe, juntamente com seu **contexto**, as **alternativas avaliadas**, a **decisão escolhida** e suas **consequências** (positivas e trade-offs).

Adotamos este padrão no repositório para:
1. **Preservar o conhecimento arquitetural:** Garantir que qualquer membro atual ou futuro da Squad entenda o *porquê* a arquitetura foi desenhada desta forma.
2. **Evitar discussões circulares:** Documentar claramente os critérios técnicos e de negócio que motivaram cada escolha.
3. **Rastreabilidade de evolução:** Conectar refatorações estruturais, requisitos não funcionais (`RNF`) e boas práticas de engenharia (Clean Architecture, DDD, SOLID).

---

## Ciclo de Vida de um ADR (Status)

| Status | Significado |
|--------|-------------|
| 🟡 **Proposto** | Em discussão ou revisão técnica pela equipe / Tech Lead |
| 🟢 **Aceito** | Decisão aprovada e implementada no código-fonte |
| ⚪ **Depreciado** | Contexto mudou e a decisão deixou de ser aplicável |
| 🔄 **Substituído** | Substituído por um ADR mais recente (com link cruzado) |

---

## Índice de Decisões Arquiteturais

| ID | Título | Status | Data | Referência |
|----|--------|--------|------|------------|
| [ADR-001](./ADR-001-modularizacao-package-by-feature.md) | Modularização do Backend em Pacotes por Funcionalidade (*Package-by-Feature / Vertical Slices*) | 🟢 Aceito | 2026-10-28 | Commit `c86dee5` / PR #21 |

