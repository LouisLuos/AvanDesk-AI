# AvanDesk-AI — Frontend

> Aplicação Web do sistema de suporte e gestão de chamados **AvanDesk-AI**  
> **Stack:** Angular · Tailwind CSS v4 · TypeScript · SSR · Vitest

---

## 🚀 Tecnologias e Versões

| Recurso | Tecnologia | Descrição |
|---|---|---|
| **Framework** | Angular | Framework SPA corporativo com arquitetura reativa (Signals) |
| **Estilização** | Tailwind CSS v4 | Framework CSS utilitário de alta performance |
| **SSR / SSG** | Angular SSR + Express | Renderização no servidor e pré-renderização |
| **Testes** | Vitest + jsdom | Executor de testes unitários ultrarrápido |
| **Linguagem** | TypeScript | Tipagem estática rigorosa |

---

## 📂 Estrutura de Pastas Modular

A aplicação adota uma organização escalável orientada a domínios e responsabilidades:

```
frontend/
├── public/                     # Arquivos estáticos (ícones, logos, manifest)
├── src/
│   ├── app/
│   │   ├── core/               # Singleton: interceptors HTTP, auth service, guards
│   │   ├── shared/             # Reutilizáveis: botões, modais, pipes, diretivas, models
│   │   ├── features/           # Módulos de negócio:
│   │   │   ├── auth/           # Login, recuperação de acesso
│   │   │   ├── tickets/        # Abertura, listagem e detalhes de chamados
│   │   │   ├── dashboard/      # Métricas de atendimento e gráficos
│   │   │   └── triage/         # Interface de triagem com IA
│   │   ├── layout/             # Componentes estruturais: navbar, sidebar, footer
│   │   ├── app.config.ts       # Provedores da aplicação (HttpClient, Router, etc.)
│   │   ├── app.routes.ts       # Configuração de rotas da aplicação
│   │   ├── app.ts              # Componente raiz (Root Component)
│   │   ├── app.html            # Template raiz
│   │   └── app.css             # Estilos específicos do componente raiz
│   ├── environments/
│   │   ├── environment.ts              # Variáveis de ambiente (Produção)
│   │   └── environment.development.ts  # Variáveis de ambiente (Desenvolvimento)
│   ├── styles.css              # Ponto de entrada global com @import 'tailwindcss'
│   └── index.html              # HTML base da aplicação
├── proxy.conf.json             # Redirecionamento local de /api -> http://localhost:8080
├── angular.json                # Configuração do Angular CLI e build
├── tsconfig.json               # Configurações do TypeScript
└── package.json                # Dependências e scripts npm
```

---

## ⚙️ Pré-requisitos

- **Node.js**: `v22+` (ou v20.17+)
- **npm**: `v10+` ou `v11+`
- **Backend AvanDesk API**: Em execução na porta `8080` (opcional para visualização inicial)

---

## 🛠️ Comandos de Execução

### 1. Instalar dependências
```bash
npm install
```

### 2. Executar em modo de desenvolvimento
```bash
npm start
# ou: ng serve
```
A aplicação estará acessível em: **`http://localhost:4200`**

> **Proxy reverso configurado:** Todas as chamadas para rotas `/api/*` são automaticamente repassadas para o backend Spring Boot em `http://localhost:8080`.

### 3. Build de Produção
```bash
npm run build
```
Os artefatos compilados serão gerados na pasta `dist/frontend`.

### 4. Executar Testes Unitários
```bash
npm test
```

---

## 🌐 Integração com o Backend

A comunicação entre Frontend e Backend é facilitada via `proxy.conf.json`:

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```

Dessa forma, os serviços Angular chamam diretamente endpoints relativos (ex: `/api/health`, `/api/tickets`), evitando problemas de CORS durante o desenvolvimento local.
