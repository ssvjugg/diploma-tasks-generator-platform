package ru.usernamedrew.edutaskcommon.dto.generation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.usernamedrew.edutaskcommon.dto.task.TaskDifficulty;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Запрос на генерацию черновика задачи")
public record TaskGenerationCreateRequest(
    @Schema(description = "Промпт преподавателя", example = "Сгенерируй задачу на циклы для 7 класса")
    @NotBlank
    @Size(max = 4000)
    String prompt,

    @Schema(description = "Идентификаторы тем, выбранных пользователем")
    Set<UUID> topicIds,

    @Schema(description = "Желаемая сложность задачи", example = "EASY")
    TaskDifficulty difficulty,

    @Schema(description = "Имя LLM provider из конфигурации worker-а", example = "openai")
    @Size(max = 50)
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Must contain only letters, digits, dot, underscore or hyphen")
    String provider,

    @Schema(description = "Название модели", example = "openai/gpt-5.4-nano")
    @Size(max = 255)
    @Pattern(regexp = ".*\\S.*", message = "Must contain non-whitespace characters")
    String model,

    @Schema(description = "Температура генерации", example = "0.3")
    @DecimalMin("0.0")
    @DecimalMax("2.0")
    BigDecimal temperature
) {
}
