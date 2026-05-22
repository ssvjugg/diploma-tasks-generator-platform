package ru.usernamedrew.edutaskllmworker.llm;

import org.springframework.stereotype.Component;
import ru.usernamedrew.edutaskllmworker.config.LlmWorkerProperties;
import ru.usernamedrew.edutaskllmworker.config.LlmWorkerProperties.Provider;
import ru.usernamedrew.edutaskllmworker.llm.ollama.OllamaQwenLlmClient;
import ru.usernamedrew.edutaskllmworker.llm.openai.OpenAiCompatibleLlmClient;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
public class LlmClientFactory {
    private final Map<String, LlmClientRegistration> registrations = new LinkedHashMap<>();

    public LlmClientFactory(
        LlmWorkerProperties properties,
        PromptBuilder promptBuilder,
        LlmResponseParser responseParser,
        LlmResponseValidator responseValidator
    ) {
        properties.getProviders().forEach((providerName, provider) ->
            register(providerName, provider, properties, promptBuilder, responseParser, responseValidator));
        if (registrations.isEmpty()) {
            throw new IllegalStateException("At least one LLM provider must be configured");
        }
    }

    public LlmClientRegistration resolveRegistration(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return defaultRegistration();
        }
        LlmClientRegistration registration = registrations.get(normalize(providerName));
        if (registration == null) {
            throw new IllegalArgumentException("Unsupported LLM provider: " + providerName);
        }
        return registration;
    }

    public LlmClient getClient(String providerName) {
        return resolveRegistration(providerName).client();
    }

    public LlmClientRegistration defaultRegistration() {
        return registrations.values().iterator().next();
    }

    public String defaultProviderName() {
        return defaultRegistration().providerName();
    }

    private void register(
        String providerName,
        Provider provider,
        LlmWorkerProperties properties,
        PromptBuilder promptBuilder,
        LlmResponseParser responseParser,
        LlmResponseValidator responseValidator
    ) {
        String normalizedName = normalize(providerName);
        LlmClient client = switch (provider.getType()) {
            case OPENAI_COMPATIBLE -> new OpenAiCompatibleLlmClient(
                normalizedName,
                provider,
                properties,
                promptBuilder,
                responseValidator
            );
            case OLLAMA -> new OllamaQwenLlmClient(
                normalizedName,
                provider,
                properties,
                promptBuilder,
                responseParser,
                responseValidator
            );
        };
        LlmClientRegistration previous = registrations.put(
            normalizedName,
            new LlmClientRegistration(normalizedName, provider.getType(), provider.getModel(), client)
        );
        if (previous != null) {
            throw new IllegalStateException("Duplicate LLM provider configuration: " + normalizedName);
        }
    }

    private String normalize(String providerName) {
        Objects.requireNonNull(providerName, "providerName must not be null");
        String normalized = providerName.trim().toLowerCase();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("providerName must not be blank");
        }
        return normalized;
    }
}
