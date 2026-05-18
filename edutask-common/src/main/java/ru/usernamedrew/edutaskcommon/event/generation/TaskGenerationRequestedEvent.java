package ru.usernamedrew.edutaskcommon.event.generation;

import ru.usernamedrew.edutaskcommon.dto.task.TaskDifficulty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record TaskGenerationRequestedEvent(
    UUID eventId,
    UUID requestId,
    UUID userId,
    String prompt,
    Set<UUID> topicIds,
    TaskDifficulty difficulty,
    String provider,
    String model,
    BigDecimal temperature,
    OffsetDateTime createdAt,
    int schemaVersion
) {
}
