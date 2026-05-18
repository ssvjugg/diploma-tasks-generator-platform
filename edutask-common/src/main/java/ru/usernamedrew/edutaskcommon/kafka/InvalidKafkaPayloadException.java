package ru.usernamedrew.edutaskcommon.kafka;

public class InvalidKafkaPayloadException extends RuntimeException {
    public InvalidKafkaPayloadException(String message) {
        super(message);
    }
}
