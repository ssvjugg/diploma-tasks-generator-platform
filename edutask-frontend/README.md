# EduTask Frontend

Web SPA для сервиса генерации и поддержки банка задач по программированию.

## Технология

Выбран стек `React + TypeScript + Vite`.

- `React` хорошо подходит для будущих экранов банка задач, генерации, чата и кабинетов ролей.
- `TypeScript` позволяет явно описать DTO backend-контрактов.
- `Vite` дает простой старт, быстрый dev server и минимум инфраструктурной сложности для MVP.

## Запуск

### Локально

```bash
npm install
npm run dev
```

По умолчанию frontend проксирует `/api/**` на `http://localhost:8080`.
Если backend будет запущен на другом адресе:

```bash
VITE_API_PROXY_TARGET=http://localhost:8081 npm run dev
```

Для локального входа через Keycloak используются значения по умолчанию:

```bash
VITE_KEYCLOAK_URL=http://localhost:8085
VITE_KEYCLOAK_REALM=edutask
VITE_KEYCLOAK_CLIENT_ID=edutask-frontend
```

### Через Docker

Сборка и запуск только frontend:

```bash
docker build \
  --build-arg VITE_KEYCLOAK_URL=http://localhost:8085 \
  --build-arg VITE_KEYCLOAK_REALM=edutask \
  --build-arg VITE_KEYCLOAK_CLIENT_ID=edutask-frontend \
  -t edutask-frontend ./edutask-frontend
docker run --rm -p 3000:80 \
  -e API_UPSTREAM=http://host.docker.internal:8080 \
  edutask-frontend
```

После запуска приложение будет доступно на `http://localhost:3000`.

Запуск через общий `docker-compose.yaml` из корня проекта:

```bash
docker compose up --build frontend
```

`API_UPSTREAM` указывает, куда Nginx проксирует `/api/**`. Сейчас значение по умолчанию подходит для backend, запущенного на хосте на порту `8080`. Когда gateway появится в compose как сервис, можно заменить значение на адрес сервиса, например `http://edutask-gateway:8080`.
