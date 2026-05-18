package ru.usernamedrew.edutaskcore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.usernamedrew.edutaskcommon.dto.task.TaskCreateRequest;
import ru.usernamedrew.edutaskcommon.dto.task.TaskResponse;
import ru.usernamedrew.edutaskcommon.dto.task.TaskSearchRequest;
import ru.usernamedrew.edutaskcommon.dto.task.TaskSummary;
import ru.usernamedrew.edutaskcommon.dto.task.TaskUpdateRequest;
import ru.usernamedrew.edutaskcore.service.TaskService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Операции с банком задач")
public class TaskController {
    private final TaskService taskService;

    @Operation(
        summary = "Получить список задач",
        description = "Возвращает постраничный список задач с фильтрами по тексту, сложности, автору и теме."
    )
    @ApiResponse(responseCode = "200", description = "Список задач получен")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<TaskSummary> getTasks(
        @ParameterObject TaskSearchRequest request,
        @ParameterObject @PageableDefault(size = 20, sort = "title") Pageable pageable
    ) {
        return taskService.findTasks(request, pageable);
    }

    @Operation(
        summary = "Найти задачи",
        description = "Возвращает постраничный список задач по фильтрам из JSON-body. Подходит для расширенного поиска."
    )
    @ApiResponse(responseCode = "200", description = "Список задач получен")
    @PostMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public Page<TaskSummary> searchTasks(
        @Valid @RequestBody TaskSearchRequest request,
        @ParameterObject @PageableDefault(size = 20, sort = "title") Pageable pageable
    ) {
        return taskService.findTasks(request, pageable);
    }

    @Operation(summary = "Получить задачу", description = "Возвращает задачу со связанными темами.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Задача найдена"),
        @ApiResponse(responseCode = "404", description = "Задача не найдена", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public TaskResponse getTask(@PathVariable UUID id) {
        return taskService.getTask(id);
    }

    @Operation(summary = "Создать задачу", description = "Создает задачу и связывает ее с автором и темами.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Задача создана"),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Автор или тема не найдены", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public TaskResponse createTask(@Valid @RequestBody TaskCreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return taskService.createTask(request, jwt);
    }

    @Operation(
        summary = "Частично обновить задачу",
        description = "Обновляет только переданные поля. Чтобы очистить темы, передайте пустой массив."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Задача обновлена"),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Задача, автор или тема не найдены", content = @Content(schema = @Schema(hidden = true)))
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public TaskResponse patchTask(
        @PathVariable UUID id,
        @Valid @RequestBody TaskUpdateRequest request,
        Authentication authentication
    ) {
        return taskService.patchTask(id, request, hasRole(authentication, "ROLE_ADMIN"));
    }

    @Operation(summary = "Удалить задачу", description = "Удаляет задачу. Связанные тест-кейсы удаляются каскадно на уровне БД.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Задача удалена"),
        @ApiResponse(responseCode = "404", description = "Задача не найдена", content = @Content(schema = @Schema(hidden = true)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public void deleteTask(@PathVariable UUID id) {
        taskService.deleteTask(id);
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(role::equals);
    }
}
