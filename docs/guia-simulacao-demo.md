# Guia de Simulação Operacional e Apresentação (Perfil `demo`)

Guia rápido de consulta para subir o **Nexo** com massa simulada em escala real da Rede Lúmen Diagnósticos (**180 chamados em 60 dias · 14 unidades ativas · 7 analistas · 30 solicitantes · 5 fornecedores**) e apresentar o **Painel Administrativo (HU08)** e a **Fila de Chamados (HU14)** para gestores, mentores ou banca.

---

## 1. Como funciona o isolamento entre ambientes

| Perfil Spring | `nexo.origem-dado` | Carrega `SimulacaoOperacaoSeeder`? | Uso |
| :--- | :---: | :---: | :--- |
| **Padrão (`application.yml`)** | `TESTE` | Não | Desenvolvimento diário e execução do `mvn test` |
| **`demo` (`application-demo.yml`)** | `SIMULADO` | Sim (`nexo.seed.simulacao-habilitada=true`) | Simulação operacional local ou em homologação/banca |
| **`prod` (`application-prod.yml`)** | `REAL` | Nunca | Produção (bean `@Profile("demo")` nem é instanciado) |

### Arquivos envolvidos
- Configuração do perfil: `backend/src/main/resources/application-demo.yml`
- Seeder determinístico (`Random(20261119L)`): `backend/src/main/java/com/avandesk/api/core/seed/SimulacaoOperacaoSeeder.java`
- Contagem idempotente: `backend/src/main/java/com/avandesk/api/chamado/repository/ChamadoRepository.java` (`countByOrigemDado`)
- Teste integrado (PostgreSQL real com rollback): `backend/src/test/java/com/avandesk/api/core/seed/SimulacaoOperacaoSeederIntegracaoTest.java`

---

## 2. Passo a passo para subir a simulação

### Opção A — Docker Compose completo (recomendado para apresentação local)

```powershell
# Na raiz do repositório (sobe Postgres, Mailpit, Backend com perfil demo e Frontend)
docker compose -f docker-compose.yml -f docker-compose.demo.yml up -d --build
```

Acesse `http://localhost:4200` (API em `http://localhost:8080`, Mailpit em `http://localhost:8025`).

### Opção B — Subindo manual (Postgres no Docker + Maven/Angular locais)

### Passo 1 — Subir o PostgreSQL (na raiz do repositório)
```powershell
docker compose up -d postgres
```

### Passo 2 — Subir o backend com o perfil `demo`

**PowerShell (Windows)** — mantenha as aspas em `"-D..."`:
```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

**Bash / Linux / macOS:**
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

**IDE (IntelliJ / VS Code), Docker ou Render (staging/banca):**
Defina a variável de ambiente:
```text
SPRING_PROFILES_ACTIVE=demo
```

> **Confirmação no log de subida:**
> - Primeira execução: `Simulação operacional concluída: 180 chamados criados (origem_dado=SIMULADO, 14 unidades ativas, 7 analistas, janela de 60 dias).`
> - Reinicializações seguintes: `Simulação operacional (origem_dado=SIMULADO) já presente no banco — seeder ignorado.`

### Passo 3 — Subir o frontend (se ainda não estiver rodando)
```powershell
cd frontend
npm start
```
Acesse `http://localhost:4200`.

---

## 3. Credenciais da simulação

Todos os usuários de desenvolvimento/simulação usam a senha padrão **`nexo-dev-2026`**.

| Perfil | E-mail | O que consegue demonstrar |
| :--- | :--- | :--- |
| **`LIDERANCA_TI`** | `lideranca@lumen.local` | **Painel Administrativo (`/api/v1/dashboard/metricas`)**, gestão de unidades e analistas |
| `ANALISTA_SUPORTE` | `analista1@lumen.local` | Fila de chamados, triagem Matriz 3D, escalonamento e resolução |
| `ANALISTA_SUPORTE` | `analista2@lumen.local` | Fila de chamados e atendimento |
| `ANALISTA_SUPORTE` | `camila.ferreira@lumen.local` | Analista criada pelo seeder (carga em M02) |
| `ANALISTA_SUPORTE` | `rafael.monteiro@lumen.local` | Analista criada pelo seeder (carga em M02) |
| `ANALISTA_SUPORTE` | `lucas.barbosa@lumen.local` | Analista criada pelo seeder (carga em M02) |
| `ANALISTA_SUPORTE` | `mariana.alencar@lumen.local` | Analista criada pelo seeder (carga em M02) |
| `ANALISTA_SUPORTE` | `thiago.santana@lumen.local` | Analista criada pelo seeder (carga em M02) |
| **`SOLICITANTE`** | `ana.ribeiro@lumen.local` | Conta de solicitante verificada (HU17) vinculada aos chamados simulados |

