package ru.usernamedrew.edutaskllmworker.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import ru.usernamedrew.edutaskcommon.event.generation.TaskGenerationResponseEvent;
import ru.usernamedrew.edutaskcommon.kafka.TaskGenerationKafkaProperties;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskGenerationResponseProducer {
    // TODO Подумать о том верное ли решение, использовать <Object, Object> вместо конкретной типизации
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final TaskGenerationKafkaProperties properties;

    public CompletableFuture<SendResult<Object, Object>> send(TaskGenerationResponseEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(event.requestId(), "event.requestId must not be null");

        String topic = properties.getTopics().getTaskGenerationResponses();
        String key = event.requestId().toString();
        return kafkaTemplate.send(topic, key, event)
            .whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to publish task generation response, requestId={}", event.requestId(), exception);
                    return;
                }
                log.debug(
                    "Published task generation response, requestId={}, topic={}, partition={}, offset={}",
                    event.requestId(),
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
                );
            });
    }
}
