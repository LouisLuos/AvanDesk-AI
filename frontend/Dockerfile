# =======================================================
# Estágio 1: Build da aplicação Angular 22 (Browser + SSR)
# =======================================================
FROM node:22-alpine AS builder
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# =======================================================
# Estágio 2: Servir com NGINX (proxy reverso /api -> backend:8080)
# =======================================================
FROM nginx:alpine

RUN rm -rf /usr/share/nginx/html/*
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80
