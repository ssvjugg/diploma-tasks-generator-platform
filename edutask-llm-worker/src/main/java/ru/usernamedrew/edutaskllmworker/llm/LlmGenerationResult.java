package ru.usernamedrew.edutaskllmworker.llm;

import ru.usernamedrew.edutaskcommon.event.generation.GeneratedTaskDraft;

import java.time.Duration;

public record LlmGenerationResult(
    GeneratedTaskDraft draft,
    String provider,
    String model,
    Duration duration
) {
}
