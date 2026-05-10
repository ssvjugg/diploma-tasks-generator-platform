package ru.usernamedrew.edutaskcore.repository.projection;

import ru.usernamedrew.edutaskcommon.dto.task.TaskDifficulty;

import java.util.UUID;

public interface TaskSummaryProjection {
    UUID getId();

    String getTitle();

    TaskDifficulty getDifficulty();
}
