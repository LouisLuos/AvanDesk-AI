# Avaliação Técnica do Time — Aderência à Seção 9 (Métricas) do Documento do MVP

> **Data da avaliação:** 14/10/2026  
> **Base avaliada:** branch `develop` (commit `c62fef8`)  
> **Referência:** Documento do MVP, aba **SLA**, Seção 9 (9.1 Princípios de medição · 9.2 Métricas M01–M12 · 9.3 Papel dos testes · 9.4 Plano de coleta · 9.5 Requisitos de instrumentação · 9.6 Limitações · 9.7 Leitura dos resultados)  
> **Versão do documento de referência:** rascunho de 07/10/2026 (Sarah Cyrne), seções 1 a 9. É a única entrada no Controle de Revisão.  
> **Autor:** Tech Lead (Luiz Henrique)

---

## 1. Resumo executivo

| Item | Situação |
| :--- | :--- |
| Cronograma da Seção 9.4 | 🔴 **Atrasado.** A instrumentação deveria ter começado em 07/10 junto com HU01 e HU05. A semana de coleta (12/10) já começou e o protótipo ainda não registra chamados de verdade. |
| Instrumentação (Seção 9.5) | 🔴 **0 de 11 itens implementados.** Não existem as tabelas `chamado`, `evento_historico`, `registro_contato` nem `escalonamento`. |
| Métricas M01–M12 | 🔴 **Nenhuma métrica pode ser calculada hoje.** O backlog só tem tarefa explícita para M01 (T15.3). |
| Testes (Seção 9.3) | 🔴 **Nenhum dos 5 tipos de teste existe.** Há só um teste de contexto no backend e um teste de componente raiz no frontend. |
| Base técnica (stack, contrato, envelope, CI) | 🟢 **Boa.** O contrato OpenAPI já prevê histórico com `tipo`, `data` e `autor`, o que é a base de M01–M08. |
| Backlog (tarefas SMART) | 🟡 **Incompleto em relação à Seção 9.** Faltam tarefas de instrumentação, telemetria, cenário de demonstração e consultas por métrica. Responsável, estimativa e prazo estão vazios em todas as tarefas. |

> [!CAUTION]
> A própria Seção 9.5 avisa: *"Esses dados não podem ser reconstruídos depois. Se não forem gravados desde o primeiro chamado, a primeira coleta perde a informação."*
> Não dá para fazer a coleta da semana de 12/10 como foi planejada. **É preciso replanejar a janela de coleta** e implementar o registro de eventos **antes** de qualquer outra funcionalidade nova.

> [!IMPORTANT]
> **Causa raiz do atraso: planejamento e comunicação, não só execução.** Segundo o Controle de Revisão, o plano de coleta foi publicado como **rascunho em 07/10**, no mesmo dia em que a instrumentação deveria começar (9.4). Não houve revisão posterior nem registro de que o time aceitou o plano. Na prática, o backlog técnico (HUs e tarefas SMART) não foi atualizado para refletir a Seção 9. A correção passa por: (1) uma revisão formal do documento com nova data de coleta; (2) incorporar a Seção 9 ao backlog; (3) combinar que mudanças no documento de produto disparam uma revisão do backlog técnico.

---

## 2. Avaliação das decisões técnicas já tomadas

