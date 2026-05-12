package ru.usernamedrew.edutaskcommon.dto.topic;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Тема")
public record TopicResponse(
    @Schema(description = "Идентификатор темы")
    UUID id,

    @Schema(description = "Название темы", example = "Динамическое программирование")
    String name,

    @Schema(description = "Идентификатор родительской темы")
    UUID parentId
) {
}
