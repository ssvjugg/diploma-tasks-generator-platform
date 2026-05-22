package ru.usernamedrew.edutaskllmworker.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import ru.usernamedrew.edutaskllmworker.llm.LlmProviderType;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "edutask.llm-worker")
public class LlmWorkerProperties {
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
    @NotEmpty
    private Map<
        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9._-]+", message = "provider name must contain only letters, digits, dot, underscore or hyphen")
        String,
        @Valid @NotNull Provider
    > providers = new LinkedHashMap<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Provider {
        @NotNull
        private LlmProviderType type;

        @NotBlank
        private String baseUrl;

        private String apiKey;

        @NotBlank
        private String model;

        @Min(1)
        private int maxTokens = 2048;

        @NotNull
        private Duration requestTimeout = Duration.ofSeconds(120);

        @AssertTrue(message = "apiKey must not be blank for remote provider")
        public boolean isApiKeyConfiguredForRemoteProvider() {
            return type == null || type.isLocal() || apiKey != null && !apiKey.isBlank();
        }
    }
}
