package ru.usernamedrew.edutaskcore.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.usernamedrew.edutaskcommon.event.generation.TaskGenerationResponseEvent;

@Slf4j
@Service
public class TaskGenerationResponseHandler {
    public void handle(TaskGenerationResponseEvent event) {
        log.info(
            "Received task generation response, requestId={}, status={}",
            event.requestId(),
            event.status()
        );
    }
}
