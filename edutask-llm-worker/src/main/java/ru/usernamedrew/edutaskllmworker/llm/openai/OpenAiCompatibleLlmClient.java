package ru.usernamedrew.edutaskllmworker.llm.openai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import ru.usernamedrew.edutaskcommon.event.generation.GeneratedTaskDraft;
import ru.usernamedrew.edutaskllmworker.config.LlmWorkerProperties;
import ru.usernamedrew.edutaskllmworker.config.LlmWorkerProperties.Provider;
import ru.usernamedrew.edutaskllmworker.llm.LlmClient;
import ru.usernamedrew.edutaskllmworker.llm.LlmClientException;
import ru.usernamedrew.edutaskllmworker.llm.LlmGenerationRequest;
import ru.usernamedrew.edutaskllmworker.llm.LlmGenerationResult;
import ru.usernamedrew.edutaskllmworker.llm.LlmResponseValidationException;
import ru.usernamedrew.edutaskllmworker.llm.LlmResponseValidator;
import ru.usernamedrew.edutaskllmworker.llm.PromptBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

public class OpenAiCompatibleLlmClient implements LlmClient {
    private final String providerName;
    private final Provider provider;
    private final LlmWorkerProperties properties;
    private final PromptBuilder promptBuilder;
    private final LlmResponseValidator responseValidator;

    public OpenAiCompatibleLlmClient(
        String providerName,
        Provider provider,
        LlmWorkerProperties properties,
        PromptBuilder promptBuilder,
        LlmResponseValidator responseValidator
    ) {
        this.providerName = providerName;
        this.provider = provider;
        this.properties = properties;
        this.promptBuilder = promptBuilder;
        this.responseValidator = responseValidator;
    }

    @Override
    public LlmGenerationResult generateTask(LlmGenerationRequest request) {
        Instant startedAt = Instant.now();
        String model = request.model() == null || request.model().isBlank()
            ? provider.getModel()
            : request.model().trim();
        String apiKey = apiKey();

        try {
            GeneratedTaskDraft draft = chatClient(provider, apiKey, model, request.temperature())
                .prompt()
                .system(promptBuilder.buildSystemPrompt())
                .user(promptBuilder.buildUserPrompt(request))
                .call()
                .entity(GeneratedTaskDraft.class);

            responseValidator.validate(draft);
            return new LlmGenerationResult(
                draft,
                providerName,
                model,
                Duration.between(startedAt, Instant.now())
            );
        } catch (LlmResponseValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new LlmClientException("Failed to call OpenAI-compatible model", exception);
        }
    }

    private ChatClient chatClient(
        Provider provider,
        String apiKey,
        String model,
        BigDecimal temperature
    ) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .baseUrl(provider.getBaseUrl())
            .apiKey(apiKey)
            .model(model)
            .temperature(temperature(temperature))
            .maxTokens(provider.getMaxTokens())
            .timeout(provider.getRequestTimeout())
            .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
            .options(options)
            .build();

        return ChatClient.create(chatModel);
    }

    private String apiKey() {
        String apiKey = provider.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmClientException(providerName + " API key is not configured", null);
        }
        return apiKey.trim();
    }

    private Double temperature(BigDecimal requestedTemperature) {
        BigDecimal value = requestedTemperature == null
            ? properties.getDefaultTemperature()
            : requestedTemperature;
        return value.doubleValue();
    }
}
