package ru.usernamedrew.edutaskllmworker.llm;

public class LlmClientException extends RuntimeException {
    public LlmClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
