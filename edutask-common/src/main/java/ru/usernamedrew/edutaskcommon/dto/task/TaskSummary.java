package ru.usernamedrew.edutaskcommon.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Краткое описание задачи для списков")
public record TaskSummary(
    @Schema(description = "Идентификатор задачи")
    UUID id,

    @Schema(description = "Название задачи", example = "Сумма двух чисел")
    String title,

    @Schema(description = "Сложность задачи", example = "EASY")
    TaskDifficulty difficulty
) {
}
