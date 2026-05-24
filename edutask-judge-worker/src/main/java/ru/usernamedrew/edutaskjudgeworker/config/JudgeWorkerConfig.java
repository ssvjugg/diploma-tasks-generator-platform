package ru.usernamedrew.edutaskjudgeworker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties({JudgeWorkerProperties.class, Judge0Properties.class})
public class JudgeWorkerConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
            .findAndAddModules()
            .build();
    }

    @Bean
    public WebClient judge0WebClient(Judge0Properties properties) {
        return WebClient.builder()
            .baseUrl(properties.getBaseUrl())
            .build();
    }
}
