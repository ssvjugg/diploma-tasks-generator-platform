package ru.usernamedrew.edutaskjudgeworker.judge;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record Judge0SubmissionRequest(
    @JsonProperty("source_code")
    String sourceCode,

    @JsonProperty("language_id")
    int languageId,

    String stdin,

    @JsonProperty("expected_output")
    String expectedOutput,

    @JsonProperty("cpu_time_limit")
    BigDecimal cpuTimeLimit,

    @JsonProperty("memory_limit")
    Integer memoryLimit
) {
}
