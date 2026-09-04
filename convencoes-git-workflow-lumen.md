# Convenções de Desenvolvimento — Git Flow, Branches, Commits e PR

> Projeto: Sistema de Suporte — Rede Lúmen Diagnósticos  
> Prazo de entrega estimado: aproximadamente 3 meses  
> Documento de referência para a equipe de desenvolvimento.

---

## 1. Estratégia de branches (Git Flow simplificado)

Para um projeto de 3 meses com equipe enxuta, adota-se uma versão **simplificada** do Git Flow — sem branches de `release` separadas, evitando overhead desnecessário e mantendo o rastreamento das User Stories (US).

```
main                → código estável, sempre "pronto para entrega/demo"
develop             → integração contínua das features (branch base do dia a dia)
feat/US-xx-nome     → nova funcionalidade associada à User Story de referência
fix/US-xx-nome      → correção de bug associada à US ou tarefa
hotfix/*            → correção urgente direto sobre main (produção)
outros prefixos     → alinhados às nomenclaturas: docs/*, style/*, refactor/*, test/*, chore/*, perf/*
```

### Fluxo de trabalho

1. Toda nova funcionalidade nasce a partir de `develop`:
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b feat/US-01-nome-da-feature
   ```
2. Ao concluir e testar localmente, abre-se um **Pull Request** de `feat/US-01-nome-da-feature` → `develop`.
3. Periodicamente (ex: ao fim de cada sprint/semana), `develop` é mesclada em `main` via PR, marcando um ponto estável.
4. `hotfix/*` só é usada para correções urgentes em produção que não podem esperar o próximo ciclo — nasce de `main` e é mesclada tanto em `main` quanto em `develop`.

### Convenção de nomes de branch

Os prefixos das branches seguem os mesmos tipos padronizados dos commits (`feat | fix | docs | style | refactor | test | chore | perf`), além de `hotfix`:

| Prefixo | Tipo correspondente | Uso | Exemplo |
|---|---|---|---|
| `feat/US-{número}-{nome}` | `feat` | Nova funcionalidade vinculada à User Story | `feat/US-01-motor-priorizacao` |
| `fix/US-{número}-{nome}` (ou `fix/{nome}`) | `fix` | Correção de bug associado à US/tarefa | `fix/US-03-calculo-sla` ou `fix/login-expirado` |
| `docs/{nome}` | `docs` | Alterações puramente em documentação | `docs/atualizar-instrucoes-setup` |
| `style/{nome}` | `style` | Formatação e estilo sem impacto em regras de negócio | `style/padronizacao-eslint` |
| `refactor/{nome}` | `refactor` | Refatoração de código sem alteração de comportamento | `refactor/service-chamados` |
| `test/{nome}` | `test` | Adição ou correção de testes automatizados | `test/testes-triagem-priorizacao` |
| `chore/{nome}` | `chore` | Tarefas de manutenção, build, dependências ou configs | `chore/atualizar-docker-compose` |
| `perf/{nome}` | `perf` | Otimização e ganho de performance | `perf/otimizar-queries-dashboard` |
| `hotfix/{nome}` | `fix` urgente | Correção urgente em produção (direto na `main`) | `hotfix/queda-banco-auth` |

Regras de nomenclatura:
- Sempre em minúsculas, com hífen separando as palavras.
- Indicar sempre o identificador da **User Story de referência** nas features (ex: `feat/US-05-painel-sla`).
- Nome curto, claro e objetivo.
- Sem acentos, cedilhas ou caracteres especiais.

---

## 2. Convenção de commits — Conventional Commits

Adotamos o padrão **Conventional Commits** para padronização e histórico limpo.

> [!IMPORTANT]
> **Nomenclaturas Padronizadas e Idioma:**
> - Tipos permitidos: `feat | fix | docs | style | refactor | test | chore | perf`
> - O **tipo** e o **escopo** seguem o padrão técnico em inglês.
> - A **mensagem** (título/descrição, corpo e rodapé) deve ser **obrigatoriamente escrita em PORTUGUÊS**.

### Formato

```
<tipo>(<escopo opcional>): <descrição curta no imperativo e em português>

<corpo opcional explicando o "porquê" da mudança, também em português>

<rodapé opcional com referência à US ou issue>
```

### Tipos padronizados (em inglês)

| Tipo | Quando usar |
|---|---|
| `feat` | Nova funcionalidade |
| `fix` | Correção de bug |
| `docs` | Alterações puramente em documentação |
| `style` | Formatação e estilo sem alteração de lógica (lint, espaços, ponto e vírgula) |
| `refactor` | Refatoração de código sem alteração de regra de negócio |
| `test` | Adição ou correção de testes unitários/integração |
| `chore` | Manutenção de build, ferramentas, dependências ou scripts |
| `perf` | Melhoria de desempenho/performance |

### Exemplos de commits (todos os tipos com mensagem em português)

```
feat(chamados): adiciona cálculo automático de score de urgência

fix(auth): corrige expiração de sessão em computadores compartilhados

docs(readme): atualiza instruções de setup do docker-compose

style(triagem): padroniza identação e formatação visual dos componentes

refactor(backend): extrai lógica de SLA para service dedicado

test(priorizacao): adiciona testes unitários para cálculo de fila prioritária

chore(deps): atualiza versão do Express para 4.19

perf(dashboard): otimiza consulta de listagem de chamados abertos
```

---

## 3. Template de Commit (Guia Rápido para Consulta)

Para facilitar o dia a dia da equipe, utilize a estrutura abaixo como referência rápida ao commitar:

```gitcommit
# ==============================================================================
# <tipo>(<escopo>): <título curto no imperativo, max ~72 chars - EM PORTUGUÊS>
# 
# Tipos: feat | fix | docs | style | refactor | test | chore | perf
# ==============================================================================
feat(triagem): adiciona classificação automática de chamados por criticidade

# [Corpo - Opcional]: Descreva o MOTIVO da alteração (o porquê, não o como)
Integra o modelo de priorização para classificar chamados de exames críticos
com score elevado, reduzindo o tempo de espera no atendimento ambulatorial.

# [Rodapé - Opcional]: Referência à User Story ou issue correspondente
Ref: US-02
Closes #15
```

### Dica: Configurando o template no Git local

Você pode salvar este modelo como `.gitmessage` na raiz do projeto e ativá-lo com:
```bash
git config commit.template .gitmessage
```

---

## 4. Template de Pull Request

Crie o arquivo `.github/PULL_REQUEST_TEMPLATE.md` no repositório com o conteúdo abaixo para que o GitHub carregue automaticamente a estrutura:

```markdown
## Descrição

<!-- Explique o que este PR faz e qual problema resolve de fato. -->

## User Story / Tarefa Relacionada

- US de referência: US-XX
- Issue: Closes #XX

## Tipo de mudança

<!-- Marque o tipo correspondente (feat | fix | docs | style | refactor | test | chore | perf) -->
- [ ] Nova funcionalidade (`feat`)
- [ ] Correção de bug (`fix`)
- [ ] Documentação (`docs`)
- [ ] Formatação / estilo sem impacto em lógica (`style`)
- [ ] Refatoração de código (`refactor`)
- [ ] Adição ou ajuste de testes (`test`)
- [ ] Manutenção / infraestrutura / dependências (`chore`)
- [ ] Otimização de desempenho (`perf`)

## Como testar

<!-- Passo a passo detalhado para validação local da mudança -->

1. 
2. 
3. 

## Checklist de Validação

- [ ] Código segue as convenções do projeto
- [ ] Testado localmente com sucesso
- [ ] Sem dados sensíveis (dados de pacientes, tokens, exames reais) em código, logs ou commits
- [ ] Documentação atualizada (se aplicável)
- [ ] Branch sincronizada com a `develop`

## Screenshots / Evidências (se aplicável)

<!-- Prints de tela ou logs demonstrando o funcionamento -->
```

---

## 5. Regras de Revisão e Aprovação de Pull Requests

### Critérios de Aceitação e Aprovação

- **Resolução Efetiva do Problema:** O Pull Request **só será aprovado se estiver resolvendo de fato o problema proposto** na User Story/tarefa, atendendo aos critérios de aceitação e às convenções de código.
- **Devolução com Feedback:** Caso o PR não atenda aos requisitos ou apresente inconsistências, ele **NÃO será mesclado**; será devolvido via solicitação de alterações (*Changes Requested*) com uma resposta clara e detalhada apontando o que precisa ser ajustado e atualizado para sua continuação.

### Responsáveis pela Revisão

- A revisão técnica e de negócio dos PRs é realizada prioritariamente pelo **Tech Lead** ou por um **Desenvolvedor Fullstack**.
- **Regra de Contingência/Escalonamento:** Caso o Desenvolvedor Fullstack tenha dúvidas, encontre divergências ou não consiga efetuar a aprovação (por complexidade ou escopo crítico), o **Tech Lead** fará a revisão e a aprovação posterior, desde que tudo esteja estritamente de acordo.

### Boas Práticas Gerais

- **Nunca commitar diretamente em `main` ou `develop`** — todo código entra obrigatoriamente via Pull Request.
- **Tamanho dos PRs:** Preferir PRs focados e objetivos em vez de entregas gigantes com múltiplos escopos misturados.
- **Squash and Merge:** Ao mesclar o PR em `develop` ou `main`, utilizar preferencialmente **Squash and Merge** para condensar o histórico da branch em um único commit conciso e rastreável.
- **Segurança e Privacidade:** Nunca commitar arquivos `.env`, credenciais de banco ou dados simulados com informações reais de pacientes (LGPD).

---

*Documento complementar à arquitetura técnica do Sistema de Suporte — Rede Lúmen Diagnósticos.*

