package ru.usernamedrew.edutaskcommon.event.judge;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record JudgeTestCasePayload(
    UUID testCaseId,

    @Min(0)
    int index,

    @NotNull
    String inputData,

    @NotNull
    String expectedOutput,

    boolean hidden,

    @PositiveOrZero
    int points,

    @PositiveOrZero
    BigDecimal cpuTimeLimit,

    @PositiveOrZero
    Integer memoryLimit
) {
}
