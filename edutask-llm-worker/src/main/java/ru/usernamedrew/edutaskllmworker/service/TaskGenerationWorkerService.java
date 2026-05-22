package ru.usernamedrew.edutaskllmworker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.usernamedrew.edutaskcommon.event.generation.GenerationEventStatus;
import ru.usernamedrew.edutaskcommon.event.generation.TaskGenerationRequestedEvent;
import ru.usernamedrew.edutaskcommon.event.generation.TaskGenerationResponseEvent;
import ru.usernamedrew.edutaskllmworker.config.LlmWorkerProperties;
import ru.usernamedrew.edutaskllmworker.llm.LlmClient;
import ru.usernamedrew.edutaskllmworker.llm.LlmClientFactory;
import ru.usernamedrew.edutaskllmworker.llm.LlmGenerationRequestFactory;
import ru.usernamedrew.edutaskllmworker.llm.LlmGenerationRequest;
import ru.usernamedrew.edutaskllmworker.llm.LlmGenerationResult;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskGenerationWorkerService {
    private final LlmClientFactory clientFactory;
    private final LlmGenerationRequestFactory requestFactory;
    private final LlmWorkerProperties properties;

    public TaskGenerationResponseEvent generate(TaskGenerationRequestedEvent event) {
        try {
            LlmGenerationRequest request = requestFactory.create(event);
            LlmClient client = clientFactory.getClient(request.providerName());
            LlmGenerationResult result = client.generateTask(request);
            return completed(event, result);
        } catch (RuntimeException exception) {
            log.warn("Task generation failed, requestId={}", event.requestId(), exception);
            return failedResponse(event, exception.getMessage());
        }
    }

    private TaskGenerationResponseEvent completed(TaskGenerationRequestedEvent event, LlmGenerationResult result) {
        return new TaskGenerationResponseEvent(
            UUID.randomUUID(),
            event.requestId(),
            GenerationEventStatus.COMPLETED,
            result.provider(),
            result.model(),
            result.draft(),
            null,
            OffsetDateTime.now(),
            properties.getSchemaVersion()
        );
    }

    public TaskGenerationResponseEvent failedResponse(TaskGenerationRequestedEvent event, String errorMessage) {
        return new TaskGenerationResponseEvent(
            UUID.randomUUID(),
            event.requestId(),
            GenerationEventStatus.FAILED,
            requestFactory.failureProvider(event.provider()),
            requestFactory.failureModel(event.model()),
            null,
            errorMessage == null || errorMessage.isBlank() ? "Task generation failed" : errorMessage,
            OffsetDateTime.now(),
            properties.getSchemaVersion()
        );
    }
}
