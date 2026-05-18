package ru.usernamedrew.edutaskllmworker.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.usernamedrew.edutaskcommon.event.generation.TaskGenerationRequestedEvent;
import ru.usernamedrew.edutaskcommon.kafka.InvalidKafkaPayloadException;
import ru.usernamedrew.edutaskllmworker.service.TaskGenerationQueueManager;

@Component
@RequiredArgsConstructor
public class TaskGenerationRequestConsumer {
    private final TaskGenerationQueueManager queueManager;

    @KafkaListener(
        topics = "${edutask.kafka.topics.task-generation-requests:task-generation-requests}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(TaskGenerationRequestedEvent event) {
        if (event == null || event.requestId() == null) {
            throw new InvalidKafkaPayloadException("Task generation request event must not be null");
        }
        queueManager.process(event);
    }
}
