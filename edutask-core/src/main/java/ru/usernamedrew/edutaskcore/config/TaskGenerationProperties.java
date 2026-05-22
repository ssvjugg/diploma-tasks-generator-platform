package ru.usernamedrew.edutaskcore.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
@ConfigurationProperties(prefix = "edutask.generation")
public class TaskGenerationProperties {
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("2.0")
    private BigDecimal defaultTemperature = BigDecimal.valueOf(0.3);

    @NotNull
    private Duration kafkaSendTimeout = Duration.ofSeconds(10);

    @NotNull
    private Duration sseTimeout = Duration.ofMinutes(10);

    @Min(1)
    private int schemaVersion = 1;
}
