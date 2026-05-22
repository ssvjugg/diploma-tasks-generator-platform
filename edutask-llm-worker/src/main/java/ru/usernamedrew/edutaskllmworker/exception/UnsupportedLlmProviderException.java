package ru.usernamedrew.edutaskllmworker.exception;

public class UnsupportedLlmProviderException extends RuntimeException {
    public UnsupportedLlmProviderException(String message) {
        super(message);
    }
}