---

## 4. O que a massa simulada popula (180 chamados / 60 dias)

As datas são calculadas relativamente a `Instant.now()` no momento da carga, garantindo cobertura completa da janela padrão do dashboard (30 dias atuais + 30 dias anteriores):

- **Porte da Rede Lúmen:**
  - **14 unidades ativas** (5 base + 9 criadas idempotentes: `Derby`, `Graças`, `Espinheiro`, `Olinda`, `Casa Forte`, `Pina`, `Madalena`, `Paulista`, `Cabo de Santo Agostinho`), com distribuição ponderada (Pareto) concentrando maior volume nas unidades 24h e grandes polos.
  - **7 analistas ativos** + **30 solicitantes** + **15 cenários clínicos distintos** marcados por setor (`[Recepção]`, `[Posto de Coleta]`, `[Sala de Laudos]`, `[Tomografia]`, etc.).
- **Período anterior (`D-58` a `D-32`) — 72 chamados:**
  - 60 encerrados (`RESOLVIDO`/`FECHADO`) com SLA ~70% e mediana mais alta, formando base comparativa real para os deltas de melhoria do período atual.
  - 6 `MESCLADO` e 6 `CANCELADO`.
- **Período atual (`D-29` a `D-0`) — 108 chamados:**
  - **54 `RESOLVIDO` / `FECHADO`:**
    - **12 P1 (Alta):** 9 dentro / 3 fora → **75.0%** (`abaixoDaMeta = true` frente à meta de 80%).
    - **24 P2 (Média):** 21 dentro / 3 fora → **87.5%**.
    - **18 P3 (Baixa):** 16 dentro / 2 fora → **88.9%**.
  - **Pausas de relógio de SLA (`eventos_historico`):** ~18 chamados resolvidos com ciclo `CHAMADO_AGUARDANDO` (`motivo_espera = SOLICITANTE`) → `NOTIFICACAO_ENVIADA` → `RETORNO_RECEBIDO`, mais contatos de ida/volta (`RECEBIDO` / `ENVIADO`) e `ANOTACAO_ADICIONADA` na timeline.
  - **Rastreabilidade (M01):** 5 chamados resolvidos propositalmente sem `RegistroContato` para manter conformidade realista (~90.7%), evitando um 100% artificial.
  - **33 chamados ativos na fila (`9 ABERTO`, `6 EM_TRIAGEM`, `9 EM_ATENDIMENTO`, `9 AGUARDANDO`):** inclui **9 sem responsável** (faixa `"Sem responsável"` no gráfico M02), 6 aguardando fornecedor com escalonamento aberto e 3 aguardando solicitante.
  - **9 `REABERTO`** (alimenta M06 — taxa de reabertura realista ~14%).
  - **6 `MESCLADO`** apontando para chamados principais (alimenta M07 — duplicidade ~5.5%).
  - **6 `CANCELADO`** com motivos variados.
- **Tempo em terceiros (M05 — tabela `escalonamentos`):**
  - 5 fornecedores com perfis de retorno bem contrastados:
    1. `Vivo Empresas` (~22h — principal gargalo)
    2. `GE HealthCare` (~16h — equipamentos de imagem)
    3. `Philips Saúde` (~11h — PACS/RIS)
    4. `TOTVS Saúde` (~7h — sistema hospitalar)
    5. `Simpress` (~3h30 — parque de impressoras)
- **Problemas recorrentes (8 causas raiz com ranking Pareto):**
  1. `Falha no spooler da impressora térmica de etiquetas`
  2. `Instabilidade intermitente no link VPN MPLS`
  3. `Timeout na integração do Portal de Resultados (LIS)`
  4. `Bloqueio recorrente de credenciais AD na recepção`
  5. `Atraso na sincronização DICOM/PACS`
  6. `Desconfiguração de leitor de código de barras na coleta`
  7. `Falha no reconhecimento de token/certificado digital A3`
  8. `Oscilação de sinal Wi-Fi nos coletores/tablets`
