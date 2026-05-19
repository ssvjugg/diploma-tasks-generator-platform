package ru.usernamedrew.edutaskllmworker.llm;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class LlmClientFactory {
    private final Map<LlmProviderType, LlmClient> clients = new EnumMap<>(LlmProviderType.class);

    public LlmClientFactory(List<LlmClientRegistration> registrations) {
        for (LlmClientRegistration registration : registrations) {
            clients.put(registration.providerType(), registration.client());
        }
    }

    public LlmClient getClient(LlmProviderType providerType) {
        LlmClient client = clients.get(providerType);
        if (client == null) {
            throw new IllegalArgumentException("Unsupported LLM provider: " + providerType);
        }
        return client;
    }
}
