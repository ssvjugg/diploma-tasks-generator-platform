package ru.usernamedrew.edutaskllmworker.llm;

public record LlmClientRegistration(
    String providerName,
    LlmProviderType providerType,
    String defaultModel,
    LlmClient client
) {
}
