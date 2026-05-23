package ru.usernamedrew.edutaskcommon.event.judge;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record JudgeTestCaseResult(
    UUID testCaseId,

    @Min(0)
    int index,

    String judge0Token,

    @NotNull
    JudgeSubmissionStatus status,

    String stdout,

    String stderr,

    String compileOutput,

    String errorMessage,

    BigDecimal time,

    Integer memory
) {
}
