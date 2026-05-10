package ru.usernamedrew.edutaskcommon.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

@Schema(description = "Запрос на обновление задачи")
public record TaskUpdateRequest(
    @Schema(description = "Название задачи", example = "Сумма двух чисел")
    @Pattern(regexp = ".*\\S.*", message = "Field must contain at least one non-whitespace character")
    @Size(max = 255)
    String title,

    @Schema(description = "Условие задачи")
    @Pattern(regexp = ".*\\S.*", message = "Field must contain at least one non-whitespace character")
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
