package ru.usernamedrew.edutaskcommon.dto.submission;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.usernamedrew.edutaskcommon.event.judge.JudgeSubmissionStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Состояние отправленного решения")
public record CodeSubmissionResponse(
    @Schema(description = "Идентификатор сабмита")
    UUID submissionId,

    @Schema(description = "Идентификатор задачи")
    UUID taskId,

    @Schema(description = "Идентификатор пользователя")
    UUID userId,

    @Schema(description = "Код языка")
    String language,

    @Schema(description = "Исходный код. Возвращается только владельцу или ADMIN.")
    String sourceCode,

    @Schema(description = "Статус проверки")
    JudgeSubmissionStatus status,

    @Schema(description = "Результаты тестов")
    List<CodeSubmissionTestResultResponse> testResults,

    @Schema(description = "Количество пройденных тестов")
    int passedCount,

    @Schema(description = "Общее количество тестов")
    int totalCount,

    @Schema(description = "Набранные баллы")
    int score,

    @Schema(description = "Максимальные баллы")
    int maxScore,

    @Schema(description = "Сообщение об ошибке")
    String errorMessage,

    @Schema(description = "Дата создания")
    OffsetDateTime createdAt,

    @Schema(description = "Дата обновления")
    OffsetDateTime updatedAt
) {
}
