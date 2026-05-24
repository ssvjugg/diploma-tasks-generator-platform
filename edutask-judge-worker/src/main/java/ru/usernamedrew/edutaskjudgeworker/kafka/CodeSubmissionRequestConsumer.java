package ru.usernamedrew.edutaskjudgeworker.kafka;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.usernamedrew.edutaskcommon.event.judge.CodeSubmissionRequestedEvent;
import ru.usernamedrew.edutaskcommon.kafka.InvalidKafkaPayloadException;
import ru.usernamedrew.edutaskjudgeworker.service.CodeJudgingQueueManager;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CodeSubmissionRequestConsumer {
    private final CodeJudgingQueueManager queueManager;
    private final Validator validator;

    @KafkaListener(
        topics = "${edutask.kafka.topics.code-submission-requests:code-submission-requests}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(CodeSubmissionRequestedEvent event) {
        if (event == null) {
            throw new InvalidKafkaPayloadException("Code submission request event must not be null");
        }
        validate(event);
        queueManager.process(event);
    }

    private void validate(CodeSubmissionRequestedEvent event) {
        Set<ConstraintViolation<CodeSubmissionRequestedEvent>> violations = validator.validate(event);
        if (violations.isEmpty()) {
            return;
        }

        String message = violations.stream()
            .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
            .sorted()
            .collect(Collectors.joining("; "));
        throw new InvalidKafkaPayloadException("Invalid code submission request event: " + message);
    }
}
