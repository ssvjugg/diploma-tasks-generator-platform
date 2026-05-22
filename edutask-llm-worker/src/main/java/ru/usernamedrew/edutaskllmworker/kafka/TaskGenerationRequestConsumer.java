package ru.usernamedrew.edutaskllmworker.kafka;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.usernamedrew.edutaskcommon.event.generation.TaskGenerationRequestedEvent;
import ru.usernamedrew.edutaskcommon.kafka.InvalidKafkaPayloadException;
import ru.usernamedrew.edutaskllmworker.service.TaskGenerationQueueManager;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TaskGenerationRequestConsumer {
    private final TaskGenerationQueueManager queueManager;
    private final Validator validator;

    @KafkaListener(
        topics = "${edutask.kafka.topics.task-generation-requests:task-generation-requests}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(TaskGenerationRequestedEvent event) {
        if (event == null) {
            throw new InvalidKafkaPayloadException("Task generation request event must not be null");
        }
        validate(event);
        queueManager.process(event);
    }

    private void validate(TaskGenerationRequestedEvent event) {
        Set<ConstraintViolation<TaskGenerationRequestedEvent>> violations = validator.validate(event);
        if (violations.isEmpty()) {
            return;
        }

        String message = violations.stream()
            .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
            .sorted()
            .collect(Collectors.joining("; "));
        throw new InvalidKafkaPayloadException("Invalid task generation request event: " + message);
    }
}
