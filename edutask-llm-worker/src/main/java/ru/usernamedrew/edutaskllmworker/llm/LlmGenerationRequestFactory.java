package ru.usernamedrew.edutaskllmworker.llm;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.usernamedrew.edutaskcommon.event.generation.TaskGenerationRequestedEvent;
import ru.usernamedrew.edutaskllmworker.config.LlmWorkerProperties;
import ru.usernamedrew.edutaskllmworker.exception.InvalidLlmGenerationRequestException;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class LlmGenerationRequestFactory {
    private final LlmWorkerProperties properties;
    private final LlmClientFactory clientFactory;

    public LlmGenerationRequest create(TaskGenerationRequestedEvent event) {
        String prompt = prompt(event);
        LlmClientRegistration registration = clientFactory.resolveRegistration(event.provider());

        return new LlmGenerationRequest(
            event.requestId(),
            registration.providerName(),
            prompt,
            event.topicIds(),
            event.difficulty(),
            model(event.model(), registration.defaultModel()),
            temperature(event.temperature())
        );
    }

    public String failureProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return clientFactory.defaultProviderName();
        }
        return provider.trim();
    }

    public String failureModel(String model) {
        return valueOrDefault(model, clientFactory.defaultRegistration().defaultModel());
    }

    private String model(String model, String defaultModel) {
        return valueOrDefault(model, defaultModel);
    }

    private BigDecimal temperature(BigDecimal temperature) {
        return temperature == null ? properties.getDefaultTemperature() : temperature;
    }

    private String prompt(TaskGenerationRequestedEvent event) {
        if (event.prompt() == null || event.prompt().isBlank()) {
            throw new InvalidLlmGenerationRequestException("Prompt must not be blank");
        }
        return event.prompt().trim();
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
