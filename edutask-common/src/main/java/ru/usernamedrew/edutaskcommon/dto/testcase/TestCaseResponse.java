package ru.usernamedrew.edutaskcommon.dto.testcase;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Тест-кейс задачи")
public record TestCaseResponse(
    @Schema(description = "Идентификатор тест-кейса")
    UUID id,

    @Schema(description = "Идентификатор задачи")
    UUID taskId,

    @Schema(description = "Входные данные тест-кейса")
    String inputData,

    @Schema(description = "Ожидаемый вывод")
    String expectedOutput,

    @Schema(description = "Скрыт ли тест-кейс от пользователей без прав управления задачами")
    boolean hidden,

    @Schema(description = "Количество баллов за тест-кейс")
    int points,

    @Schema(description = "Дата создания")
    OffsetDateTime createdAt,

    @Schema(description = "Дата обновления")
    OffsetDateTime updatedAt
) {
}
