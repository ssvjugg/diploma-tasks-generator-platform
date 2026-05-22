package ru.usernamedrew.edutaskllmworker.llm.ollama;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import ru.usernamedrew.edutaskcommon.event.generation.GeneratedTaskDraft;
import ru.usernamedrew.edutaskllmworker.config.LlmWorkerProperties;
import ru.usernamedrew.edutaskllmworker.config.LlmWorkerProperties.Provider;
import ru.usernamedrew.edutaskllmworker.llm.LlmClient;
import ru.usernamedrew.edutaskllmworker.llm.LlmClientException;
import ru.usernamedrew.edutaskllmworker.llm.LlmGenerationRequest;
import ru.usernamedrew.edutaskllmworker.llm.LlmGenerationResult;
import ru.usernamedrew.edutaskllmworker.llm.LlmResponseParser;
import ru.usernamedrew.edutaskllmworker.llm.LlmResponseValidator;
import ru.usernamedrew.edutaskllmworker.llm.PromptBuilder;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;

public class OllamaQwenLlmClient implements LlmClient {
    private final String providerName;
    private final Provider provider;
    private final ChatClient chatClient;
    private final LlmWorkerProperties properties;
    private final PromptBuilder promptBuilder;
    private final LlmResponseParser responseParser;
    private final LlmResponseValidator responseValidator;

    public OllamaQwenLlmClient(
        String providerName,
        Provider provider,
        LlmWorkerProperties properties,
        PromptBuilder promptBuilder,
        LlmResponseParser responseParser,
        LlmResponseValidator responseValidator
    ) {
        this.providerName = providerName;
        this.provider = provider;
        this.chatClient = createChatClient(provider);
        this.properties = properties;
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
        this.responseValidator = responseValidator;
    }

    @Override
    public LlmGenerationResult generateTask(LlmGenerationRequest request) {
        Instant startedAt = Instant.now();
        String model = request.model() == null || request.model().isBlank()
            ? provider.getModel()
            : request.model().trim();

        String response;
        try {
            response = chatClient
                .prompt()
                .options(ollamaOptions(model, request.temperature()))
                .system(promptBuilder.buildSystemPrompt())
                .user(promptBuilder.buildUserPrompt(request))
                .call()
                .content();
        } catch (RuntimeException exception) {
            throw new LlmClientException("Failed to call Ollama model", exception);
        }

        if (response == null || response.isBlank()) {
            throw new LlmClientException("Ollama returned empty response", null);
        }

        GeneratedTaskDraft draft = responseParser.parseTaskDraft(response);
        responseValidator.validate(draft);
        return new LlmGenerationResult(
            draft,
            providerName,
            model,
            Duration.between(startedAt, Instant.now())
        );
    }

    private ChatClient createChatClient(Provider provider) {
        OllamaApi ollamaApi = OllamaApi.builder()
            .baseUrl(provider.getBaseUrl())
            .restClientBuilder(restClientBuilder(provider.getRequestTimeout()))
            .build();

        OllamaChatModel chatModel = OllamaChatModel.builder()
            .ollamaApi(ollamaApi)
            .build();

        return ChatClient.create(chatModel);
    }

    private RestClient.Builder restClientBuilder(Duration requestTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(requestTimeout)
            .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(requestTimeout);

        return RestClient.builder()
            .requestFactory(requestFactory);
    }

    private OllamaChatOptions.Builder ollamaOptions(String model, BigDecimal requestedTemperature) {
        OllamaChatOptions.Builder options = OllamaChatOptions.builder();
        options.model(model);
        options.temperature(temperature(requestedTemperature).doubleValue());
        options.maxTokens(provider.getMaxTokens());
        return options;
    }

    private BigDecimal temperature(BigDecimal requestedTemperature) {
        return requestedTemperature == null ? properties.getDefaultTemperature() : requestedTemperature;
    }
}