| # | Decisão | Aderência à Seção 9 | Veredito | Ação recomendada |
| :-- | :--- | :--- | :--- | :--- |
| D1 | Implementar HU11 (Unidades) antes de HU01/HU05 | Respeita a tabela de dependências das HUs (HU01 depende de HU11). Porém a 9.4 pede **registro de eventos + campos da 9.5 junto com HU01 e HU05, antes das demais**. | 🟡 Justificável | Manter HU11. A próxima entrega **obrigatoriamente** é a base de eventos + HU01 (backend) + HU05. |
| D2 | Autenticação básica temporária (`spring.security.user` admin/admin) até a HU09 | A 9.5 exige o **autor** de todos os eventos. Com um único usuário fixo, todos os eventos ficam com o mesmo autor, o que invalida M03 por responsável, M02 e a separação de perfis (RN06/M12). | 🔴 Bloqueante para coleta | Antecipar o mínimo da HU09 (tabela `usuario` + perfil + JWT) antes da coleta, ou pelo menos usuários *in-memory* distintos por analista. |
| D3 | Contrato OpenAPI (`docs/openapi-mvp.yaml`) com histórico `tipo/data/autor` | Cobre a base de M01–M08. **Faltam:** prioridade sugerida × final + justificativa (M08), motivo de espera (M03/M04), primeira resposta (M03), escalonamento (M05), mesclagem (M07), direção + identificador externo do contato (M09/RN16), `AnexoVisualizado` (M12), marcação real/teste/simulado (todas). | 🟡 Parcial | Versionar o contrato (v1.1) com os campos da 9.5 antes de implementar o backend de Chamado. |
| D4 | Enum `canal_origem` = `LIGACAO, PRESENCIAL, WHATSAPP, EMAIL` | A HU01 pede também **"outro"**. A M09 lista só 4 canais. | 🟡 Inconsistência | Decidir com a Sarah (produto) se `OUTRO` entra. Se entrar, atualizar contrato, model do front e a M09. |
| D5 | API simulada no front (`mock-api.interceptor.ts`, `useMocks: true` só em dev) | Ajuda a trabalhar em paralelo. A 9.1 (fonte única) e a 9.6 (dados simulados não contam) exigem que **nenhum dado da coleta passe pelo mock**. Em produção está `useMocks: false`. | 🟢 Ok com ressalva | Garantir no CI/build de produção que `useMocks` é `false`. Exibir um selo "MODO SIMULADO" na UI quando o mock estiver ativo. |
| D6 | Datas sem fuso explícito (`application.yml` sem `hibernate.jdbc.time_zone`) | A 9.5 exige datas e horas **em UTC**. | 🔴 Risco de dado errado | Usar `TIMESTAMPTZ` nas migrations + `spring.jpa.properties.hibernate.jdbc.time_zone: UTC` + `Instant`/`OffsetDateTime` nas entidades. |
| D7 | Tabela `unidades` sem `tipo` (unidade/sede) | A Análise de Domínio (seção 5) adiciona `tipo`. Não afeta métricas diretamente. A M04 é por unidade e já funciona. | 🟢 Baixo impacto | Adicionar em migration futura (V3+), sem urgência. |
| D8 | Testes: JUnit + Mockito / Vitest declarados no README | A 9.3 exige 5 tipos de teste (cálculo, eventos, permissão/privacidade, cenário de demonstração, usabilidade). Hoje nenhum existe. O `UnidadeService` não tem teste unitário. | 🔴 Não atendido | Ver plano de ação (P0/P1). O Marcos (QA) é dono da estratégia de testes da 9.3. |
| D9 | Painel da Liderança (HU08) com "conjunto final de indicadores: decisão em aberto" | A 9.4 já resolve isso: **o painel deve ter uma consulta por métrica (M01–M12)**. | 🟡 Desatualizado | Atualizar T08.1–T08.4 para referenciar explicitamente M01–M12. |
| D10 | Documento de SLA/métricas mantido como rascunho, fora do repositório | O Controle de Revisão tem uma única entrada (07/10, rascunho). O repositório não tem cópia nem link para a aba SLA, então o time técnico não tem fonte versionada. | 🟡 Processo | Versionar a aba SLA (ou um link fixo) em `docs/`, com o Controle de Revisão atualizado a cada mudança. |
| D11 | Reação a sinais da 9.7 não prevista no sistema | A 9.7 define ações por sinal. Destaque: **"M12 maior que zero → bloquear a coleta até corrigir e registrar o incidente"**. M08 > 30% leva a recalibrar as faixas da **Seção 3** com o mentor. M11 > 60 s leva a reduzir campos obrigatórios. | 🟡 Não implementado | O painel (HU08) deve destacar os limiares da 9.7. M12 precisa de alerta e de um procedimento de incidente documentado. |

---

## 3. Rastreabilidade: Requisitos de instrumentação (9.5) → Implementação → Backlog

