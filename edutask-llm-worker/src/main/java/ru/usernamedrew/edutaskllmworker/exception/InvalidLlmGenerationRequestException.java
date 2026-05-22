package ru.usernamedrew.edutaskllmworker.exception;

public class InvalidLlmGenerationRequestException extends RuntimeException {
    public InvalidLlmGenerationRequestException(String message) {
        super(message);
    }
}
