package ru.usernamedrew.edutaskcommon.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Краткое описание языка программирования")
public record ProgrammingLanguageSummary(
    @Schema(description = "Идентификатор языка", example = "1")
    Integer id,

    @Schema(description = "Название языка", example = "Java 21")
    String name,

    @Schema(description = "Код языка", example = "java")
    String code,

    @Schema(description = "Идентификатор языка в Judge0", example = "91")
    Integer judge0LanguageId
) {
}
