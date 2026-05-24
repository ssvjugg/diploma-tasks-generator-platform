package ru.usernamedrew.edutaskjudgeworker.judge;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Judge0SubmissionResult(
    String token,
    String stdout,
    String stderr,

    @JsonProperty("compile_output")
    String compileOutput,

    String message,
    Judge0Status status,
    String time,
    Integer memory
) {
    public boolean isFinished() {
        return status != null && status.id() != null && status.id() > 2;
    }
}
