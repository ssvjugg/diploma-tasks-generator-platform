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

Для создания задач в dev-режиме используется UUID существующего `user_profile`:

```bash
VITE_DEV_AUTHOR_ID=11111111-1111-1111-1111-111111111111 npm run dev
```

Если переменную не задавать, frontend использует этот же dev UUID по умолчанию.

### Через Docker

Сборка и запуск только frontend:

```bash
docker build \
  --build-arg VITE_DEV_AUTHOR_ID=11111111-1111-1111-1111-111111111111 \
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

`VITE_DEV_AUTHOR_ID` нужен только до подключения авторизации. Позже frontend будет брать автора из текущей сессии.
