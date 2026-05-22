package ru.usernamedrew.edutaskcommon.dto.testcase;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Запрос на обновление тест-кейса задачи")
public record TestCaseUpdateRequest(
    @Schema(description = "Входные данные тест-кейса", example = "2 3")
    String inputData,

    @Schema(description = "Ожидаемый вывод", example = "5")
    String expectedOutput,

    @Schema(description = "Скрыт ли тест-кейс от пользователей без прав управления задачами", example = "true")
    Boolean hidden,

    @Schema(description = "Количество баллов за тест-кейс", example = "2")
    @PositiveOrZero
    Integer points
) {
}
