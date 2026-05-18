package ru.usernamedrew.edutaskllmworker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.usernamedrew.edutaskcommon.event.generation.TaskGenerationRequestedEvent;

@Slf4j
@Service
public class TaskGenerationQueueManager {
    public void process(TaskGenerationRequestedEvent event) {
        log.info(
            "Received task generation request, requestId={}, provider={}, model={}",
            event.requestId(),
            event.provider(),
            event.model()
        );
    }
}
