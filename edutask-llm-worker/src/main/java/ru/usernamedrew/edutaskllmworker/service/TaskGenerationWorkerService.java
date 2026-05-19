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
import ru.usernamedrew.edutaskllmworker.llm.LlmGenerationRequest;
import ru.usernamedrew.edutaskllmworker.llm.LlmGenerationResult;
import ru.usernamedrew.edutaskllmworker.llm.LlmProviderType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskGenerationWorkerService {
    private final LlmClientFactory clientFactory;
    private final LlmWorkerProperties properties;

    public TaskGenerationResponseEvent generate(TaskGenerationRequestedEvent event) {
        try {
            LlmProviderType providerType = providerType(event.provider());
            LlmClient client = clientFactory.getClient(providerType);
            LlmGenerationRequest request = new LlmGenerationRequest(
                event.requestId(),
                event.prompt(),
                event.topicIds() == null ? Set.of() : Set.copyOf(event.topicIds()),
                event.difficulty(),
                providerType,
                model(event.model()),
                temperature(event.temperature())
            );
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
            provider(event.provider()),
            model(event.model()),
            null,
            errorMessage == null || errorMessage.isBlank() ? "Task generation failed" : errorMessage,
            OffsetDateTime.now(),
            properties.getSchemaVersion()
        );
    }

    private LlmProviderType providerType(String provider) {
        return LlmProviderType.valueOf(provider(provider));
    }

    private String provider(String provider) {
        return provider == null || provider.isBlank()
            ? properties.getDefaultProvider()
            : provider.trim();
    }

    private String model(String model) {
        return model == null || model.isBlank()
            ? properties.getDefaultModel()
            : model.trim();
    }

    private BigDecimal temperature(BigDecimal temperature) {
        return temperature == null ? properties.getDefaultTemperature() : temperature;
    }
}
