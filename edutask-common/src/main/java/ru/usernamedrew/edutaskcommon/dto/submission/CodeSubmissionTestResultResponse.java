package ru.usernamedrew.edutaskcommon.dto.submission;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.usernamedrew.edutaskcommon.event.judge.JudgeSubmissionStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Результат проверки одного тест-кейса")
public record CodeSubmissionTestResultResponse(
    @Schema(description = "Идентификатор результата теста")
    UUID id,

    @Schema(description = "Идентификатор тест-кейса. Для скрытых тестов может не возвращаться.")
    UUID testCaseId,

    @Schema(description = "Порядковый номер теста в проверке")
    int index,

    @Schema(description = "Скрыт ли тест")
    boolean hidden,

    @Schema(description = "Статус теста")
    JudgeSubmissionStatus status,

    @Schema(description = "Входные данные. Не возвращаются для скрытых тестов.")
    String inputData,

    @Schema(description = "Ожидаемый вывод. Не возвращается для скрытых тестов.")
    String expectedOutput,

    @Schema(description = "Фактический stdout. Не возвращается для скрытых тестов.")
    String actualOutput,

    @Schema(description = "stderr. Не возвращается для скрытых тестов.")
    String stderr,

    @Schema(description = "Вывод компилятора. Не возвращается для скрытых тестов.")
    String compileOutput,

    @Schema(description = "Краткое сообщение об ошибке")
    String errorMessage,

    @Schema(description = "Время выполнения в секундах")
    BigDecimal time,

    @Schema(description = "Память в KB")
    Integer memory,

    @Schema(description = "Баллы за тест")
    int points,

    @Schema(description = "Дата создания результата")
    OffsetDateTime createdAt
) {
}
