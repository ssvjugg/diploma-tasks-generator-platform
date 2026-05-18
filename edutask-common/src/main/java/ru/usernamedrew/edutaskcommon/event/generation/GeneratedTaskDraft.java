package ru.usernamedrew.edutaskcommon.event.generation;

import ru.usernamedrew.edutaskcommon.dto.task.TaskDifficulty;

import java.util.List;

public record GeneratedTaskDraft(
    String title,
    String statement,
    String inputFormat,
    String outputFormat,
    TaskDifficulty difficulty,
    List<GeneratedTopicDraft> topics,
    List<GeneratedTestCaseDraft> testCases
) {
}
