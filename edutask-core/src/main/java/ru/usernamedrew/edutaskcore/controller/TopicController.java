package ru.usernamedrew.edutaskcore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.usernamedrew.edutaskcommon.dto.topic.TopicCreateRequest;
import ru.usernamedrew.edutaskcommon.dto.topic.TopicResponse;
import ru.usernamedrew.edutaskcommon.dto.topic.TopicSearchRequest;
import ru.usernamedrew.edutaskcommon.dto.topic.TopicSummary;
import ru.usernamedrew.edutaskcommon.dto.topic.TopicUpdateRequest;
import ru.usernamedrew.edutaskcore.service.TopicService;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/topics")
@RequiredArgsConstructor
@Tag(name = "Topics", description = "Операции с темами задач")
public class TopicController {
    private final TopicService topicService;

    @Operation(
        summary = "Получить список тем",
        description = "Возвращает постраничный список тем с временным поиском по названию до подключения Elasticsearch."
    )
    @ApiResponse(responseCode = "200", description = "Список тем получен")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<TopicResponse> getTopics(
        @ParameterObject TopicSearchRequest request,
        @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return topicService.findTopics(request, pageable);
    }

    @Operation(summary = "Найти темы для автодополнения", description = "Возвращает ограниченный список тем по части названия.")
    @ApiResponse(responseCode = "200", description = "Темы найдены")
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public List<TopicSummary> searchTopics(
        @RequestParam(required = false) String query,
        @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit
    ) {
        return topicService.searchTopics(query, limit);
    }

    @Operation(summary = "Получить тему", description = "Возвращает тему по идентификатору.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Тема найдена"),
        @ApiResponse(responseCode = "404", description = "Тема не найдена", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public TopicResponse getTopic(@PathVariable UUID id) {
        return topicService.getTopic(id);
    }

    @Operation(summary = "Создать тему", description = "Создает новую тему или подтему.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Тема создана"),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Родительская тема не найдена", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public TopicResponse createTopic(@Valid @RequestBody TopicCreateRequest request) {
        return topicService.createTopic(request);
    }

    @Operation(summary = "Частично обновить тему", description = "Обновляет переданные поля темы.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Тема обновлена"),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Тема или родительская тема не найдена", content = @Content(schema = @Schema(hidden = true)))
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public TopicResponse patchTopic(@PathVariable UUID id, @Valid @RequestBody TopicUpdateRequest request) {
        return topicService.patchTopic(id, request);
    }

    @Operation(summary = "Удалить тему", description = "Удаляет тему. Связи с задачами удаляются каскадно на уровне БД.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Тема удалена"),
        @ApiResponse(responseCode = "404", description = "Тема не найдена", content = @Content(schema = @Schema(hidden = true)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public void deleteTopic(@PathVariable UUID id) {
        topicService.deleteTopic(id);
    }
}
