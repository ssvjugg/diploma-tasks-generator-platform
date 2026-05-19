package ru.usernamedrew.edutaskllmworker.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "edutask.llm-worker")
public class LlmWorkerProperties {
    @NotBlank
    private String defaultProvider = "DEFAULT_QWEN";

    @NotBlank
    private String defaultModel = "qwen2.5-coder:7b";

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("2.0")
    private BigDecimal defaultTemperature = BigDecimal.valueOf(0.3);

    @Min(1)
    private int maxConcurrentGenerations = 2;

    @Min(1)
    private int schemaVersion = 1;

    @NotNull
    private Duration responseSendTimeout = Duration.ofSeconds(10);

    @NotNull
    private Duration concurrencyAcquireTimeout = Duration.ofSeconds(30);

    @Valid
    @NotNull
    private Ollama ollama = new Ollama();

    @Getter
    @Setter
    public static class Ollama {
        @NotBlank
        private String baseUrl = "http://localhost:11434";

        @NotNull
        private Duration requestTimeout = Duration.ofSeconds(120);
    }
}
