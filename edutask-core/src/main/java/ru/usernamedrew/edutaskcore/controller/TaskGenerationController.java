package ru.usernamedrew.edutaskcore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.usernamedrew.edutaskcommon.dto.generation.TaskGenerationCreateRequest;
import ru.usernamedrew.edutaskcommon.dto.generation.TaskGenerationResponse;
import ru.usernamedrew.edutaskcore.service.TaskGenerationService;
import ru.usernamedrew.edutaskcore.service.TaskGenerationSseService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks/generations")
@RequiredArgsConstructor
@Tag(name = "Task generations", description = "Асинхронная генерация черновиков задач через LLM")
public class TaskGenerationController {
    private final TaskGenerationService taskGenerationService;
    private final TaskGenerationSseService taskGenerationSseService;

    @Operation(summary = "Запустить генерацию задачи", description = "Создает запрос генерации и отправляет его в Kafka.")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Запрос генерации принят"),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Пользователь или тема не найдены", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public TaskGenerationResponse createGeneration(
        @Valid @RequestBody TaskGenerationCreateRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return taskGenerationService.createGeneration(request, jwt);
    }

    @Operation(summary = "Получить состояние генерации", description = "Возвращает статус и результат генерации, если он готов.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Состояние генерации получено"),
        @ApiResponse(responseCode = "403", description = "Нет доступа к запросу генерации", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Запрос генерации не найден", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public TaskGenerationResponse getGeneration(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return taskGenerationService.getGeneration(id, jwt);
    }

    @Operation(summary = "Подписаться на результат генерации", description = "Открывает SSE stream до завершения генерации.")
    @GetMapping(path = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter streamGeneration(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        TaskGenerationResponse initialState = taskGenerationService.getGeneration(id, jwt);
        SseEmitter emitter = taskGenerationSseService.subscribe(initialState);
        TaskGenerationResponse latestState = taskGenerationService.getGeneration(id, jwt);
        taskGenerationSseService.synchronize(initialState, latestState);
        return emitter;
    }
}
