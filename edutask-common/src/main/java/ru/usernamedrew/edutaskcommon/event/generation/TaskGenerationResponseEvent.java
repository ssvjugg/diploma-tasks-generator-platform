package ru.usernamedrew.edutaskcommon.event.generation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TaskGenerationResponseEvent(
    UUID eventId,
    UUID requestId,
    GenerationEventStatus status,
    String provider,
    String model,
    GeneratedTaskDraft result,
    String errorMessage,
    OffsetDateTime createdAt,
    int schemaVersion
) {
}
