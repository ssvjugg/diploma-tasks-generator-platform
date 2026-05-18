package ru.usernamedrew.edutaskcommon.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.usernamedrew.edutaskcommon.dto.topic.TopicSummary;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Задача из банка задач")
public record TaskResponse(
    @Schema(description = "Идентификатор задачи")
    UUID id,

    @Schema(description = "Название задачи", example = "Сумма двух чисел")
    String title,

    @Schema(description = "Условие задачи")
    String statement,

    @Schema(description = "Формат входных данных")
    String inputFormat,

    @Schema(description = "Формат выходных данных")
    String outputFormat,

    @Schema(description = "Сложность задачи", example = "EASY")
    TaskDifficulty difficulty,

    @Schema(description = "Идентификатор автора")
    UUID authorId,

    @Schema(description = "Темы задачи")
    Set<TopicSummary> topics,

    @Schema(description = "Дата создания")
    OffsetDateTime createdAt,

    @Schema(description = "Дата обновления")
    OffsetDateTime updatedAt
) {
}
