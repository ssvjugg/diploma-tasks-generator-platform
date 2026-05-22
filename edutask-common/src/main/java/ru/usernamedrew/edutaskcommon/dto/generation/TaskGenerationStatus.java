package ru.usernamedrew.edutaskcommon.dto.generation;

public enum TaskGenerationStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}
