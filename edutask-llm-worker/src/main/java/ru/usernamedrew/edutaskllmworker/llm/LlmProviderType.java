package ru.usernamedrew.edutaskllmworker.llm;

public enum LlmProviderType {
    OLLAMA,
    OPENAI_COMPATIBLE;

    public boolean isLocal() {
        return this == OLLAMA;
    }
}
