# AvanDesk-AI — Rede Lúmen Diagnósticos

> Plataforma inteligente para registro, triagem e acompanhamento de chamados de suporte técnico em saúde e medicina diagnóstica.

Projeto desenvolvido no âmbito da **Residência Tecnológica Avanade 2026.2**, em parceria com o **Porto Digital**, a **CESAR School** e o programa **Embarque Digital**.

---

## 🏥 Contexto do Cliente: Squad 01 — Rede Lúmen Diagnósticos

A **Rede Lúmen Diagnósticos** é uma rede fictícia de saúde e medicina diagnóstica (referência de mercado: *Dasa*) que realiza exames laboratoriais e de imagem:

* **Escala Operacional:** 14 unidades distribuídas em Pernambuco (sendo duas unidades operando 24 horas).
* **Público Atendido:** Pacientes particulares, convênios médicos, empresas e instituições hospitalares.
* **Corpo Colaborador:** Aproximadamente 900 profissionais (recepção, técnicos de laboratório, enfermagem, médicos, áreas administrativas e gestores).
* **Equipe de TI & Suporte:** Time enxuto de 7 analistas de suporte (com atendimento remoto e deslocamento presencial volante), responsável por sistemas corporativos, agendamento, recepção/autorização, impressoras de etiquetas de coleta, estações de trabalho e infraestrutura de rede.

---

## ⚠️ Desafios e Dores Operacionais

1. **Canais Fragmentados e Falta de Rastreabilidade:** Chamados chegam de forma caótica via grupos de WhatsApp, ligações, e-mails, conversas paralelas e planilhas isoladas, sem identificador único (`Ticket ID`), gerando retrabalho e perda de histórico.
2. **"Tudo é Urgente" vs. Impacto Assistencial Real:** Gestores de unidade tendem a classificar qualquer ocorrência local como prioridade máxima. O suporte precisa de inteligência para arbitrar a fila diferenciando um bloqueio na coleta/exame de paciente de um problema administrativo secundário.
3. **Falta de Visibilidade Operacional:** Dificuldade em identificar rapidamente quais analistas estão alocados em cada incidente, quais problemas dependem de fornecedores externos de sistemas e quais unidades possuem maior recorrência de falhas.
4. **Privacidade e LGPD na Saúde:** Nas solicitações de suporte, funcionários frequentemente anexam capturas de tela com nomes, CPFs ou dados médicos. O sistema de suporte **não deve armazenar resultados de exames nem prontuários clínicos**, registrando estritamente os dados técnicos necessários para a resolução.
5. **Estações Compartilhadas:** Múltiplos profissionais compartilham computadores nas recepções e laboratórios com ritmos de trabalho intensos, exigindo sessões seguras e formulários rápidos de preenchimento.

---

## 💡 Como o AvanDesk-AI Transforma essa Realidade

O **AvanDesk-AI** atua como o hub central inteligente de atendimento da Rede Lúmen Diagnósticos:

* 🏷️ **Identificador Único e Centralização:** Cada chamado recebe um protocolo exclusivo, agrupando históricos de mensagens, anexos técnicos e status de resolução em um só lugar.
* 🤖 **Triagem Inteligente com IA:** Algoritmos analisam a descrição do ticket e calculam o nível de criticidade com base no impacto real na cadeia assistencial (ex: impressora de etiquetas da triagem laboratoral parada tem prioridade superior a impressora administrativa de escritório).
* 📝 **Entrada de Dados Guiada:** Sugere campos indispensáveis no momento da abertura (ex: identificação da unidade, setor, equipamento afetado e horário), reduzindo o vaivém de mensagens.
* ⏱️ **Gestão de SLA e Fornecedores:** Alertas automáticos para chamados próximos ao vencimento e acompanhamento transparente de incidentes encaminhados a parceiros externos (ex: sistemas laboratoriais de terceiros).
* 🔒 **Segurança e Conformidade com LGPD:** Validação de anexos e orientações ativas para evitar a exposição indevida de dados clínicos de pacientes em tíquetes de suporte.

---

## 🛠️ Tecnologias & Arquitetura

> ⏳ *A ser definido (em fase de alinhamento com a Squad e consolidação do ecossistema técnico).*

---

## 📁 Estrutura do Repositório

```text
├── .github/
│   └── pull_request_template.md
├── backend/
├── frontend/
├── docs/
├── .gitignore
└── README.md
```

---

## 📚 Documentação e Padrões de Engenharia

* 📑 [Convenções de Git Flow, Branches e Commits](./docs/convencoes-git-workflow-lumen.md)
* 📋 [Template Oficial de Pull Request](./.github/pull_request_template.md)
* 📊 [Análise de Domínio](./docs/01-analise-de-dominio.md)
* ⚙️ [Requisitos Funcionais](./docs/02-requisitos-funcionais.md)
* 🛡️ [Requisitos Não Funcionais](./docs/03-requisitos-nao-funcionais.md)
* 📖 [Histórias de Usuário & Tasks](./docs/04-historias-de-usuario.md)
* 🧭 [Próximos Passos & Roadmap](./docs/05-proximos-passos.md)


