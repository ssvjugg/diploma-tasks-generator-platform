## Sequence diagram: teacher task generation flow

```mermaid
sequenceDiagram
    autonumber
    actor Teacher as Преподаватель
    participant FE as Frontend React
    participant KC as Keycloak
    participant GW as edutask-gateway
    participant Core as edutask-core
    participant DB as PostgreSQL
    participant Kafka as Kafka
    participant LLMW as edutask-llm-worker
    participant LLM as LLM provider

    Teacher->>FE: Открывает приложение
    FE->>KC: Authorization Code + PKCE
    KC-->>FE: Access token с realm role TEACHER

    FE->>GW: POST /api/v1/users/me/register
    GW->>Core: Проксирование запроса с JWT
    Core->>Core: Извлечь роль из JWT
    Core->>DB: Найти или создать user_profile, синхронизировать role
    DB-->>Core: UserProfile
    Core-->>FE: UserProfileResponse

    Teacher->>FE: Открывает /tasks и форму создания
    FE->>GW: GET /api/v1/topics/search?query=...
    GW->>Core: Проксирование запроса
    Core->>DB: Поиск тем по названию
    DB-->>Core: TopicSummary[]
    Core-->>FE: Список тем

    Teacher->>FE: Вводит prompt, выбирает темы и сложность
    FE->>GW: POST /api/v1/tasks/generations
    GW->>Core: Проксирование запроса с JWT
    Core->>Core: @PreAuthorize TEACHER/ADMIN
    Core->>Core: resolveCurrentUser(jwt)
    Core->>DB: Проверить topicIds
    DB-->>Core: Topic[]
    Core->>DB: INSERT generation_request(status=QUEUED)
    DB-->>Core: requestId
    Core->>Kafka: afterCommit: TaskGenerationRequestedEvent
    Core-->>FE: 202 TaskGenerationResponse(status=QUEUED)

    FE->>GW: GET /api/v1/tasks/generations/{requestId}/stream
    GW->>Core: Проксирование SSE-запроса
    Core->>DB: Получить generation_request
    DB-->>Core: Текущее состояние
    Core->>Core: Проверить owner/admin и зарегистрировать SseEmitter
    Core-->>FE: SSE generation-status, status=QUEUED

    Kafka-->>LLMW: Consume task-generation-requests
    LLMW->>LLMW: Validate event, acquire semaphore permit
    LLMW->>LLMW: Build system/user prompt
    LLMW->>LLM: generateTask(prompt, model, temperature)

    alt LLM успешно вернула валидный JSON
        LLM-->>LLMW: GeneratedTaskDraft JSON
        LLMW->>LLMW: Parse and validate draft
        LLMW->>Kafka: TaskGenerationResponseEvent(status=COMPLETED)
    else Ошибка provider-а, валидации или перегрузка worker-а
        LLMW->>Kafka: TaskGenerationResponseEvent(status=FAILED, errorMessage)
    end

    Kafka-->>Core: Consume task-generation-responses
    Core->>DB: Найти generation_request
    Core->>DB: UPDATE status, generated_draft или error_message
    Core->>Core: afterCommit publish SSE

    alt COMPLETED
        Core-->>FE: SSE generation-completed с GeneratedTaskDraft
        FE-->>Teacher: Показать AI-черновик
        Teacher->>FE: Применить черновик к форме
        FE->>FE: Заполнить title, statement, formats, difficulty, testCases
    else FAILED
        Core-->>FE: SSE generation-failed с errorMessage
        FE-->>Teacher: Показать ошибку генерации
    end

    Teacher->>FE: Проверяет и редактирует форму
    Teacher->>FE: Сохраняет задачу
    FE->>GW: POST /api/v1/tasks
    GW->>Core: Проксирование запроса с JWT
    Core->>Core: @PreAuthorize TEACHER/ADMIN
    Core->>Core: resolveCurrentUser(jwt)
    Core->>DB: INSERT task, связи task_topic
    Core->>DB: INSERT test_case[] из формы
    DB-->>Core: Task + TestCases
    Core-->>FE: 201 TaskResponse
    FE->>GW: GET /api/v1/tasks?... 
    GW->>Core: Проксирование запроса
    Core->>DB: Поиск задач с фильтрами
    DB-->>Core: Page<TaskSummary>
    Core-->>FE: Обновленный банк задач
    FE-->>Teacher: Новая задача видна в списке
```

Поток студенческого решения задач реализован отдельно через `CodeSubmissionController`, Kafka `code-submission-*`, 
`edutask-judge-worker` и Judge0. В этой диаграмме он не является основным, потому что фокус - преподавательское 
создание задания.