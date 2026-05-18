package ru.usernamedrew.edutaskcommon.event.generation;

public record GeneratedTestCaseDraft(
    String inputData,
    String expectedOutput,
    boolean hidden,
    int points
) {
}
