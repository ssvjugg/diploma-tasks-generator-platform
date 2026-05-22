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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.usernamedrew.edutaskcommon.dto.testcase.TestCaseCreateRequest;
import ru.usernamedrew.edutaskcommon.dto.testcase.TestCaseResponse;
import ru.usernamedrew.edutaskcommon.dto.testcase.TestCaseUpdateRequest;
import ru.usernamedrew.edutaskcore.service.TestCaseService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/test-cases")
@RequiredArgsConstructor
@Tag(name = "Test cases", description = "Операции с тест-кейсами задач")
public class TestCaseController {
    private final TestCaseService testCaseService;

    @Operation(
        summary = "Получить тест-кейсы задачи",
        description = "Возвращает тест-кейсы указанной задачи. Скрытые тесты доступны только TEACHER и ADMIN."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список тест-кейсов получен"),
        @ApiResponse(responseCode = "404", description = "Задача не найдена", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<TestCaseResponse> getTaskTestCases(@PathVariable UUID taskId, Authentication authentication) {
        return testCaseService.getTaskTestCases(taskId, canManageTestCases(authentication));
    }

    @Operation(summary = "Добавить тест-кейс", description = "Создает тест-кейс и привязывает его к задаче.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Тест-кейс создан"),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Задача не найдена", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public TestCaseResponse createTestCase(
        @PathVariable UUID taskId,
        @Valid @RequestBody TestCaseCreateRequest request
    ) {
        return testCaseService.createTestCase(taskId, request);
    }

    @Operation(summary = "Обновить тест-кейс", description = "Частично обновляет тест-кейс указанной задачи.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Тест-кейс обновлен"),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Задача или тест-кейс не найдены", content = @Content(schema = @Schema(hidden = true)))
    })
    @PatchMapping("/{testCaseId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public TestCaseResponse patchTestCase(
        @PathVariable UUID taskId,
        @PathVariable UUID testCaseId,
        @Valid @RequestBody TestCaseUpdateRequest request
    ) {
        return testCaseService.patchTestCase(taskId, testCaseId, request);
    }

    @Operation(summary = "Удалить тест-кейс", description = "Удаляет тест-кейс указанной задачи.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Тест-кейс удален"),
        @ApiResponse(responseCode = "404", description = "Задача или тест-кейс не найдены", content = @Content(schema = @Schema(hidden = true)))
    })
    @DeleteMapping("/{testCaseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public void deleteTestCase(@PathVariable UUID taskId, @PathVariable UUID testCaseId) {
        testCaseService.deleteTestCase(taskId, testCaseId);
    }

    private boolean canManageTestCases(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(authority -> "ROLE_TEACHER".equals(authority) || "ROLE_ADMIN".equals(authority));
    }
}
