package ru.usernamedrew.edutaskllmworker.llm;

import ru.usernamedrew.edutaskcommon.dto.task.TaskDifficulty;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record LlmGenerationRequest(
    UUID requestId,
    String prompt,
    Set<UUID> topicIds,
    TaskDifficulty difficulty,
    LlmProviderType providerType,
    String model,
    BigDecimal temperature
) {
}
