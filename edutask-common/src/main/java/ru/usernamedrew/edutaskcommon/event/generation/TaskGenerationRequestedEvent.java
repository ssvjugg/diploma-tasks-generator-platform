package ru.usernamedrew.edutaskcommon.event.generation;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.usernamedrew.edutaskcommon.dto.task.TaskDifficulty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record TaskGenerationRequestedEvent(
    @NotNull
    UUID eventId,

    @NotNull
    UUID requestId,

    @NotNull
    UUID userId,

    @NotBlank
    @Size(max = 4000)
    String prompt,

    Set<@NotNull UUID> topicIds,

    TaskDifficulty difficulty,

    @Size(max = 50)
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Must contain only letters, digits, dot, underscore or hyphen")
    String provider,

    @Size(max = 255)
    @Pattern(regexp = ".*\\S.*", message = "Must contain non-whitespace characters")
    String model,

    @DecimalMin("0.0")
    @DecimalMax("2.0")
    BigDecimal temperature,

    @NotNull
    OffsetDateTime createdAt,

    @Min(1)
    int schemaVersion
) {
}
