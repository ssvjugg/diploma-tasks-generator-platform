# Keycloak Realm

Локально запускаем Keycloak следующей командой:

```bash
docker compose up -d keycloak
```

Админ консоль:

```text
http://localhost:8085/admin
```

Дефолтные данные для входа:

```text
username: admin
password: admin
```

## Realm

Создаем реалм со следующим названием:

```text
edutask
```

issuer URL используемый Gateway-ем:

```text
http://localhost:8085/realms/edutask
```

По дефолту уже прописано в `KEYCLOAK_ISSUER_URI`.

## Roles

Создаем роли в нашем реалме:

```text
STUDENT
TEACHER
ADMIN
```

Ограничения ролей:

```text
STUDENT - может читать задачи, топики, отправлять решения
TEACHER - все тоже что и STUDENT только может создавать задачи, топики, тест-кейсы
ADMIN   - все что и TEACHER но также может редактировать авторов 
```

## Frontend Client

Создаем клиент:

```text
Client ID: edutask-frontend
Client type: OpenID Connect
Client authentication: Off
Authorization: Off
Standard flow: On
Direct access grants: Off
```

Valid redirect URIs для frontend:

```text
http://localhost:3000/*
```

Valid post logout redirect URIs:

```text
http://localhost:3000/*
```

Web origins:

```text
http://localhost:3000
```

## Test Users

Создаем тестовых пользователей и привязываем к ним по роли:

```text
student@example.test -> STUDENT
teacher@example.test -> TEACHER
admin@example.test   -> ADMIN
```

## Service Environment

JVM сервисы используют эти настройки по дефолту, адрес keycloak можно заменить на название контейнера если Core и 
Gateway в той же сети:

```text
KEYCLOAK_ISSUER_URI=http://localhost:8085/realms/edutask
KEYCLOAK_JWK_SET_URI=http://localhost:8085/realms/edutask/protocol/openid-connect/certs
KEYCLOAK_CLIENT_ID=edutask-frontend
```

Gateway проверяет аутентификацию запросов и перенаправляет их в Core сервис.
Core проверяет тот же JWT локально и отвечает за правила авторизации в соответствии с ролями.
Оба сервиса используют эндпоинт JWKS для ключей подписи токенов, поэтому им не требуется проверка токенов при каждом 
запросе.

Frontend должен отправлять access token в вызовах:

```text
Authorization: Bearer <access_token>
```

Frontend использует Authorization Code + PKCE и следующие настройки Vite:

```text
VITE_KEYCLOAK_URL=http://localhost:8085
VITE_KEYCLOAK_REALM=edutask
VITE_KEYCLOAK_CLIENT_ID=edutask-frontend
```
