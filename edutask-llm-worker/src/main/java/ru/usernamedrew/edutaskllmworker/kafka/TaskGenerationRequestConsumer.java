package ru.usernamedrew.edutaskllmworker.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.usernamedrew.edutaskcommon.event.generation.TaskGenerationRequestedEvent;
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
        if (event.requestId() == null) {
            throw new IllegalArgumentException("Task generation request requestId must not be null");
        }
        queueManager.process(event);
    }
}
