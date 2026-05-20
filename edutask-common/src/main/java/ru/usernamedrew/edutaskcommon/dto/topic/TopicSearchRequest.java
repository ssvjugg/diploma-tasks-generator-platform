package ru.usernamedrew.edutaskcommon.dto.topic;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Фильтры поиска тем")
public record TopicSearchRequest(
    @Schema(description = "Поисковая строка по названию темы", example = "динамическое")
    String query,

    @Schema(description = "Идентификатор родительской темы")
    UUID parentId,

    @Schema(description = "Искать только корневые темы без родителя", example = "true")
    Boolean rootOnly
) {
}
