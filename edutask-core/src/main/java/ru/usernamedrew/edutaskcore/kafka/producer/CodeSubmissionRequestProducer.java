package ru.usernamedrew.edutaskcore.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import ru.usernamedrew.edutaskcommon.event.judge.CodeSubmissionRequestedEvent;
import ru.usernamedrew.edutaskcommon.kafka.TaskGenerationKafkaProperties;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodeSubmissionRequestProducer {
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final TaskGenerationKafkaProperties properties;

    public CompletableFuture<SendResult<Object, Object>> send(CodeSubmissionRequestedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(event.submissionId(), "event.submissionId must not be null");

        String topic = properties.getTopics().getCodeSubmissionRequests();
        String key = event.submissionId().toString();
        return kafkaTemplate.send(topic, key, event)
            .whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to publish code submission request, submissionId={}", event.submissionId(), exception);
                    return;
                }
                log.debug(
                    "Published code submission request, submissionId={}, topic={}, partition={}, offset={}",
                    event.submissionId(),
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
                );
            });
    }
}
