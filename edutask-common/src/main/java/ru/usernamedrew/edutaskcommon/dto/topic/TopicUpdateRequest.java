package ru.usernamedrew.edutaskcommon.dto.topic;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Запрос на частичное обновление темы")
public record TopicUpdateRequest(
    @Schema(description = "Название темы", example = "Динамическое программирование")
    @Size(max = 255)
    @Pattern(regexp = ".*\\S.*", message = "Must contain non-whitespace characters")
    String name,

    @Schema(description = "Идентификатор родительской темы")
    UUID parentId
) {
}
