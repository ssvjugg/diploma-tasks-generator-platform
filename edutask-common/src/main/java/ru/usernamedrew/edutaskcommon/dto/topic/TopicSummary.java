package ru.usernamedrew.edutaskcommon.dto.topic;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Краткое описание темы")
public record TopicSummary(
    @Schema(description = "Идентификатор темы")
    UUID id,

    @Schema(description = "Название темы", example = "Динамическое программирование")
    String name
) {
}