- **Indicadores 9.7 (M08, M11, M12):**
  - **M08:** divergências de prioridade final vs. sugerida com justificativa obrigatória (`aplicarTriagemPrioridade`).
  - **M11:** tempo de registro médio ~39s (abaixo do limiar de alerta de 60s).
  - **M12:** `0` incidentes de privacidade (status conforme).

---

## 5. Como resetar a simulação do zero

A tabela `eventos_historico` possui a trigger `trg_eventos_historico_imutavel` (`V5`), que bloqueia `UPDATE` e `DELETE` por requisito de auditoria. Por isso, o seeder **nunca apaga registros** — ele apenas ignora se já existirem chamados `SIMULADO`.

Para regenerar os 180 chamados com datas frescas (por exemplo, semanas depois antes de uma apresentação), recrie o volume local do Postgres:

```powershell
# Na raiz do repositório (Docker completo com massa demo)
docker compose -f docker-compose.yml -f docker-compose.demo.yml down -v
docker compose -f docker-compose.yml -f docker-compose.demo.yml up -d --build
```

Ou, se estiver rodando o backend via Maven:

```powershell
docker compose down -v
docker compose up -d postgres
```

Em seguida, suba o backend novamente com `"-Dspring-boot.run.profiles=demo"`. O Flyway recriará todas as tabelas e o seeder gerará os 180 chamados atualizados para a data do dia.

> **Nota sobre a Fila de Chamados (`HU14`):** o `DashboardRepository` filtra por `origem_dado`, mas a listagem da fila (`GET /api/v1/chamados`) hoje exibe todos os chamados presentes no banco. Se você rodar em `demo` e depois voltar para o perfil padrão sem resetar o volume, os chamados `SIMULADO` continuarão visíveis na fila (embora não entrem nas métricas `TESTE` do dashboard).

---

## 6. Roteiro sugerido de 2 minutos para apresentar ao mentor

1. **Login como `lideranca@lumen.local`** e abertura do **Painel Administrativo**: destacar os comparativos automáticos entre os últimos 30 dias (108 chamados) e os 30 dias anteriores (72 chamados), mostrando melhoria de SLA e redução da mediana.
2. **SLA por Prioridade:** apontar o alerta visual em **Alta (P1)** em 75% (abaixo da meta de 80%), enquanto P2 e P3 estão conformes.
3. **Chamados por Unidade:** destacar as **14 unidades da rede** com distribuição realista de volume (unidades 24h puxando demanda).
4. **Tempo em Terceiros (M05) e Problemas Recorrentes:** mostrar o ranking dos **5 fornecedores** (`Vivo Empresas` e `GE HealthCare` como gargalos) e o Top Recorrentes liderado pela impressora térmica de etiquetas e link VPN.
5. **Carga por Responsável (M02) + Fila:** mostrar a distribuição entre os **7 analistas** e a barra explícita de **9 chamados `"Sem responsável"`**.
6. **Ação ao vivo:** abrir 1 chamado novo durante a apresentação — como o ambiente está em `nexo.origem-dado=SIMULADO`, ele é contabilizado imediatamente no painel sem misturar com dados `REAL`.

---

## 7. Defesa técnica (Code Review / Entrevista)

- **Por que `ApplicationRunner` com `@Profile("demo")` em vez de Flyway SQL (`db/seed/`)?**
  - Reaproveita a regra de domínio `CalculadoraPrioridadeMatriz3D`, consome `chamados_codigo_seq` sem colidir com novos chamados, gera timestamps relativos a `Instant.now()` (nunca expiram fora da janela de 30 dias), mantém `mvn test` limpo (`TESTE`) e garante que `prod` (`REAL`) sequer instancie o bean.
- **Ponto de evolução em escala (multi-réplica):**
  - Se múltiplas instâncias subirem simultaneamente com `SPRING_PROFILES_ACTIVE=demo` em um banco vazio, pode haver corrida na checagem `countByOrigemDado == 0`. Em ambiente distribuído, o ajuste natural é envolver a checagem com `pg_advisory_xact_lock` no PostgreSQL.
