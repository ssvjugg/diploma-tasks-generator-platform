package ru.usernamedrew.edutaskcommon.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Сложность задачи", example = "EASY")
public enum TaskDifficulty {
    EASY,
    MEDIUM,
    HARD
}