| Dado a registrar (9.5) | Onde fica | Métricas | Implementado? | Tarefa existente no backlog | Lacuna no backlog |
| :--- | :--- | :--- | :---: | :--- | :--- |
| Data/hora UTC de abertura, primeira resposta e resolução | Chamado | M03, M04 | ❌ | T01.1 (só data de criação), T06.2 (resolução) | **Primeira resposta** não tem tarefa. UTC não está especificado. |
| Tipo, data/hora e autor de **todos** os eventos de domínio (18 eventos, Análise de Domínio §6) | Evento de Histórico | M01–M08 | ❌ | T01.4 (só abertura), T02.1 (estrutura) | Falta uma tarefa transversal: "todo comando de domínio grava seu evento". Enum dos 18 tipos. |
| Motivo de espera, com início e fim | Chamado | M03, M04 | ❌ | — | **Sem tarefa.** Necessário para pausar o relógio de TT/TPR/TR. |
| Prioridade sugerida, prioridade final e justificativa | Chamado | M08 | ❌ | T04.1, T04.2 (só prioridade resultante) | Separar **sugerida** (calculada) de **final** (decisão do analista) + **justificativa** obrigatória quando divergirem. |
| Canal de origem; no Registro de Contato: direção e identificador externo | Chamado + Registro de Contato | M09 | ⚠️ Só no front (canal) | T01.1, T01.2, T13.1 | Direção (recebido/enviado) e identificador externo (RN16) não aparecem nas tarefas. |
| Fornecedor, data de envio e data de retorno | Escalonamento | M05 | ❌ | T07.1, T07.3 | Ok. Pendente a decisão "fornecedor: campo livre × cadastro". |
| Chamado principal e eventos de vínculo/mesclagem | Chamado + Evento | M07 | ❌ | T03.1, T03.2 | **Mesclagem** (RN17, evento `ChamadoMesclado`) não tem tarefa, só vínculo de relato. |
| Cada visualização de anexo (quem e quando) | Evento `AnexoVisualizado` | M12 | ❌ | — | **Não existe HU nem tarefa de anexos/evidências.** M12 tem meta "zero (condição obrigatória)". |
| Abertura e salvamento da tela Novo Chamado | Telemetria da interface | M11 | ❌ | — | **Sem tarefa.** `NovoChamado` não registra o instante em que a tela abre. |
| Marcação de dado real, de teste ou simulado | Chamado ou ambiente separado | Todas | ❌ | — | **Sem tarefa.** Exigida pela 9.1 ("toda medição informa n e tipo de dado"). |
| Contatos fora da plataforma (contagem manual semanal) | Fora da plataforma | M10 | n/a | — | Definir formulário/planilha e o responsável pela consolidação semanal. |

---

## 4. Situação das métricas M01–M12

| ID | Métrica | Depende de (HU / dado 9.5) | Pode ser calculada hoje? | Bloqueio principal |
| :-- | :--- | :--- | :---: | :--- |
| M01 | % de chamados rastreáveis | HU15 + Evento de Histórico | ❌ | Chamado e encerramento inexistentes |
| M02 | Taxa de Chamados Conhecidos (TCD) | HU05/HU12 + último evento ≤ 2h | ❌ | Sem responsável nem eventos |
| M03 | Tempos de atendimento (TT, TPR, TR) | Eventos `ChamadoAberto`, `PrioridadeDefinida`, `ChamadoAtribuido`, `ChamadoResolvido` + motivo de espera | ❌ | Eventos + pausa + **definições da Seção 2** |
| M04 | % dentro da meta | M03 + metas da Seção 4 | ❌ | Metas da Seção 4 + M03 |
| M05 | Tempo em terceiros | HU07 (Escalonamento) | ❌ | HU07 |
| M06 | Taxa de reabertura | `ChamadoReaberto`, `ChamadoResolvido` | ❌ | HU06 + transição Resolvido → Reaberto |
| M07 | Duplicidade tratada | `ChamadoMesclado`, `RelatoVinculado` | ❌ | HU03 + mesclagem (sem tarefa) |
| M08 | Concordância da prioridade | Prioridade sugerida × final + justificativa | ❌ | HU04 + implementar a matriz (faixas e critérios da **Seção 3** do doc SLA) |
| M09 | Adoção por canal | Canal de origem do Chamado | ❌ | Backend da HU01 |
| M10 | Contatos fora da plataforma | Contagem manual | ⚠️ | Instrumento de coleta manual não definido |
| M11 | Tempo de registro manual (≤ 60 s) | Telemetria da tela Novo Chamado | ❌ | Telemetria inexistente |
| M12 | Incidentes de exposição de dados sensíveis | `AnexoVisualizado` + testes de permissão | ❌ | Anexos fora do backlog. **Pela 9.7, M12 > 0 bloqueia a coleta.** |

### Leitura dos resultados (9.7): ações previstas por sinal

