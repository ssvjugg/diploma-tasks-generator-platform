package ru.usernamedrew.edutaskcore.config;

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
@ConfigurationProperties(prefix = "edutask.submissions")
public class CodeSubmissionProperties {
    @NotNull
    private Duration kafkaSendTimeout = Duration.ofSeconds(10);

    @NotNull
    private Duration sseTimeout = Duration.ofMinutes(10);

    @Min(1)
    private int schemaVersion = 1;
}
