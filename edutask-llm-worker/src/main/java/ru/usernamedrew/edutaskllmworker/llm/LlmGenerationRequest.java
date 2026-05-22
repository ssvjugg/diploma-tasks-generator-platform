package ru.usernamedrew.edutaskllmworker.llm;

import ru.usernamedrew.edutaskcommon.dto.task.TaskDifficulty;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record LlmGenerationRequest(
    UUID requestId,
    String providerName,
    String prompt,
    Set<UUID> topicIds,
    TaskDifficulty difficulty,
    String model,
    BigDecimal temperature
) {
    public LlmGenerationRequest {
        topicIds = topicIds == null ? Set.of() : Set.copyOf(topicIds);
    }
}
