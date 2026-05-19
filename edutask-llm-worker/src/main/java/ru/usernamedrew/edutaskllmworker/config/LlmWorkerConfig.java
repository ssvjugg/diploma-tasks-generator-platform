package ru.usernamedrew.edutaskllmworker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(LlmWorkerProperties.class)
public class LlmWorkerConfig {
    @Bean
    @Qualifier("ollamaWebClient")
    public WebClient ollamaWebClient(LlmWorkerProperties properties) {
        return WebClient.builder()
            .baseUrl(properties.getOllama().getBaseUrl())
            .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
            .findAndAddModules()
            .build();
    }
}
