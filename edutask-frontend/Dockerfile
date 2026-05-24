FROM node:22-alpine AS build

WORKDIR /app

ARG VITE_KEYCLOAK_URL=http://localhost:8085
ARG VITE_KEYCLOAK_REALM=edutask
ARG VITE_KEYCLOAK_CLIENT_ID=edutask-frontend
ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL
ENV VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM
ENV VITE_KEYCLOAK_CLIENT_ID=$VITE_KEYCLOAK_CLIENT_ID

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build

FROM nginx:1.27-alpine

ENV API_UPSTREAM=http://host.docker.internal:8080

COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
