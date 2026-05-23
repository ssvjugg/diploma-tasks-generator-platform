package ru.usernamedrew.edutaskcommon.event.judge;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CodeSubmissionResultEvent(
    @NotNull
    UUID eventId,

    @NotNull
    UUID submissionId,

    @NotNull
    UUID taskId,

    @NotNull
    UUID userId,

    @NotNull
    JudgeSubmissionStatus status,

    @Valid
    List<@NotNull JudgeTestCaseResult> testResults,

    @PositiveOrZero
    int passedCount,

    @PositiveOrZero
    int totalCount,

    @PositiveOrZero
    int score,

    @PositiveOrZero
    int maxScore,

    String errorMessage,

    @NotNull
    OffsetDateTime createdAt,

    @Min(1)
    int schemaVersion
) {
}
