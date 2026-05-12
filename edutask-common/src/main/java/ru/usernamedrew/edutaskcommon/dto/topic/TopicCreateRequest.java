package ru.usernamedrew.edutaskcommon.dto.topic;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Запрос на создание темы")
public record TopicCreateRequest(
    @Schema(description = "Название темы", example = "Динамическое программирование")
    @NotBlank
    @Size(max = 255)
    String name,

    @Schema(description = "Идентификатор родительской темы")
    UUID parentId
) {
}
