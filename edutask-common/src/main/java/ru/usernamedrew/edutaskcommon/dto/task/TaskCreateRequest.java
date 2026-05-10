package ru.usernamedrew.edutaskcommon.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

@Schema(description = "Запрос на создание задачи")
public record TaskCreateRequest(
    @Schema(description = "Название задачи", example = "Сумма двух чисел")
    @NotBlank
    @Size(max = 255)
    String title,

    @Schema(description = "Условие задачи", example = "Даны два целых числа. Выведите их сумму.")
    @NotBlank
    String statement,

    @Schema(description = "Формат входных данных", example = "В одной строке записаны два целых числа a и b.")
    String inputFormat,

    @Schema(description = "Формат выходных данных", example = "Выведите одно целое число.")
    String outputFormat,

    @Schema(description = "Сложность задачи", example = "EASY")
    @NotNull
    TaskDifficulty difficulty,

    @Schema(description = "Идентификатор автора задачи")
    @NotNull
    UUID authorId,

    @Schema(description = "Идентификаторы тем")
    Set<UUID> topicIds,

    @Schema(description = "Идентификаторы поддерживаемых языков программирования")
    Set<Integer> languageIds
) {
}
