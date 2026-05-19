package ru.usernamedrew.edutaskllmworker.llm;

public record LlmClientRegistration(
    LlmProviderType providerType,
    LlmClient client
) {
}
