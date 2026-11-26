# Histórias de Usuário do MVP — Nexo

Rede Lúmen Diagnósticos

## 1. Visão geral

Este documento consolida as histórias de usuário propostas para o MVP do Nexo. As histórias HU01 a HU08 mantêm a numeração original; as histórias HU09 a HU15 foram adicionadas para cobrir lacunas necessárias ao funcionamento completo do ciclo Registrar → Triar/Acompanhar → Resolver/Escalar. As histórias HU16 a HU26 foram incluídas depois do MVP inicial (pós-MVP e evoluções), conforme o quadro do Trello em 25/11/2026.

| ID | História | Situação |
| :---- | :---- | :---- |
| HU01 | Registrar um chamado | Existente (ajustada) |
| HU02 | Consultar e manter o histórico do chamado | Existente |
| HU03 | Identificar relatos duplicados ou relacionados | Existente |
| HU04 | Definir a prioridade do chamado | Existente |
| HU05 | Acompanhar o status e responsável pelo chamado | Existente |
| HU06 | Registrar resolução e causa raiz | Existente |
| HU07 | Escalar chamado para fornecedor externo | Existente (ajustada) |
| HU08 | Visualizar a situação dos chamados | Existente |
| HU09 | Autenticar no sistema | Nova |
| HU10 | Gerenciar analistas | Nova |
| HU11 | Cadastrar unidades | Nova |
| HU12 | Atribuir responsável e atualizar status | Nova |
| HU13 | Registrar novo contato em chamado existente | Nova |
| HU14 | Listar e buscar chamados | Nova |
| HU15 | Encerrar chamado com verificação de rastreabilidade | Nova |
| HU16 | Obrigatoriedade de e-mail e notificações automáticas de status | Nova (pós-MVP) |
| HU17 | Cadastro de conta do Solicitante e verificação por e-mail | Nova (pós-MVP) |
| HU18 | Módulo Web do Solicitante (/solicitante/**) e Chat Assíncrono em Texto (via HTTP) | Nova (pós-MVP) |
| HU19 | Prevenção de Race Condition, justificativa obrigatória em reatribuição e checkbox "Atribuir a mim" desmarcado | Nova (refina a HU12) |
| HU20 | Visão operacional da unidade e reatribuição auditada pelo Gestor de Unidade | Nova (pós-MVP) |
| HU21 | Upload e visualização segura de anexos essenciais (Fotos e Documentos) ponta a ponta | Nova (pós-MVP) |
| HU22 | Questionário de negócio na abertura e validação da Matriz 3D pelo analista | Nova (refina a HU04) |
| HU23 | Painel de Métricas e SLA (Gráficos de Barras, Rosca e Indicadores) para Liderança e Gestor de Unidade | Nova (pós-MVP) |
| HU24 | (Extra/Evolução) Exportação estruturada de dados para ferramentas de BI | Nova (pós-MVP) |
| HU25 | Atualização em tempo real de fila, responsáveis, status e alertas P1 via WebSocket | Nova (pós-MVP) |
| HU26 | Refatorar autenticação JWT para armazenamento em Cookies HttpOnly | Nova (segurança, refina a HU09) |

## 2. Histórias de usuário

### HU01 - Registrar um chamado

**Como** analista de suporte, **quero** registrar uma solicitação recebida por um canal existente como um chamado estruturado, **para** que o problema possa ser acompanhado e rastreado.

**Critérios de aceite**

* O chamado deve estar associado a exatamente uma unidade.
* Deve ser possível registrar informações mínimas sobre o problema.
* O chamado deve possuir um identificador único.
* O registro não deve exigir informações clínicas completas ou resultados de exames.
* O chamado deve iniciar seu ciclo de vida a partir do registro.
* O registro deve permitir identificar o solicitante e o contexto disponível da solicitação.
* O registro deve indicar o canal de origem da solicitação (WhatsApp, ligação, e-mail ou presencial).

### HU02 - Consultar e manter o histórico do chamado

**Como** analista de suporte, **quero** visualizar o histórico completo de um chamado, **para** entender o que já aconteceu e continuar o atendimento sem depender de mensagens espalhadas em diferentes canais.

**Critérios de aceite**

* O histórico deve reunir os registros de contato associados ao chamado.
* Deve ser possível identificar os acontecimentos relevantes do atendimento.
* Deve ser possível visualizar o responsável atual.
* As atualizações devem permanecer associadas ao mesmo chamado.
* Um analista diferente daquele que iniciou o atendimento deve conseguir compreender o histórico.

### HU03 - Identificar relatos duplicados ou relacionados

**Como** analista de suporte, **quero** associar diferentes relatos ao mesmo chamado quando eles se referirem ao mesmo problema, **para** evitar que a equipe trate o mesmo incidente como vários atendimentos independentes.

**Critérios de aceite**

* Deve ser possível associar mais de um Registro de Contato a um Chamado.
* O analista deve conseguir identificar que dois ou mais relatos estão relacionados.
* Relatos associados devem permanecer rastreáveis dentro do chamado.
* A associação não deve apagar o registro original do contato.

### HU04 - Definir a prioridade do chamado

**Como** analista de suporte, **quero** avaliar severidade, urgência e impacto operacional, **para** determinar a prioridade de atendimento de forma estruturada.

**Critérios de aceite**

* Severidade e urgência devem ser avaliadas separadamente.
* O impacto operacional deve fazer parte da avaliação.
* A prioridade final deve resultar desses critérios.
* O solicitante ou gestor não deve conseguir definir livremente a prioridade final.
* A prioridade deve estar registrada no chamado.

### HU05 - Acompanhar o status e responsável pelo chamado

**Como** analista de suporte, **quero** visualizar o status e o responsável de cada chamado, **para** saber quais atendimentos estão em andamento e quem está responsável por cada um.

**Critérios de aceite**

* Deve existir um status atual para cada chamado.
* Deve ser possível identificar o analista responsável, quando houver.
* O chamado deve permitir acompanhamento de seu ciclo de vida.
* O status deve respeitar as transições definidas pelo domínio.
* Um chamado resolvido só poderá retornar para o estado Reaberto, não diretamente para Em Triagem.

Observação: a HU05 trata da visualização; a ação de atribuir e alterar status está na HU12.

### HU06 - Registrar resolução e causa raiz

**Como** analista de suporte, **quero** registrar como o chamado foi resolvido e, quando identificada, sua causa raiz, **para** manter o conhecimento do atendimento e reconhecer problemas recorrentes.

**Critérios de aceite**

* Deve ser possível registrar a resolução do chamado.
* Deve ser possível registrar a causa raiz quando ela for identificada.
* O chamado deve permanecer com seu histórico após a resolução.
* Deve ser possível distinguir um atendimento pontual de uma causa recorrente quando essa relação for identificada.
* O registro da causa raiz não deve ser obrigatório quando ela ainda não tiver sido identificada.

### HU07 - Escalar chamado para fornecedor externo

**Como** analista de suporte, **quero** encaminhar um chamado para um fornecedor externo quando a resolução depender dele, mantendo o acompanhamento interno, **para** que o atendimento não perca sua rastreabilidade.

**Critérios de aceite**

* Deve ser possível indicar que o chamado depende de fornecedor externo.
* O chamado deve permanecer registrado no sistema durante o escalonamento.
* Deve ser possível identificar que o atendimento foi encaminhado ao fornecedor.
* O histórico anterior deve permanecer disponível.
* O time interno deve continuar identificável como responsável pelo acompanhamento.
* Deve ser possível identificar qual fornecedor recebeu o chamado. **(novo)**
* Deve ser possível registrar o retorno do fornecedor e o fim do escalonamento. **(novo)**

### HU08 - Visualizar a situação dos chamados

**Como** liderança de TI, **quero** visualizar os chamados em aberto, seus responsáveis e informações agregadas da operação, **para** acompanhar a situação do suporte e identificar padrões de atendimento.

**Critérios de aceite**

* Deve ser possível visualizar chamados em aberto.
* Deve ser possível identificar seus responsáveis.
* Deve ser possível consultar informações agregadas por unidade.
* Deve ser possível identificar problemas recorrentes registrados.
* As informações devem ser baseadas nos chamados efetivamente registrados na plataforma.

### HU09 - Autenticar no sistema

**Como** analista de suporte ou liderança de TI, **quero** acessar a plataforma com minhas credenciais, **para** utilizar apenas as funcionalidades permitidas ao meu perfil.

**Critérios de aceite**

* Deve ser possível entrar com e-mail e senha.
* Cada usuário deve possuir um perfil: Analista de Suporte ou Liderança de TI.
* Usuários não autenticados não devem acessar nenhuma tela de chamados.
* Credenciais inválidas devem exibir mensagem genérica, sem revelar se o e-mail existe.
* Deve ser possível encerrar a sessão (logout).
* As ações realizadas no sistema devem ficar associadas ao usuário autenticado.

### HU10 - Gerenciar analistas

**Como** liderança de TI, **quero** cadastrar, editar e desativar analistas de suporte, **para** controlar quem pode atuar nos chamados.

**Critérios de aceite**

* Deve ser possível cadastrar um analista com nome, e-mail e perfil.
* Deve ser possível editar os dados de um analista.
* Deve ser possível desativar um analista sem apagar seu histórico de atuação.
* Um analista desativado não deve conseguir acessar o sistema nem receber novos chamados.
* O e-mail deve ser único na plataforma.

### HU11 - Cadastrar unidades

**Como** liderança de TI, **quero** manter o cadastro das unidades da Rede Lúmen, **para** que todo chamado seja associado a uma unidade válida.

**Critérios de aceite**

* Deve ser possível cadastrar uma unidade com nome e identificação.
* Deve ser possível editar e desativar unidades.
* Unidades desativadas não devem aparecer na abertura de novos chamados.
* Chamados já registrados devem manter a associação com a unidade, mesmo que ela seja desativada.

### HU12 - Atribuir responsável e atualizar status

**Como** analista de suporte, **quero** assumir, atribuir ou reatribuir um chamado e atualizar seu status, **para** que fique claro quem está conduzindo cada atendimento e em que etapa ele está.

**Critérios de aceite**

* Deve ser possível assumir um chamado para si.
* Deve ser possível reatribuir um chamado a outro analista ativo.
* Deve ser possível alterar o status apenas por transições válidas do ciclo de vida.
* Toda atribuição e mudança de status deve ser registrada no histórico com data e autor.
* Transições inválidas devem ser bloqueadas com mensagem explicativa.

### HU13 - Registrar novo contato em chamado existente

**Como** analista de suporte, **quero** registrar um novo contato recebido sobre um chamado já aberto, **para** manter todas as interações concentradas no mesmo atendimento.

**Critérios de aceite**

* Deve ser possível adicionar um Registro de Contato a um chamado existente.
* O registro deve indicar canal (WhatsApp, telefone, e-mail, presencial, outro), data e quem relatou.
* O novo contato deve aparecer no histórico do chamado.
* Não deve ser permitido incluir informações clínicas completas ou resultados de exames.

### HU14 - Listar e buscar chamados

**Como** analista de suporte, **quero** listar e buscar chamados com filtros, **para** encontrar rapidamente os atendimentos que preciso tratar ou consultar.

**Critérios de aceite**

* Deve ser possível listar chamados exibindo identificador, unidade, status, prioridade e responsável.
* Deve ser possível filtrar por status, unidade, prioridade e responsável.
* Deve ser possível buscar pelo identificador único.
* Deve ser possível visualizar apenas os chamados atribuídos a mim.
* A partir da lista, deve ser possível abrir o detalhe e o histórico do chamado.

Observação: esta história também viabiliza a HU03, pois o analista precisa encontrar o chamado existente para vincular um novo relato.

### HU15 - Encerrar chamado com verificação de rastreabilidade

**Como** analista de suporte, **quero** encerrar um chamado resolvido e ser informado se o registro está completo, **para** garantir que o atendimento fique rastreável para consultas futuras.

**Critérios de aceite**

* Só deve ser possível encerrar um chamado que esteja no status Resolvido.
* Ao encerrar, a plataforma deve verificar os elementos mínimos: problema registrado, unidade, responsável, ao menos um contato, evolução de status e resolução.
* Caso falte algum elemento, a plataforma deve indicar o que está pendente.
* O encerramento deve ser registrado no histórico.
* O resultado da verificação deve ser armazenado para alimentar a métrica de chamados rastreáveis.

### HU16 - Obrigatoriedade de e-mail e notificações automáticas de status

**Como** solicitante de um chamado, **quero** receber e-mails automáticos com o código do chamado e as mudanças de status, **para** acompanhar o atendimento sem precisar consultar a equipe de suporte.

**Critérios de aceite**

* O campo de e-mail do solicitante passa a ser obrigatório e validado (formato RFC e conversão para minúsculas) na criação do chamado.
* Ao criar o chamado, o solicitante recebe um e-mail com o código único (`LUM-AAAA-NNNNNN`) e resumo da solicitação.
* A cada mudança de status relevante, um e-mail assíncrono é disparado informando o status anterior, o novo status e orientações cabíveis.
* Falhas no servidor de e-mail (SMTP indisponível) **não** devem cancelar (dar rollback) na mudança de status do chamado, devendo registrar log/tentativa de reenvio.
* Todo envio bem-sucedido grava o evento `NOTIFICACAO_ENVIADA` em `eventos_historico`.

### HU17 - Cadastro de conta do Solicitante e verificação por e-mail

**Como** colaborador de uma unidade (`SOLICITANTE`), **quero** criar minha conta informando meu e-mail (seja corporativo `@lumen` ou pessoal como Gmail/Outlook, conforme política configurada) e confirmá-lo por link/código de verificação, **para** acessar meus chamados via Web com segurança e ter meu histórico vinculado automaticamente.

> **Política Flexível de Domínio de E-mail (`@lumen` vs. E-mail Aberto):**
> No contexto hospitalar, recepcionistas ou técnicos de turno muitas vezes não possuem caixa de e-mail corporativa individual (usando Gmail/Outlook), enquanto para Analistas (`HU10`) exigimos domínio corporativo. Por isso, implementamos uma **propriedade configurável** (`nexo.solicitantes.restringir-dominio-email: false` por padrão, com whitelist `nexo.solicitantes.dominios-permitidos: "@lumen.com.br"`). Assim, hoje aceitamos qualquer provedor válido mediante verificação de propriedade do e-mail (Token/OTP), mas podemos ativar a trava exclusiva `@lumen` via variável de ambiente sem alterar código!

**Critérios de aceite**

* Deve existir o perfil `SOLICITANTE` na estrutura de controle de acesso.
* O auto-cadastro exige nome, e-mail válido (aceitando qualquer provedor por padrão ou restringindo a `@lumen` via flag de configuração), unidade padrão e senha forte.
* A conta nasce com status `PENDENTE_VERIFICACAO` e não permite login completo até a confirmação do token enviado por e-mail (expiração de 24h).
* Ao confirmar o e-mail, chamados anteriores abertos pelo Analista com aquele mesmo endereço de e-mail são vinculados automaticamente ao `solicitante_id` do usuário.

### HU18 - Módulo Web do Solicitante (/solicitante/**) e Chat Assíncrono em Texto (via HTTP)

**Como** solicitante autenticado na aplicação Web (desktop ou navegador mobile), **quero** acessar meu módulo dedicado ("Meus Chamados") e conversar por mensagens de texto com o analista na thread do chamado, **para** tirar dúvidas, acrescentar contexto textual ao problema e acompanhar a resolução sem abrir tickets duplicados.

> **Decisão de Arquitetura Pragmática (100% Web + Chat Texto via HTTP REST):**
> - **Módulo e Namespace Semântico:** Isolamos as rotas de API e Frontend do solicitante sob `/api/v1/solicitante/**` (no Backend) e `/solicitante/**` (módulo lazy-loaded no Angular), facilitando regras de segurança (`hasRole('SOLICITANTE')`) e separação de contexto.
> - **Apenas Texto no Chat (Evidências/Arquivos ficam na Abertura):** A thread de conversa trafega exclusivamente mensagens de texto (`registros_contato` com direção `RECEBIDO` / `ENVIADO`), mantendo os uploads de fotos/documentos concentrados no contexto inicial do problema (`HU21`).
> - **Transporte HTTP + Polling Leve (15s):** Operamos via HTTP REST padrão com atualização automática (`visibilitychange` + *polling* de 15s quando a aba Web está aberta) e aviso por e-mail com *deep link* quando a aba está fechada, deixando o plug com WebSocket (`HU25`) como evolução futura transparente.

**Critérios de aceite**

* O perfil `SOLICITANTE` acessa exclusivamente o módulo Web `/solicitante/**` ("Abrir Chamado" e "Meus Chamados"), com isolamento estrito no Backend (`/api/v1/solicitante/**`).
* A tela de detalhe exibe uma **Thread de Conversação (UX de Chat somente texto)** entre Solicitante e Analista, listando as mensagens públicas (`RECEBIDO` para o solicitante e `ENVIADO` para o analista), mantendo notas técnicas internas ocultas para a TI.
* Não há envio de novos arquivos/fotos dentro do chat; o foco da thread é o diálogo e contexto textual complementar sobre o problema.
* Quando o analista envia uma mensagem para o solicitante, o sistema dispara e-mail com link direto para o chamado na Web.
* Se o chamado estiver em `AGUARDANDO` (com motivo `SOLICITANTE`), a resposta do solicitante na thread registra `RETORNO_RECEBIDO` e sinaliza o analista.
* Se o chamado estiver em `RESOLVIDO` dentro do prazo de 7 dias (RN20), o solicitante pode confirmar a solução (movendo para `FECHADO`) ou responder na thread que o problema persiste (movendo para `REABERTO`).

### HU19 - Prevenção de Race Condition, justificativa obrigatória em reatribuição e checkbox "Atribuir a mim" desmarcado

**Como** analista de suporte, **quero** que o sistema impeça colisões simultâneas ao assumir um chamado, inicie com a opção "Atribuir a mim" desmarcada na criação e exija justificativa sempre que um chamado já atribuído trocar de responsável, **para** garantir governança, auditoria completa e evitar retenção acidental na fila.

**Critérios de aceite**

* Na tela de "Novo Chamado" do Analista, o checkbox *"Atribuir chamado a mim"* deve iniciar **desmarcado (`false`) por padrão**.
* **Primeira atribuição (`responsavel_id == null`):** caso dois ou mais analistas cliquem em "Assumir chamado" simultaneamente, apenas a primeira transação é efetivada (`200 OK`) e a segunda recebe `409 Conflict` informando quem assumiu.
* **Reatribuição (`responsavel_id != null`):** sempre que um analista (ou gestor na `HU20`) assumir ou transferir um chamado que **já possui um analista atribuído**, o sistema exige **justificativa obrigatória** (mín. 15 caracteres), gravando o responsável anterior, o novo responsável e a justificativa em `eventos_historico` (`CHAMADO_REATRIBUIDO`).
* Mutações concorrentes de status, prioridade, reatribuição ou mesclagem no mesmo chamado são protegidas por controle de concorrência otimista (`@Version`).

### HU20 - Visão operacional da unidade e reatribuição auditada pelo Gestor de Unidade

**Como** gestor de unidade, **quero** visualizar o andamento, métricas de prazo (SLA/referência) e chamados da minha unidade, além de poder reatribuir o analista de um chamado da minha unidade mediante justificativa obrigatória, **para** acompanhar o desempenho local e destravar atendimentos críticos com total rastreabilidade.

> **Regra Unificada de Auditoria em Reatribuição:**
> Seguindo a decisão de negócio alinhada na `HU19`, **tanto o Analista quanto o Gestor de Unidade** podem reatribuir um chamado que já possui responsável, mas **em ambos os casos a justificativa é estritamente obrigatória** e fica gravada de forma imutável no `EventoHistorico` com o autor da ação, o analista anterior e o novo analista.

**Critérios de aceite**

* Novo perfil `GESTOR_UNIDADE` vinculado obrigatoriamente a uma ou mais unidades (`unidade_id`).
* O Gestor de Unidade visualiza no painel e na listagem **exclusivamente** os chamados e indicadores da(s) unidade(s) sob sua gestão.
* O Gestor de Unidade pode visualizar chamados com prazo de referência estourado e o percentual resolvido dentro da meta na sua unidade.
* Ao reatribuir um chamado da sua unidade de um analista para outro, o sistema exige justificativa obrigatória (`>= 15 caracteres`) e grava evento auditável `CHAMADO_REATRIBUIDO` no histórico.
* O Gestor de Unidade **não** tem acesso a anexos/evidências sensíveis (mantendo a proteção LGPD da RN06).

### HU21 - Upload e visualização segura de anexos essenciais (Fotos e Documentos) ponta a ponta

**Como** solicitante ou analista de suporte, **quero** anexar arquivos essenciais (fotos e documentos como PDF, Excel, JPG, PNG e WebP) no contexto do chamado, com validação binária (*Magic Bytes*) e auditoria de visualização, **para** comprovar o problema técnico com segurança e conformidade LGPD (RN01 e RN06).

**Critérios de aceite**

* Permite upload apenas de formatos essenciais de imagens e documentos corporativos:
  * **Imagens/Fotos:** `JPG/JPEG` (`image/jpeg`), `PNG` (`image/png`), `WebP` (`image/webp`).
  * **Documentos/Planilhas:** `PDF` (`application/pdf`), `Excel` (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` - `.xlsx`, `application/vnd.ms-excel` - `.xls`).
* Limite máximo de tamanho por arquivo (ex.: 5MB) e quantidade máxima por chamado (ex.: até 5 anexos).
* O Backend valida não apenas a extensão do nome do arquivo, mas a **assinatura binária real (*Magic Bytes*)** para impedir executáveis ou scripts maliciosos disfarçados (`.exe`, `.bat`, `.sh` renomeados).
* Exibe alerta claro de LGPD antes do upload orientando a não enviar laudos, resultados de exames ou dados clínicos de pacientes.
* Os anexos ficam ocultos por padrão na tela de detalhe (exigem clique intencional em "Visualizar Evidência").
* Perfis `LIDERANCA_TI` e `GESTOR_UNIDADE` são bloqueados (`403 Forbidden`) ao tentar baixar ou visualizar anexos (RN06); apenas `ANALISTA_SUPORTE` (e o próprio `SOLICITANTE` autor do chamado) podem acessá-los.
* Cada visualização ou download grava na mesma transação o evento imutável `ANEXO_VISUALIZADO` em `eventos_historico` (Métrica M12).

### HU22 - Questionário de negócio na abertura e validação da Matriz 3D pelo analista

**Como** solicitante na abertura do chamado (e analista na triagem), **quero** responder a perguntas objetivas do negócio que alimentem automaticamente os eixos de Severidade, Urgência e Impacto da Matriz 3D, **para** que o chamado já chegue na fila com uma prioridade sugerida (`P1`, `P2` ou `P3`) justa e pronta para validação técnica do analista.

**Mapeamento das Perguntas de Negócio para os 3 Eixos (1 a 3):**
1. **Pergunta de Severidade (*Quão quebrado está?*):**
   - *(Nota 1)* "Tenho uma dúvida ou lentidão, mas consigo trabalhar com alternativa."
   - *(Nota 2)* "Uma função específica parou sem alternativa, mas o restante do sistema funciona."
   - *(Nota 3)* "O sistema ou equipamento está totalmente fora do ar / inacessível."
2. **Pergunta de Urgência (*Quão rápido precisa?*):**
   - *(Nota 1)* "Pode aguardar; nenhum atendimento ou rotina imediata foi interrompido."
   - *(Nota 2)* "Afeta atividades administrativas/operacionais do dia, mas não há paciente esperando agora."
   - *(Nota 3)* "Há paciente aguardando presencialmente na recepção/sala de exame neste momento."
3. **Pergunta de Impacto (*Quantos são afetados?*):**
   - *(Nota 1)* "Apenas o meu computador / meu usuário."
   - *(Nota 2)* "Várias pessoas do meu setor ou recepção."
   - *(Nota 3)* "A unidade inteira está parada (ou mais de uma unidade)."

**Critérios de aceite**

* Na abertura do chamado, o solicitante responde às 3 perguntas objetivas em linguagem de negócio (sem jargão técnico).
* O Backend traduz as respostas nas notas `(Severidade, Urgência, Impacto)` de 1 a 3 e calcula a **Prioridade Sugerida** (`P1 - Crítica/Alta`, `P2 - Média`, `P3 - Baixa`) já no ato da criação do chamado.
* Na tela de triagem do analista, as respostas dadas pelo solicitante e a Prioridade Sugerida são exibidas de forma clara.
* O analista pode **Confirmar a prioridade sugerida** com 1 clique ou **Reclassificar** ajustando os eixos da Matriz 3D / exigindo justificativa obrigatória (`>= 15 caracteres`) caso divirja da matriz, alimentando a métrica M08 (Taxa de Concordância).

### HU23 - Painel de Métricas e SLA (Gráficos de Barras, Rosca e Indicadores) para Liderança e Gestor de Unidade

**Como** liderança de TI ou gestor de unidade, **quero** visualizar um dashboard interativo com gráfico de barras (volume e incidência), gráfico de rosca (distribuição P1, P2, P3) e indicadores de cumprimento de prazo (chamados estourados e % resolvido no prazo), **para** tomar decisões baseadas em dados reais sobre a operação.

**Critérios de aceite**

* **Segregação de Escopo:** `LIDERANCA_TI` visualiza todas as unidades (com filtro por unidade, período e categoria); `GESTOR_UNIDADE` visualiza apenas os dados da sua unidade.
* **Cards de KPI no Topo:** Total em aberto, % resolvidos dentro do prazo de referência, total de chamados com prazo estourado, Tempo Médio de Primeira Resposta (M03) e Tempo Médio de Resolução (M04).
* **Gráfico de Barras:** Volume e incidência de chamados agrupados por período (diário/semanal/mensal), unidade ou categoria.
* **Gráfico de Rosca/Pizza:** Distribuição percentual e absoluta das prioridades ativas (`P1`, `P2`, `P3` e `Sem Prioridade`).
* Filtra estritamente registros com `origem_dado = 'REAL'` por padrão.

### HU24 - (Extra/Evolução) Exportação estruturada de dados para ferramentas de BI

**Como** liderança de TI, **quero** exportar os dados consolidados de chamados e métricas em formato estruturado (CSV/JSON), anonimizados conforme a LGPD, **para** alimentar painéis corporativos externos de Business Intelligence (Power BI, Looker, Metabase).

**Critérios de aceite**

* Permite exportar dados filtrados por intervalo de datas e unidade em formato `CSV` (compatível com Excel/Power BI) ou `JSON`.
* Nenhum dado pessoal sensível ou texto livre de descrição/anexo que possa conter dados clínicos é exposto na exportação analítica de BI (exporta apenas códigos, dimensões de unidade, categoria, prioridades, status, timestamps UTC e métricas calculadas).

### HU25 - Atualização em tempo real de fila, responsáveis, status e alertas P1 via WebSocket

**Como** analista de suporte, **quero** que a fila de chamados e a tela de detalhe atualizem instantaneamente quando novos chamados entrarem ou quando outro analista assumir/mudar o status de um chamado, **para** eliminar a necessidade de recarregar a página (F5), responder imediatamente a incidentes P1 e evitar colisões de atendimento.

**Critérios de aceite**

* Novos chamados abertos aparecem no topo da fila dos analistas conectados em tempo real (sem F5).
* Se o novo chamado tiver prioridade sugerida `P1`, um alerta visual (e sonoro opcional) destaca a chegada imediata na tela do suporte.
* Quando um analista assume um chamado ou altera seu status, o card correspondente na fila e a tela de detalhe de todos os outros analistas conectados são atualizados em `< 1 segundo`.
* Se a conexão WebSocket cair (oscilação de rede na unidade), o cliente Frontend reconecta automaticamente com *exponential backoff*.

### HU26 - Refatorar autenticação JWT para armazenamento em Cookies HttpOnly

**Como** usuário da plataforma (Analista, Solicitante, Gestor ou Liderança), **quero** que minha sessão autenticada (JWT) seja armazenada em um Cookie `HttpOnly`, `Secure` e `SameSite` gerenciado pelo servidor em vez do `localStorage`, **para** impedir que scripts maliciosos (ataques XSS) possam ler ou exfiltrar meu token de acesso em um ambiente clínico regulado pela LGPD.

**Critérios de aceite**

* No `POST /api/v1/auth/login`, o Backend emite o JWT dentro do cabeçalho `Set-Cookie` com as flags `HttpOnly`, `Secure` (em HTTPS/Prod), `SameSite=Strict` (ou `Lax`) e `Path=/api`, deixando de expor o `accessToken` bruto no corpo JSON para armazenamento em `localStorage`.
* O `JwtAuthenticationFilter` do Spring Security passa a extrair o token prioritariamente do cookie `nexo_access_token` (mantendo fallback opcional para header `Authorization` apenas se necessário para ferramentas de API/Swagger).
* O Frontend Angular remove a gravação de tokens em `localStorage`/`sessionStorage`, passa a enviar requisições com `withCredentials: true` no `HttpClient` e consulta `/api/v1/auth/me` (ou dados não-sensíveis do perfil retornados no login) para hidratar o estado do usuário na UI.
* O `POST /api/v1/auth/logout` invalida a sessão no navegador sobrescrevendo o cookie `HttpOnly` com `Max-Age=0`.
* Proteção contra CSRF ativa e configurada (`SameSite=Strict/Lax` + `CookieCsrfTokenRepository` ou validação de header customizado em mutações).

## 3. Dependências entre as histórias

| História | Depende de |
| :---- | :---- |
| HU09 Autenticação | — |
| HU10 Analistas | HU09 |
| HU11 Unidades | HU09 |
| HU01 Registrar chamado | HU09 + HU11 |
| HU02 Histórico | HU01 |
| HU13 Novo contato | HU01 + HU02 |
| HU14 Listar e buscar | HU01 |
| HU03 Relacionar relatos | HU13 + HU14 |
| HU04 Prioridade | HU01 |
| HU05 Visualizar status e responsável | HU01 |
| HU12 Atribuir e atualizar status | HU05 + HU10 |
| HU06 Resolução e causa raiz | HU12 |
| HU07 Escalonamento | HU12 |
| HU15 Encerramento | HU06 |
| HU08 Visão operacional | HU04 + HU12 + HU06 |
| HU16 Obrigatoriedade de e-mail e notificações automáticas de status | HU01 + HU12 |
| HU17 Cadastro de conta do Solicitante e verificação por e-mail | HU09 + HU16 |
| HU18 Módulo Web do Solicitante (/solicitante/**) e Chat Assíncrono em Texto (via HTTP) | HU13 + HU17 |
| HU19 Prevenção de Race Condition, justificativa obrigatória em reatribuição e checkbox "Atribuir a mim" desmarcado | HU12 |
| HU20 Visão operacional da unidade e reatribuição auditada pelo Gestor de Unidade | HU08 + HU19 |
| HU21 Upload e visualização segura de anexos essenciais (Fotos e Documentos) ponta a ponta | HU01 |
| HU22 Questionário de negócio na abertura e validação da Matriz 3D pelo analista | HU01 + HU04 |
| HU23 Painel de Métricas e SLA (Gráficos de Barras, Rosca e Indicadores) para Liderança e Gestor de Unidade | HU08 |
| HU24 (Extra/Evolução) Exportação estruturada de dados para ferramentas de BI | HU23 |
| HU25 Atualização em tempo real de fila, responsáveis, status e alertas P1 via WebSocket | HU12 + HU14 |
| HU26 Refatorar autenticação JWT para armazenamento em Cookies HttpOnly | HU09 |

Algumas histórias podem ser desenvolvidas em paralelo, desde que suas dependências estejam atendidas.

As dependências das histórias HU16 a HU26 foram inferidas a partir dos cartões do Trello e devem ser confirmadas pelo time.

## 4. Pontos a validar

* Confirmar se a Liderança de TI é o perfil responsável pelo cadastro de analistas e unidades (HU10 e HU11) ou se existirá um perfil administrador.
* Definir se os fornecedores externos serão um campo livre ou um cadastro próprio (o que geraria uma história adicional).
* Definir quem possui autoridade para reclassificar a prioridade (HU04).

**Atualização de 25/11/2026**

* Anexos: decidido formalizar como HU21 (pós-MVP), em substituição à trilha T-PRIV como tarefa transversal.
* Reclassificação de prioridade: tratada na HU22. O analista valida os eixos da Matriz 3D e, se a prioridade final divergir da sugerida na abertura, informa justificativa com no mínimo 15 caracteres.
* Perfil responsável por cadastros: a HU20 cria o perfil GESTOR_UNIDADE. A existência de um perfil ADMIN para analistas e unidades segue em aberto.
* Fornecedor externo como cadastro próprio: segue em aberto e está no quadro como "Gerenciar fornecedores externos" (Possíveis Entregas).

## 5. Tarefas SMART por camada

As tarefas abaixo estão descritas de forma independente de tecnologia. Escolhas de linguagem, framework, banco de dados, formato de API, responsáveis, estimativas e prazos ficam em aberto para definição posterior pelo time.

**Como cada tarefa atende ao SMART**

| Critério | Onde está na tarefa |
| :---- | :---- |
| **S**pecific (Específica) | Coluna "Tarefa": descreve uma entrega única e delimitada. |
| **M**easurable (Mensurável) | Coluna "Critério de conclusão": condição verificável para considerar a tarefa pronta. |
| **A**chievable (Atingível) | Cada tarefa se limita a uma única camada e a uma parte da HU. |
| **R**elevant (Relevante) | Cada tarefa está vinculada a uma HU do MVP. |
| **T**ime-bound (Temporal) | Colunas "Estimativa" e "Prazo": a preencher pelo time. |

**Legenda de camadas:** BD = Banco de Dados · BE = Backend · FE = Frontend · QA = Qualidade (as histórias HU16 a HU26 trazem tarefas de QA)

Itens marcados com **(decisão em aberto)** dependem de uma definição do time ou do cliente antes da execução.

### HU01 - Registrar um chamado

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T01.1 | BD | Modelar a estrutura de dados do Chamado (identificador único, unidade, solicitante, descrição do problema, canal de origem, status, data de criação e autor). | Estrutura criada e versionada no repositório; unidade obrigatória e identificador único garantidos por restrição no banco. | | | |
| T01.2 | BD | Modelar a estrutura do Registro de Contato vinculada ao Chamado. | Estrutura criada; todo Registro de Contato exige vínculo com um chamado existente. | | | |
| T01.3 | BE | Implementar a operação de registro de chamado com validação dos campos obrigatórios. | Operação retorna o identificador único; rejeita requisições sem unidade ou com unidade inativa; testes automatizados cobrem cenários de sucesso e erro. | | | |
| T01.4 | BE | Registrar automaticamente o evento de abertura no histórico do chamado. | Todo chamado criado possui evento de abertura com data e autor, comprovado por teste automatizado. | | | |
| T01.5 | FE | Criar o formulário de registro de chamado. | Formulário exibe os campos definidos, lista apenas unidades ativas, bloqueia envio com campos obrigatórios vazios e exibe o identificador após sucesso. | | | |
| T01.6 | FE | Exibir orientação sobre a não inclusão de dados clínicos e resultados de exames. | Aviso visível no formulário de registro. | | | |

### HU02 - Consultar e manter o histórico do chamado

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T02.1 | BD | Modelar a estrutura de eventos do histórico (chamado, tipo de evento, descrição, data e autor). | Estrutura criada; eventos registrados não podem ser alterados nem excluídos. | | | |
| T02.2 | BE | Implementar a operação de consulta do detalhe do chamado com seu histórico. | Operação retorna dados do chamado, responsável atual, contatos e eventos em ordem cronológica; testes automatizados cobrem a consulta. | | | |
| T02.3 | BE | Implementar a operação de inclusão de atualização (anotação) em um chamado. | Atualização fica associada ao chamado e aparece no histórico com data e autor. | | | |
| T02.4 | FE | Criar a tela de detalhe do chamado com linha do tempo do histórico. | Tela exibe dados do chamado, responsável atual e todos os eventos em ordem cronológica. | | | |
| T02.5 | FE | Criar a ação de adicionar atualização ao chamado. | Após salvar, a atualização aparece na linha do tempo sem necessidade de recarregar manualmente a tela. | | | |

### HU03 - Identificar relatos duplicados ou relacionados

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T03.1 | BD | Ajustar a estrutura para permitir múltiplos Registros de Contato por Chamado, preservando a referência de origem. | Um chamado aceita N contatos; o contato vinculado mantém a informação de sua origem. | | | |
| T03.2 | BE | Implementar a operação de vincular um relato a um chamado existente. | Vínculo registrado no histórico dos chamados envolvidos; registro original do contato não é apagado; testes automatizados cobrem o cenário. | | | |
| T03.3 | FE | Criar a ação "vincular a chamado existente" com seleção do chamado de destino. | Analista consegue localizar o chamado de destino e confirmar o vínculo. | | | |
| T03.4 | FE | Exibir os relatos vinculados na tela de detalhe do chamado. | Todos os relatos associados aparecem identificados no detalhe do chamado. | | | |

Critério de duplicidade/relacionamento: **(decisão em aberto)** com o cliente.

### HU04 - Definir a prioridade do chamado

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T04.1 | BD | Modelar os campos de severidade, urgência, impacto operacional e prioridade resultante. | Campos criados com valores restritos às escalas definidas **(decisão em aberto)**. | | | |
| T04.2 | BE | Implementar a regra de cálculo da prioridade a partir de severidade, urgência e impacto. | Regra implementada conforme matriz definida **(decisão em aberto)**; testes automatizados cobrem todas as combinações possíveis. | | | |
| T04.3 | BE | Restringir a definição e reclassificação da prioridade aos perfis autorizados e registrar a alteração no histórico. | Perfis não autorizados recebem recusa; toda alteração gera evento com valor anterior, novo valor, data e autor. | | | |
| T04.4 | FE | Criar os controles de avaliação de severidade, urgência e impacto com exibição da prioridade resultante. | Os três critérios são selecionados separadamente e a prioridade resultante é exibida antes de salvar. | | | |

### HU05 - Acompanhar o status e responsável pelo chamado

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T05.1 | BD | Definir e armazenar os status possíveis e as transições permitidas do ciclo de vida. | Status e transições documentados e representados na estrutura de dados, incluindo a regra Resolvido → Reaberto. | | | |
| T05.2 | BE | Incluir status atual e responsável nas respostas de consulta de chamados. | Consultas de detalhe e de listagem retornam status e responsável (ou indicação de "sem responsável"). | | | |
| T05.3 | FE | Exibir status e responsável de forma destacada no detalhe do chamado. | Status e responsável visíveis no detalhe; chamados sem responsável identificados claramente. | | | |

### HU06 - Registrar resolução e causa raiz

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T06.1 | BD | Modelar os campos de resolução, causa raiz e indicação de recorrência. | Resolução obrigatória ao resolver; causa raiz opcional; possível relacionar chamados com a mesma causa. | | | |
| T06.2 | BE | Implementar a operação de registrar resolução do chamado. | Operação exige descrição da resolução, altera o status para Resolvido e registra evento no histórico; testes cobrem cenários com e sem causa raiz. | | | |
| T06.3 | BE | Implementar a marcação de causa recorrente entre chamados. | Analista consegue associar chamados à mesma causa; associação consultável posteriormente. | | | |
| T06.4 | FE | Criar o formulário de resolução com campo opcional de causa raiz. | Formulário bloqueia envio sem resolução e permite envio sem causa raiz. | | | |

### HU07 - Escalar chamado para fornecedor externo

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T07.1 | BD | Modelar a estrutura de escalonamento (chamado, fornecedor, data de envio, retorno, data de retorno e responsável interno). | Estrutura criada; um chamado pode ter um ou mais escalonamentos registrados. | | | |
| T07.2 | BE | Implementar a operação de escalar chamado para fornecedor. | Escalonamento registrado no histórico; responsável interno permanece associado ao chamado; testes cobrem o cenário. | | | |
| T07.3 | BE | Implementar a operação de registrar retorno do fornecedor e encerrar o escalonamento. | Retorno salvo com data; escalonamento marcado como encerrado; evento registrado no histórico. | | | |
| T07.4 | FE | Criar as ações de escalar chamado e de registrar retorno do fornecedor. | Analista consegue escalar e registrar retorno; chamados escalados exibem indicação visual de dependência de fornecedor. | | | |

Forma de identificação do fornecedor (campo livre ou cadastro): **(decisão em aberto)**.

### HU08 - Visualizar a situação dos chamados

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T08.1 | BD | Avaliar e criar os recursos de consulta necessários para as agregações (por unidade, status, responsável e recorrência). | Consultas agregadas executam dentro do tempo de resposta definido **(decisão em aberto)**. | | | |
| T08.2 | BE | Implementar as operações de consulta agregada da operação. | Operações retornam: chamados em aberto, responsáveis, totais por unidade e problemas recorrentes; resultados conferidos com dados de teste. | | | |
| T08.3 | BE | Restringir as consultas agregadas ao perfil Liderança de TI. | Usuários de outros perfis recebem recusa de acesso. | | | |
| T08.4 | FE | Criar o painel de visão operacional. | Painel exibe os indicadores definidos e é acessível apenas ao perfil Liderança de TI. | | | |

Conjunto final de indicadores: **(decisão em aberto)**.

### HU09 - Autenticar no sistema

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T09.1 | BD | Modelar a estrutura de Usuário (nome, e-mail, credencial protegida, perfil e situação ativo/inativo). | Estrutura criada; e-mail único garantido por restrição; nenhuma senha armazenada em texto puro. | | | |
| T09.2 | BE | Implementar a operação de autenticação com e-mail e senha. | Credenciais válidas liberam acesso; inválidas retornam mensagem genérica; usuários inativos são recusados; testes cobrem os três cenários. | | | |
| T09.3 | BE | Proteger as operações do sistema por autenticação e perfil. | Requisições sem autenticação ou com perfil não autorizado são recusadas em todas as operações protegidas. | | | |
| T09.4 | BE | Implementar o encerramento de sessão. | Após logout, a sessão anterior não permite novas requisições. | | | |
| T09.5 | FE | Criar a tela de login. | Tela valida campos obrigatórios, exibe mensagem de erro genérica e redireciona após sucesso. | | | |
| T09.6 | FE | Proteger as telas e ocultar funcionalidades conforme o perfil. | Usuário não autenticado é redirecionado ao login; funcionalidades de outro perfil não ficam visíveis. | | | |

Mecanismo de autenticação e sessão: **(decisão em aberto)**.

### HU10 - Gerenciar analistas

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T10.1 | BE | Implementar as operações de cadastrar, editar, listar e desativar analistas. | Operações restritas ao perfil autorizado; e-mail duplicado é recusado; desativação não remove o registro; testes cobrem os cenários. | | | |
| T10.2 | BE | Impedir acesso e atribuição de chamados a analistas desativados. | Analista inativo não autentica e não aparece como opção de responsável. | | | |
| T10.3 | FE | Criar a tela de gestão de analistas. | Tela permite cadastrar, editar, listar e desativar analistas, exibindo a situação de cada um. | | | |

Perfil responsável pela gestão: **(decisão em aberto)**.

### HU11 - Cadastrar unidades

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T11.1 | BD | Modelar a estrutura de Unidade (nome, identificação e situação ativo/inativo). | Estrutura criada; identificação única garantida por restrição. | | | |
| T11.2 | BE | Implementar as operações de cadastrar, editar, listar e desativar unidades. | Unidades inativas não são aceitas em novos chamados; chamados existentes mantêm o vínculo; testes cobrem os cenários. | | | |
| T11.3 | FE | Criar a tela de gestão de unidades. | Tela permite cadastrar, editar, listar e desativar unidades, exibindo a situação de cada uma. | | | |

### HU12 - Atribuir responsável e atualizar status

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T12.1 | BE | Implementar as operações de assumir e reatribuir chamado. | Apenas analistas ativos podem ser atribuídos; toda atribuição gera evento no histórico com responsável anterior, novo responsável, data e autor. | | | |
| T12.2 | BE | Implementar a operação de alteração de status com validação das transições. | Transições inválidas são recusadas com mensagem explicativa; transições válidas geram evento no histórico; testes cobrem todas as transições. | | | |
| T12.3 | FE | Criar os controles de atribuição e de alteração de status no detalhe do chamado. | Apenas transições válidas para o status atual são oferecidas; alterações refletem imediatamente no detalhe e no histórico. | | | |

### HU13 - Registrar novo contato em chamado existente

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T13.1 | BD | Definir e armazenar a lista de canais de origem aceitos. | Canais disponíveis representados na estrutura de dados e utilizados pelos Registros de Contato. | | | |
| T13.2 | BE | Implementar a operação de adicionar Registro de Contato a um chamado existente. | Contato salvo com canal, data e solicitante; evento registrado no histórico; testes cobrem o cenário. | | | |
| T13.3 | FE | Criar o formulário de novo contato no detalhe do chamado. | Formulário exige canal, data e solicitante; contato aparece no histórico após salvar. | | | |

### HU14 - Listar e buscar chamados

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T14.1 | BD | Avaliar e criar os recursos de consulta para os filtros de status, unidade, prioridade e responsável. | Listagem filtrada executa dentro do tempo de resposta definido **(decisão em aberto)**. | | | |
| T14.2 | BE | Implementar a operação de listagem de chamados com filtros, busca por identificador e paginação. | Operação retorna identificador, unidade, status, prioridade e responsável; filtros combináveis; testes cobrem filtros e busca. | | | |
| T14.3 | FE | Criar a tela de listagem de chamados. | Tela exibe filtros, busca por identificador, opção "meus chamados", paginação e acesso ao detalhe de cada chamado. | | | |

### HU15 - Encerrar chamado com verificação de rastreabilidade

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T15.1 | BD | Modelar o armazenamento do resultado da verificação de rastreabilidade. | Para cada chamado encerrado, ficam registrados: resultado (completo/incompleto), itens pendentes e data. | | | |
| T15.2 | BE | Implementar a operação de encerramento com verificação dos elementos mínimos. | Encerramento só é aceito a partir de Resolvido; a verificação avalia todos os elementos definidos na HU15; evento registrado no histórico; testes cobrem chamados completos e incompletos. | | | |
| T15.3 | BE | Implementar o cálculo da métrica de percentual de chamados rastreáveis por período. | Métrica calculada conforme a fórmula definida no documento do MVP e conferida com dados de teste. | | | |
| T15.4 | FE | Criar a ação de encerramento com exibição dos itens pendentes. | Antes de confirmar, o analista visualiza a lista de elementos completos e pendentes. | | | |

Comportamento diante de pendências (bloquear ou apenas alertar): **(decisão em aberto)**.

### HU16 - Obrigatoriedade de e-mail e notificações automáticas de status

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T16.1 | BD | Criar migration Flyway tornando `email` obrigatório para novos solicitantes e adicionando tabela/colunas de controle de entrega de notificação. | Constraint `NOT NULL` validada para novos registros; migration executa sem quebrar seeds existentes. | | | |
| T16.2 | BE | Definir a porta de saída `NotificacaoPort` no domínio e implementar o adaptador `SmtpEmailAdapter` com processamento assíncrono (`@Async` + `@TransactionalEventListener(AFTER_COMMIT)`). | Mudança de status persiste no banco mesmo se o mock SMTP lançar exceção; evento `NOTIFICACAO_ENVIADA` gravado quando enviado com sucesso. | | | |
| T16.3 | BE | Criar templates HTML/Texto enxutos e seguros (sem expor dados sensíveis) para abertura e transição de status (`EM_TRIAGEM`, `EM_ATENDIMENTO`, `AGUARDANDO`, `RESOLVIDO`). | Templates renderizam código `LUM-AAAA-NNNNNN`, unidade, novo status e data formatada. | | | |
| T16.4 | FE | Atualizar o formulário de **Novo Chamado (visão do Analista de Suporte)** tornando o campo `emailSolicitante` obrigatório com validação reativa de formato. | Na tela do Analista, o botão de salvar permanece desabilitado se o e-mail do solicitante estiver vazio ou inválido; telefone permanece como campo complementar. | | | |
| T16.5 | QA | Criar testes de integração validando disparo pós-commit e resiliência quando o serviço de e-mail falha. | Suite de testes passa validando que o status do chamado muda para `EM_ATENDIMENTO` mesmo com falha simulada no adaptador SMTP. | | | |

### HU17 - Cadastro de conta do Solicitante e verificação por e-mail

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T17.1 | BD | Adicionar role `SOLICITANTE`, vínculo `usuario_id` na tabela `solicitantes` e tabela `tokens_verificacao_email` (token hash, expiração, utilizado). | Migration executada com índices de busca rápida por token e integridade referencial garantida. | | | |
| T17.2 | BE | Implementar casos de uso de auto-cadastro (`POST /api/v1/auth/registrar` com política configurável de domínio `@lumen` vs. livre), confirmação de token (`POST /api/v1/auth/verificar-email`) e reenvio de código. | Token expirado ou já utilizado retorna `422 Unprocessable Entity`; confirmação ativa o usuário e vincula chamados preexistentes pelo mesmo e-mail. | | | |
| T17.3 | FE | Construir telas Web de Auto-Cadastro, "Verifique seu E-mail" e callback de confirmação de conta. | Fluxo completo navegável com feedbacks claros de sucesso, token expirado e botão de reenvio com cooldown de 60s. | | | |
| T17.4 | QA | Testar ciclo de vida do token de verificação (sucesso, expiração, reutilização, vínculo retroativo de chamados e toggle da flag de domínio `@lumen`). | Testes automatizados cobrindo 100% das regras de segurança do cadastro. | | | |

### HU18 - Módulo Web do Solicitante (/solicitante/**) e Chat Assíncrono em Texto (via HTTP)

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T18.1 | BE | Criar pacote/módulo `solicitante` com endpoints semânticos `GET /api/v1/solicitante/chamados`, `GET /api/v1/solicitante/chamados/{id}/mensagens` e `POST /api/v1/solicitante/chamados/{id}/mensagens` filtrando pelo `solicitante_id` autenticado. | Tentativa de acessar ou enviar mensagem em chamado de outro solicitante retorna `403 Forbidden` ou `404 Not Found`. | | | |
| T18.2 | BE | Implementar regra de domínio de reação às mensagens de texto da thread: destravar `AGUARDANDO/SOLICITANTE`, disparar e-mail com link Web quando o analista responde e permitir Reabrir/Confirmar quando `RESOLVIDO`. | Eventos `REGISTRO_DE_CONTATO_RECEBIDO`, `RETORNO_RECEBIDO`, `NOTIFICACAO_ENVIADA` ou `CHAMADO_REABERTO` gravados corretamente no histórico. | | | |
| T18.3 | FE | Criar módulo Angular `features/solicitante/` (`/solicitante/chamados`) e componente de **Thread de Chat em Texto** (compartilhável com a visão do Analista), com *polling* leve de 15s e `visibilitychange`. | Solicitante e analista trocam mensagens de texto via HTTP com atualização fluida na Web. | | | |
| T18.4 | QA | Testes de isolamento de dados (IDOR) na rota `/api/v1/solicitante/**`, separação entre nota interna vs. mensagem pública e testes de reabertura. | Nenhuma falha de autorização (IDOR) permitida nos testes de segurança. | | | |

### HU19 - Prevenção de Race Condition, justificativa obrigatória em reatribuição e checkbox "Atribuir a mim" desmarcado

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T19.1 | BD | Adicionar coluna `versao BIGINT NOT NULL DEFAULT 0` na tabela `chamados` via migration Flyway para controle de concorrência otimista. | Coluna criada e populada nos registros existentes sem downtime. | | | |
| T19.2 | BE | Mapear `@Version` na entidade `Chamado`, exigir justificativa obrigatória quando `this.responsavel != null` na troca de responsável e tratar `ObjectOptimisticLockingFailureException` retornando `409 Conflict`. | Tentativa de sobrescrever responsável existente sem justificativa retorna `422 Unprocessable Entity`; colisão simultânea retorna `409 Conflict`. | | | |
| T19.3 | FE | Alterar `atribuirAMim` para `false` por padrão em Novo Chamado, exibir modal de justificativa ao assumir/reatribuir chamado que já tem dono e tratar erro HTTP 409 recarregando o card. | Checkbox nasce desmarcado; modal exige justificativa na reatribuição; colisão 409 exibe toast e atualiza responsável na tela. | | | |
| T19.4 | QA | Criar teste de concorrência multithread (`CountDownLatch` com 5 threads simultâneas) e teste de obrigatoriedade de justificativa na reatribuição. | Exatamente 1 thread recebe `200 OK`, 4 recebem `409 Conflict`; reatribuição sem justificativa é bloqueada. | | | |

### HU20 - Visão operacional da unidade e reatribuição auditada pelo Gestor de Unidade

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T20.1 | BD | Adicionar perfil `GESTOR_UNIDADE` e coluna/tabela de vínculo `usuario_unidades` relacionando o gestor às suas unidades. | Vínculo persistido no banco; constraint garante que todo `GESTOR_UNIDADE` possua ao menos uma unidade ativa associada. | | | |
| T20.2 | BE | Implementar filtro de escopo automático por unidade no Backend (baseado no usuário autenticado) para listagem de chamados e endpoints de métricas, reutilizando a validação de justificativa obrigatória na reatribuição. | Gestor da Unidade A tentando consultar ou reatribuir chamado da Unidade B recebe `403 Forbidden`; reatribuição sem justificativa recebe `422`. | | | |
| T20.3 | FE | Adaptar rotas, menu e telas de Fila e Dashboard para o perfil `GESTOR_UNIDADE`, fixando o contexto na sua unidade e reutilizando o modal de justificativa obrigatória ao reatribuir analista. | Gestor logado vê o nome da sua unidade fixado no topo, acessa gráficos locais e realiza reatribuição justificada. | | | |
| T20.4 | QA | Testes de RBAC garantindo isolamento entre unidades para `GESTOR_UNIDADE`, bloqueio de acesso a anexos (RN06) e auditoria obrigatória na reatribuição. | 100% dos testes de fronteira de permissão (RBAC) passando. | | | |

### HU21 - Upload e visualização segura de anexos essenciais (Fotos e Documentos) ponta a ponta

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T21.1 | BD | Criar migration Flyway da tabela `anexos` (id, chamado_id, nome_original, chave_storage, mime_type, tamanho_bytes, hash_sha256, enviado_por, criado_em). | Tabela criada com chaves estrangeiras e restrições de tamanho/tipo. | | | |
| T21.2 | BE | Criar porta `AnexoStoragePort` e implementar adaptador de armazenamento seguro + validador de *Magic Bytes* (JPG, PNG, WebP, PDF, XLSX/XLS) + endpoints de upload e download auditado (`GET /anexos/{id}/conteudo`). | Upload de arquivo `.exe` renomeado para `.png` ou `.xlsx` retorna `415 Unsupported Media Type`; download por analista grava `ANEXO_VISUALIZADO` e por liderança/gestor retorna `403`. | | | |
| T21.3 | FE | Construir componente *Dropzone* de upload na abertura do chamado com validação de tamanho/tipo (`jpg, png, webp, pdf, xlsx, xls`), aviso de LGPD e galeria de anexos ocultos por padrão no detalhe. | Usuário anexa fotos/documentos na abertura e analista visualiza mediante confirmação de auditoria. | | | |
| T21.4 | QA | Testes automatizados de segurança de upload (arquivos > 5MB, MIME falso em imagens/planilhas, path traversal) e auditoria estrita de visualização (M12). | Testes QA4-06, QA4-07 e QA4-08 verdes no pipeline. | | | |

### HU22 - Questionário de negócio na abertura e validação da Matriz 3D pelo analista

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T22.1 | BD | Garantir colunas de respostas/notas preliminares (`severidade_informada`, `urgencia_informada`, `impacto_informado`, `prioridade_sugerida`) e notas validadas pelo analista na tabela `chamados`. | Migration aplicada; constraints validam valores no intervalo `1..3`. | | | |
| T22.2 | BE | Refinar o Domain Service `CalculadoraPrioridadeMatriz3D` para operar tanto na abertura (gerando a sugestão preliminar) quanto na confirmação/reclassificação pelo analista. | Teste paramétrico JUnit cobre as 27 combinações possíveis ($3 \times 3 \times 3$) sem falhas. | | | |
| T22.3 | FE | Substituir seletor isolado por *Wizard/Cards de Perguntas de Negócio* na abertura do chamado e criar painel comparativo ("Sugerido pelo Contexto vs. Validação do Analista") na tela de detalhe. | Solicitante responde às 3 perguntas em menos de 20 segundos; analista visualiza a sugestão `P1/P2/P3` e valida ou reclassifica com justificativa. | | | |
| T22.4 | QA | Testar fluxo ponta a ponta: abertura pelo solicitante gerando sugestão automática -> validação pelo analista -> cálculo correto da métrica de concordância M08. | Testes unitários e de integração aprovados para confirmação direta e divergência justificada. | | | |

### HU23 - Painel de Métricas e SLA (Gráficos de Barras, Rosca e Indicadores) para Liderança e Gestor de Unidade

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T23.1 | BD | Criar índices compostos em `chamados` (`unidade_id`, `aberto_em`, `status`, `prioridade_final`, `origem_dado`) para garantir agregações rápidas. | Consultas agregadas executam em `< 200ms` para base de 10.000 chamados. | | | |
| T23.2 | BE | Implementar endpoints agregados (`GET /api/v1/dashboard/resumo`, `/volume-barras`, `/distribuicao-prioridades`) respeitando o filtro de unidade do perfil logado. | Retorno JSON estruturado com séries prontas para os gráficos e cálculo exato de chamados dentro/fora do prazo de referência. | | | |
| T23.3 | FE | Construir tela `features/dashboard/` com filtros de período/unidade, cards de indicadores de prazo/SLA, gráfico de barras e gráfico de rosca responsivos. | Gráficos atualizam dinamicamente ao alterar os filtros de período e unidade; estados vazios (*empty states*) tratados. | | | |
| T23.4 | QA | Validar exatidão matemática dos agregados contra massa de dados controlada (oráculo) e testar restrição de escopo do `GESTOR_UNIDADE`. | 100% de precisão nos percentuais de prazo e contagens por prioridade. | | | |

### HU24 - (Extra/Evolução) Exportação estruturada de dados para ferramentas de BI

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T24.1 | BE | Implementar `GET /api/v1/dashboard/exportacao` com streaming de resposta (`CSV` e `JSON`) restrito a `LIDERANCA_TI`. | Download do arquivo gerado corretamente com cabeçalhos UTF-8 BOM (para acentuação correta no Excel/Power BI). | | | |
| T24.2 | FE | Adicionar botão "Exportar para BI (CSV / JSON)" no topo do Dashboard respeitando os filtros selecionados. | Clique inicia download imediato do relatório filtrado. | | | |
| T24.3 | QA | Testar ausência de colunas de dados pessoais (PII) no arquivo exportado e validar limite de período máximo por requisição. | Arquivo validado sem vazamento de e-mail, telefone ou descrição bruta. | | | |

### HU25 - Atualização em tempo real de fila, responsáveis, status e alertas P1 via WebSocket

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T25.1 | BE | Configurar Spring WebSocket com STOMP + autenticação via JWT no handshake/channel interceptor e criar `StompEventoTempoRealAdapter` implementando `EventoTempoRealPort`. | Conexões sem JWT válido são rejeitadas; eventos `CHAMADO_CRIADO`, `CHAMADO_ATRIBUIDO` e `STATUS_ALTERADO` são publicados em `/topic/chamados` após o commit da transação. | | | |
| T25.2 | FE | Implementar `RealtimeChamadosService` no Angular (usando `@stomp/rx-stomp`) integrado aos Signals/Observables da `FilaChamados` e `DetalheChamado`. | Ao receber mensagem no tópico, a lista local atualiza o item in-place com animação sutil de destaque e exibe banner de novo P1. | | | |
| T25.3 | QA | Teste de integração de WebSocket validando autenticação no canal STOMP e entrega de mensagem pós-mutação de chamado. | Cliente STOMP de teste recebe o payload JSON em menos de 500ms após o `POST`/`PATCH` REST. | | | |

### HU26 - Refatorar autenticação JWT para armazenamento em Cookies HttpOnly

| ID | Camada | Tarefa | Critério de conclusão | Responsável | Estimativa | Prazo |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| T26.1 | BE | Refatorar `AuthController` (`/login` e `/logout`) para construir `ResponseCookie` com `httpOnly(true)`, `secure`, `sameSite` e `maxAge`, e criar endpoint `GET /api/v1/auth/me` retornando `id`, `nome`, `email` e `perfil`. | Resposta de login retorna cabeçalho `Set-Cookie: nexo_access_token=...; HttpOnly; SameSite=Lax`; logout retorna `Max-Age=0`. | | | |
| T26.2 | BE | Atualizar `JwtAuthenticationFilter` e configuração de CORS (`allowCredentials(true)` com origens explícitas) e CSRF no Spring Security para leitura do cookie `HttpOnly`. | Filtro autentica requisições lendo `request.getCookies()` sem depender do header `Authorization: Bearer`. | | | |
| T26.3 | FE | Refatorar `authInterceptor` e `AuthService` no Angular para usar `withCredentials: true`, remover persistência de JWT do `TokenStorageService` (`localStorage`) e manter apenas o estado reativo do perfil logado em memória/`/auth/me`. | Inspecionando `window.localStorage` e `document.cookie` via console JS, nenhum token JWT é legível por script. | | | |
| T26.4 | QA | Criar testes de segurança validando presença das flags `HttpOnly`, `SameSite` e `Max-Age=0` no logout, além de testes E2E com `withCredentials`. | 100% dos testes de autenticação e sessão passando sem token no `localStorage`. | | | |

### Resumo de tarefas por camada

| Camada | Quantidade |
| :---- | :---- |
| Banco de Dados (BD) | 21 |
| Backend (BE) | 40 |
| Frontend (FE) | 30 |
| Qualidade (QA) | 11 |
| **Total** | **102** |
