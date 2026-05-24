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
import ru.usernamedrew.edutaskcommon.dto.submission.CodeSubmissionCreateRequest;
import ru.usernamedrew.edutaskcommon.dto.submission.CodeSubmissionResponse;
import ru.usernamedrew.edutaskcore.service.CodeSubmissionService;
import ru.usernamedrew.edutaskcore.service.CodeSubmissionSseService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Code submissions", description = "Асинхронная проверка решений через Judge0")
public class CodeSubmissionController {
    private final CodeSubmissionService codeSubmissionService;
    private final CodeSubmissionSseService codeSubmissionSseService;

    @Operation(summary = "Отправить решение", description = "Создает сабмит и отправляет событие проверки в Kafka.")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Решение принято на проверку"),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации или у задачи нет тестов", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Задача или язык не найдены", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/tasks/{taskId}/submissions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("isAuthenticated()")
    public CodeSubmissionResponse createSubmission(
        @PathVariable UUID taskId,
        @Valid @RequestBody CodeSubmissionCreateRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return codeSubmissionService.createSubmission(taskId, request, jwt);
    }

    @Operation(summary = "Получить сабмит", description = "Возвращает статус и результаты проверки решения.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Сабмит найден"),
        @ApiResponse(responseCode = "403", description = "Нет доступа к сабмиту", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Сабмит не найден", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/submissions/{submissionId}")
    @PreAuthorize("isAuthenticated()")
    public CodeSubmissionResponse getSubmission(@PathVariable UUID submissionId, @AuthenticationPrincipal Jwt jwt) {
        return codeSubmissionService.getSubmission(submissionId, jwt);
    }

    @Operation(summary = "Подписаться на сабмит", description = "Открывает SSE stream до завершения проверки.")
    @GetMapping(path = "/submissions/{submissionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter streamSubmission(@PathVariable UUID submissionId, @AuthenticationPrincipal Jwt jwt) {
        CodeSubmissionResponse initialState = codeSubmissionService.getSubmission(submissionId, jwt, false);
        SseEmitter emitter = codeSubmissionSseService.subscribe(initialState);
        CodeSubmissionResponse latestState = codeSubmissionService.getSubmission(submissionId, jwt, false);
        codeSubmissionSseService.synchronize(initialState, latestState);
        return emitter;
    }
}
