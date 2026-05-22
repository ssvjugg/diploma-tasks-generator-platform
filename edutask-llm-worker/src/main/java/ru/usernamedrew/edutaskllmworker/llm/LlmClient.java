package ru.usernamedrew.edutaskllmworker.llm;

public interface LlmClient {
    LlmGenerationResult generateTask(LlmGenerationRequest request);
}
