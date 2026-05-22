package ru.usernamedrew.edutaskcommon.dto.testcase;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Запрос на создание тест-кейса задачи")
public record TestCaseCreateRequest(
    @Schema(description = "Входные данные тест-кейса", example = "2 3")
    @NotNull
    String inputData,

    @Schema(description = "Ожидаемый вывод", example = "5")
    @NotNull
    String expectedOutput,

    @Schema(description = "Скрыт ли тест-кейс от пользователей без прав управления задачами", example = "false")
    Boolean hidden,

    @Schema(description = "Количество баллов за тест-кейс", example = "1")
    @PositiveOrZero
    Integer points
) {
}
