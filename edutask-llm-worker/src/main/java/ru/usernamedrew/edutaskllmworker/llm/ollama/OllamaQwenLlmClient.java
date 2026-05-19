package ru.usernamedrew.edutaskllmworker.llm.ollama;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.usernamedrew.edutaskcommon.event.generation.GeneratedTaskDraft;
import ru.usernamedrew.edutaskllmworker.config.LlmWorkerProperties;
import ru.usernamedrew.edutaskllmworker.llm.LlmClient;
import ru.usernamedrew.edutaskllmworker.llm.LlmClientException;
import ru.usernamedrew.edutaskllmworker.llm.LlmClientRegistration;
import ru.usernamedrew.edutaskllmworker.llm.LlmGenerationRequest;
import ru.usernamedrew.edutaskllmworker.llm.LlmGenerationResult;
import ru.usernamedrew.edutaskllmworker.llm.LlmProviderType;
import ru.usernamedrew.edutaskllmworker.llm.LlmResponseParser;
import ru.usernamedrew.edutaskllmworker.llm.LlmResponseValidator;
import ru.usernamedrew.edutaskllmworker.llm.PromptBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class OllamaQwenLlmClient implements LlmClient {
    private final WebClient webClient;
    private final LlmWorkerProperties properties;
    private final PromptBuilder promptBuilder;
    private final LlmResponseParser responseParser;
    private final LlmResponseValidator responseValidator;

    public OllamaQwenLlmClient(
        @Qualifier("ollamaWebClient") WebClient webClient,
        LlmWorkerProperties properties,
        PromptBuilder promptBuilder,
        LlmResponseParser responseParser,
        LlmResponseValidator responseValidator
    ) {
        this.webClient = webClient;
        this.properties = properties;
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
        this.responseValidator = responseValidator;
    }

    @Override
    public LlmGenerationResult generateTask(LlmGenerationRequest request) {
        Instant startedAt = Instant.now();
        String model = request.model() == null || request.model().isBlank()
            ? properties.getDefaultModel()
            : request.model().trim();

        OllamaChatRequest ollamaRequest = new OllamaChatRequest(
            model,
            List.of(
                new OllamaMessage("system", promptBuilder.buildSystemPrompt()),
                new OllamaMessage("user", promptBuilder.buildUserPrompt(request))
            ),
            false,
            Map.of("temperature", temperature(request.temperature()))
        );

        OllamaChatResponse response;
        try {
            response = webClient.post()
                .uri("/api/chat")
                .bodyValue(ollamaRequest)
                .retrieve()
                .bodyToMono(OllamaChatResponse.class)
                .timeout(properties.getOllama().getRequestTimeout())
                .block(properties.getOllama().getRequestTimeout().plusSeconds(1));
        } catch (RuntimeException exception) {
            throw new LlmClientException("Failed to call Ollama Qwen model", exception);
        }

        if (response == null || response.message() == null || response.message().content() == null) {
            throw new LlmClientException("Ollama returned empty response", null);
        }

        GeneratedTaskDraft draft = responseParser.parseTaskDraft(response.message().content());
        responseValidator.validate(draft);
        return new LlmGenerationResult(
            draft,
            request.providerType().name(),
            model,
            Duration.between(startedAt, Instant.now())
        );
    }

    @Bean
    public LlmClientRegistration defaultQwenRegistration(OllamaQwenLlmClient client) {
        return new LlmClientRegistration(LlmProviderType.DEFAULT_QWEN, client);
    }

    @Bean
    public LlmClientRegistration ollamaRegistration(OllamaQwenLlmClient client) {
        return new LlmClientRegistration(LlmProviderType.OLLAMA, client);
    }

    private BigDecimal temperature(BigDecimal requestedTemperature) {
        return requestedTemperature == null ? properties.getDefaultTemperature() : requestedTemperature;
    }

    private record OllamaChatRequest(
        String model,
        List<OllamaMessage> messages,
        boolean stream,
        Map<String, Object> options
    ) {
    }

    private record OllamaMessage(
        String role,
        String content
    ) {
    }

    private record OllamaChatResponse(
        OllamaMessage message
    ) {
    }
}
