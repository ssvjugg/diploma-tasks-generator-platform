package ru.usernamedrew.edutaskcore.kafka.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.usernamedrew.edutaskcommon.event.judge.CodeSubmissionResultEvent;
import ru.usernamedrew.edutaskcommon.kafka.InvalidKafkaPayloadException;
import ru.usernamedrew.edutaskcore.service.CodeSubmissionResultHandler;

@Component
@RequiredArgsConstructor
public class CodeSubmissionResultConsumer {
    private final CodeSubmissionResultHandler resultHandler;

    @KafkaListener(
        topics = "${edutask.kafka.topics.code-submission-results:code-submission-results}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(CodeSubmissionResultEvent event) {
        if (event == null || event.submissionId() == null) {
            throw new InvalidKafkaPayloadException("Code submission result event must not be null");
        }
        resultHandler.handle(event);
    }
}
