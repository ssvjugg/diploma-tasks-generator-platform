package ru.usernamedrew.edutaskjudgeworker.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "edutask.judge-worker")
public class JudgeWorkerProperties {
    @Min(1)
    private int maxConcurrentSubmissions = 1;

    @Min(1)
    private int schemaVersion = 1;

    @NotNull
    private Duration responseSendTimeout = Duration.ofSeconds(10);

    @NotNull
    private Duration concurrencyAcquireTimeout = Duration.ofSeconds(30);
}
