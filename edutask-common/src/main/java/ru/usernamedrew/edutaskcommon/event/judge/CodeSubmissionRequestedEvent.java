package ru.usernamedrew.edutaskcommon.event.judge;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CodeSubmissionRequestedEvent(
    @NotNull
    UUID eventId,

    @NotNull
    UUID submissionId,

    @NotNull
    UUID taskId,

    @NotNull
    UUID userId,

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Must contain only letters, digits, dot, underscore or hyphen")
    String language,

    @NotBlank
    @Size(max = 65536)
    String sourceCode,

    @Valid
    @NotEmpty
    List<@NotNull JudgeTestCasePayload> testCases,

    @NotNull
    OffsetDateTime createdAt,

    @Min(1)
    int schemaVersion
) {
}
