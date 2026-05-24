package ru.usernamedrew.edutaskcommon.dto.submission;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на отправку решения задачи")
public record CodeSubmissionCreateRequest(
    @Schema(description = "Код языка программирования", example = "python")
    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Must contain only letters, digits, dot, underscore or hyphen")
    String language,

    @Schema(description = "Исходный код решения")
    @NotBlank
    @Size(max = 65536)
    String sourceCode
) {
}