| Sinal | Leitura | Ação (9.7) | Pré-requisito técnico |
| :--- | :--- | :--- | :--- |
| M01 ou M02 < 90% | Chamados sem dono ou sem atualização | Revisar campos obrigatórios e destacar na Fila os chamados sem responsável | HU05/HU14 com o filtro "sem responsável" |
| M04 < 80% em uma prioridade | Meta inviável ou capacidade insuficiente | Comparar com M03 e com o volume por unidade antes de recalibrar | Consultas M03/M04 por prioridade e unidade |
| M05 alto em um fornecedor | Gargalo externo | Levar o dado ao cliente para negociar SLA com o fornecedor | HU07 |
| M06 alto | Resolução mal registrada | Revisar o registro de resolução e a notificação | HU06 + `ChamadoReaberto` |
| M08 com divergência > 30% | Matriz pouco aderente | Recalibrar faixas e critérios da Seção 3 com o mentor | Prioridade sugerida × final persistidas |
| M09 concentrado em ligação/presencial ou M10 sem queda | Canais integrados pouco usados | Investigar atrito no chatbot e no e-mail | Canal de origem + contagem M10 |
| M11 > 60 s | Registro lento demais | Reduzir campos obrigatórios ou melhorar o preenchimento | Telemetria da tela Novo Chamado |
| **M12 > 0** | **Falha de privacidade** | **Bloquear a coleta até corrigir e registrar o incidente** | `AnexoVisualizado` + testes de permissão (RN06) |

---

## 5. Testes exigidos pela Seção 9.3 × situação atual

| Tipo (9.3) | O que verifica | Situação | Dono sugerido |
| :--- | :--- | :---: | :--- |
| Testes do cálculo (unitários) | Fórmulas de TT, TPR, TR e % dentro da meta com horários conhecidos, incluindo pausa pelo solicitante, mesclagem, mudança de prioridade e reabertura | ❌ | Elis (BE) + Marcos (QA) |
| Testes dos eventos (integração) | Todo evento gravado com data/hora; mensagens repetidas não duplicam chamados (RN16, RN19) | ❌ | Elis (BE) + Marcos (QA) |
| Testes de permissão e privacidade | Liderança não acessa evidências (RN06); nenhum campo clínico obrigatório; solicitante não vê prazos (RN12) | ❌ | Aguinaldo (Fullstack) + Marcos (QA) |
| Cenário de demonstração | Reproduz a manhã de segunda-feira com dados de semente, **rotulados como simulados** | ❌ | Marcos (QA) + Sarah (Produto) |
| Teste de usabilidade | Cronometra o registro manual com analistas (M11) | ❌ | Sarah (UX) + Pedro (FE) |

---

## 6. Plano de ação priorizado

### P0: antes de qualquer nova funcionalidade (bloqueia a coleta)

> Os itens P0.2 a P0.7 estão detalhados como tarefas prontas para o backlog em [backlog-tarefas-instrumentacao.md](./backlog-tarefas-instrumentacao.md) (BE-01..03, FE-01..03, INT-01).

| # | Ação | Camada | Dono sugerido |
| :-- | :--- | :--- | :--- |
| P0.1 | **Replanejar a janela de coleta** (a semana de 12/10 está perdida) e registrar o ajuste no Controle de Revisão | Produto | Sarah + Luiz |
| P0.2 | Migration `V3`: tabelas `chamado` e `registro_contato` com todos os campos da 9.5 (abertura/primeira resposta/resolução em UTC, motivo de espera + início, prioridade sugerida/final/justificativa, canal, direção + identificador externo, chamado principal, `origem_dado` REAL/TESTE/SIMULADO) (BE-01) | BD | Elis |
| P0.3 | Migration `V4`: tabela `evento_historico` **append-only**, com trigger (`id`, `chamado_id`, `tipo` com os 18 eventos, `ocorrido_em TIMESTAMPTZ`, `autor_id`, `dados JSONB`, `origem_dado`) (BE-02) | BD | Elis |
| P0.4 | Configurar UTC de ponta a ponta (`hibernate.jdbc.time_zone`, `Instant`) (BE-01) | BE | Elis |
| P0.5 | Backend da HU01 (`POST /chamados`) gravando `ChamadoAberto` na **mesma transação** (BE-03). Integração com o front na INT-01. | BE | Elis (BE-03) + Aguinaldo (INT-01) |
| P0.6 | HU05 mínima: status + responsável, com eventos `ChamadoAtribuido` e de mudança de status | BE/FE | Aguinaldo + Pedro |
| P0.7 | Telemetria M11 no `NovoChamado`: guardar o instante em que a tela abre e enviar `registro_iniciado_em` no payload (FE-01) | FE/BE | Pedro |
| P0.8 | Usuários distintos com perfil (HU09 mínima) para que o **autor** dos eventos seja real | BE | Luiz |
| P0.9 | Decidir se o piloto **aceita anexos**. Se aceitar: implementar ocultação, `AnexoVisualizado` e bloqueio da Liderança antes da coleta (M12 > 0 bloqueia a coleta). Se não aceitar: registrar a decisão no Controle de Revisão e na HU01. | Produto/BE | Sarah + Aguinaldo |

### P1: durante o desenvolvimento

