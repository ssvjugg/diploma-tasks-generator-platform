package ru.usernamedrew.edutaskcommon.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

@Schema(description = "Запрос на обновление задачи")
public record TaskUpdateRequest(
    @Schema(description = "Название задачи", example = "Сумма двух чисел")
    @NotBlank
    @Size(max = 255)
    String title,

    @Schema(description = "Условие задачи")
    @NotBlank
    String statement,

    @Schema(description = "Формат входных данных")
    String inputFormat,

    @Schema(description = "Формат выходных данных")
    String outputFormat,

    @Schema(description = "Сложность задачи", example = "MEDIUM")
    TaskDifficulty difficulty,

    @Schema(description = "Идентификатор автора")
    UUID authorId,

    @Schema(description = "Идентификаторы тем. Пустой массив очищает список тем.")
    Set<UUID> topicIds,

    @Schema(description = "Идентификаторы языков. Пустой массив очищает список языков.")
    Set<Integer> languageIds
) {
}
