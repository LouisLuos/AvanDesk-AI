# Histórias de Usuário do MVP — Nexo

Rede Lúmen Diagnósticos

## 1. Visão geral

Este documento consolida as histórias de usuário propostas para o MVP do Nexo. As histórias HU01 a HU08 mantêm a numeração original; as histórias HU09 a HU15 foram adicionadas para cobrir lacunas necessárias ao funcionamento completo do ciclo Registrar → Triar/Acompanhar → Resolver/Escalar.

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
* O registro deve indicar o canal de origem da solicitação (WhatsApp, telefone, e-mail, presencial, outro). **(novo)**

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

Algumas histórias podem ser desenvolvidas em paralelo, desde que suas dependências estejam atendidas.

## 4. Pontos a validar

* Confirmar se a Liderança de TI é o perfil responsável pelo cadastro de analistas e unidades (HU10 e HU11) ou se existirá um perfil administrador.
* Definir se os fornecedores externos serão um campo livre ou um cadastro próprio (o que geraria uma história adicional).
* Definir quem possui autoridade para reclassificar a prioridade (HU04).

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

**Legenda de camadas:** BD = Banco de Dados · BE = Backend · FE = Frontend

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

### Resumo de tarefas por camada

| Camada | Quantidade |
| :---- | :---- |
| Banco de Dados (BD) | 14 |
| Backend (BE) | 26 |
| Frontend (FE) | 19 |
| **Total** | **59** |