| # | Ação | Dono sugerido |
| :-- | :--- | :--- |
| P1.1 | Atualizar o backlog (`Nexo - Historias de Usuario do MVP.md`) com as tarefas faltantes da Seção 3 deste documento | Sarah + Luiz |
| P1.2 | Preencher **Responsável / Estimativa / Prazo** em todas as tarefas (hoje o "T" do SMART não é atendido) | Sarah |
| P1.3 | Testes do cálculo e dos eventos (9.3) junto com cada HU, não depois | Marcos |
| P1.4 | Uma consulta por métrica alimentando o Painel da Liderança (HU08) | Elis + Pedro |
| P1.5 | Atualizar o contrato OpenAPI (v1.1) com os campos da 9.5 | Luiz |
| P1.6 | Criar HU/tarefas de **Evidências/Anexos** com `AnexoVisualizado` e bloqueio para Liderança (RN06/M12) | Sarah + Aguinaldo |

### P2: antes da coleta

| # | Ação | Dono sugerido |
| :-- | :--- | :--- |
| P2.1 | Congelar as definições e as metas iniciais da Seção 9 | Sarah |
| P2.2 | Script de sementes do cenário de demonstração com `origem_dado = SIMULADO` | Marcos |
| P2.3 | Instrumento de contagem manual para M10 + combinado com os analistas | Sarah |
| P2.4 | Roteiro da sessão de usabilidade (M11) | Sarah + Pedro |

---

## 7. Decisões em aberto que bloqueiam métricas

> [!NOTE]
> Pelo Controle de Revisão, o doc SLA tem **Seções 1 a 9**: objetivo, definições, metas, regras do relógio, comportamento na plataforma, calibração, pendências, métricas e plano de coleta. Só a Seção 9 foi analisada aqui. Vários itens marcados como "decisão em aberto" nas HUs **já podem estar definidos** nas Seções 2–5 e 7 e precisam ser levados para o backlog.

| Decisão | Bloqueia | Situação |
| :--- | :--- | :--- |
| Matriz de prioridade (severidade × urgência × impacto) | M08, T04.2 | Provavelmente definida na **Seção 3** do doc SLA ("faixas e critérios"). Levar para a T04.2. |
| Metas por prioridade (Seção 4) e definições de TT/TPR/TR (Seção 2) | M03, M04 | Definidas no doc SLA (rascunho). Congelar antes da coleta (P2.1). |
| Regras do relógio (pausa por motivo de espera, reabertura, mesclagem) | M03, M04, testes de cálculo (9.3) | Provavelmente na Seção 4/5 do doc SLA. **Precisa ser lida antes de modelar `chamado`.** |
| Pendências listadas na seção "pendências" do doc SLA | Diversas | Cruzar com a Seção 4 das HUs ("Pontos a validar"). |
| Fornecedor: campo livre × cadastro | M05, T07.1 | Time |
| Quem reclassifica prioridade | M08, T04.3 | Cliente |
| Canal `OUTRO` entra ou não | M09, HU01 | Produto |
| Pendências no encerramento: bloquear × alertar | M01, T15.2 | Produto |
| Marcação de origem do dado: campo no Chamado × ambiente separado | Todas (9.1) | Time |

---

## 8. Observações sobre o processo do time (git log e repositório)

* **Concentração de código:** os commits de código estão em Luiz, Elis e Pedro. O Marcos (QA/DevOps) só aparece em merges, e o Aguinaldo (Fullstack) ainda não tem commits. Com a 9.3 exigindo bastante teste, o papel de QA precisa aparecer no código.
* **Nome das branches:** há padrões misturados (`feat/HU01-HU11-…`, `feat/US-11-…`, `docs/US-01-…`). Sugestão: usar só `HUxx`, como nos documentos de requisitos.
* **README desatualizado:** cita arquivos que não existem mais (`01-analise-de-dominio.md`, `04-historias-de-usuario.md`, `05-proximos-passos.md`, `ApiApplication.java`) e mistura os nomes AvanDesk-AI e Nexo.
* **V1 vazia:** a `V1__init_schema.sql` não tem DDL e cita uma "US-007" que não existe na numeração atual.

---

## 9. Próximos passos desta avaliação

- [ ] Analisar as Seções 1–8 do doc SLA (definições, metas, regras do relógio, calibração, pendências) e atualizar esta avaliação
- [ ] Validar este diagnóstico com o time (daily ou refinamento)
- [ ] Sarah: replanejar a coleta (P0.1) e atualizar o backlog (P1.1, P1.2)
- [ ] Abrir issues/cards para cada item P0
- [ ] Reavaliar a aderência à Seção 9 ao fim da próxima sprint
