package ru.usernamedrew.edutaskcommon.dto.generation;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.usernamedrew.edutaskcommon.event.generation.GeneratedTaskDraft;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Состояние запроса генерации задачи")
public record TaskGenerationResponse(
    @Schema(description = "Идентификатор запроса генерации")
    UUID requestId,

    @Schema(description = "Статус генерации")
    TaskGenerationStatus status,

    @Schema(description = "LLM provider")
    String provider,

    @Schema(description = "Название модели")
    String model,

    @Schema(description = "Сгенерированный черновик задачи")
    GeneratedTaskDraft result,

    @Schema(description = "Текст ошибки")
    String errorMessage,

    @Schema(description = "Дата создания")
    OffsetDateTime createdAt,

    @Schema(description = "Дата обновления")
    OffsetDateTime updatedAt
) {
}
