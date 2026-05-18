package ru.usernamedrew.edutaskcore.kafka.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.usernamedrew.edutaskcommon.event.generation.TaskGenerationResponseEvent;
import ru.usernamedrew.edutaskcommon.kafka.InvalidKafkaPayloadException;
import ru.usernamedrew.edutaskcore.service.TaskGenerationResponseHandler;

@Component
@RequiredArgsConstructor
public class TaskGenerationResponseConsumer {
    private final TaskGenerationResponseHandler responseHandler;

    @KafkaListener(
        topics = "${edutask.kafka.topics.task-generation-responses:task-generation-responses}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(TaskGenerationResponseEvent event) {
        if (event == null || event.requestId() == null) {
            throw new InvalidKafkaPayloadException("Task generation response event must not be null");
        }
        responseHandler.handle(event);
    }
}
