package ru.usernamedrew.edutaskcommon.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Фильтры поиска задач")
public record TaskSearchRequest(
    @Schema(description = "Поисковая строка по названию и условию", example = "массив")
    String query,

    @Schema(description = "Сложность задачи", example = "EASY")
    TaskDifficulty difficulty,

    @Schema(description = "Идентификатор автора")
    UUID authorId,

    @Schema(description = "Идентификатор темы")
    UUID topicId,

    @Schema(description = "Код языка программирования", example = "java")
    String languageCode
) {
}
