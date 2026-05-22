package ru.usernamedrew.edutaskcommon.kafka;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "edutask.kafka")
public class TaskGenerationKafkaProperties {
    @Valid
    @NotNull
    private Topics topics = new Topics();

    @Min(1)
    private int partitions = 3;

    @Min(1)
    @Max(500)
    private short replicationFactor = 1;

    @Min(1)
    private int consumerConcurrency = 2;

    @Valid
    @NotNull
    private Retry retry = new Retry();

    @Getter
    @Setter
    public static class Topics {
        @NotNull
        @NotBlank
        private String taskGenerationRequests = "task-generation-requests";

        @NotNull
        @NotBlank
        private String taskGenerationResponses = "task-generation-responses";

        @NotNull
        @NotBlank
        private String taskGenerationRequestsDlt = "task-generation-requests.dlt";

        @NotNull
        @NotBlank
        private String taskGenerationResponsesDlt = "task-generation-responses.dlt";
    }

    @Getter
    @Setter
    public static class Retry {
        @Min(1)
        private int maxAttempts = 3;

        @NotNull
        private Duration backoff = Duration.ofSeconds(2);
    }
}
