package ru.usernamedrew.edutaskjudgeworker.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "edutask.judge-worker.judge0")
public class Judge0Properties {
    @NotBlank
    private String baseUrl = "http://localhost:2358";

    @NotNull
    private Duration requestTimeout = Duration.ofSeconds(10);

    @NotNull
    private Duration pollingInterval = Duration.ofMillis(500);

    @NotNull
    private Duration pollingTimeout = Duration.ofSeconds(60);

    @NotNull
    @DecimalMin("0.001")
    private BigDecimal defaultCpuTimeLimit = BigDecimal.valueOf(2);

    @Min(1)
    private int defaultMemoryLimit = 131072;

    @Valid
    @NotEmpty
    private Map<
        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9._-]+", message = "language code must contain only letters, digits, dot, underscore or hyphen")
        String,
        @NotNull @Min(1) Integer
    > languages = new LinkedHashMap<>();
}
