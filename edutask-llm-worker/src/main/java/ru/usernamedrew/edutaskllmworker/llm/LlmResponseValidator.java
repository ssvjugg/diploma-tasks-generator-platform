package ru.usernamedrew.edutaskllmworker.llm;

import org.springframework.stereotype.Component;
import ru.usernamedrew.edutaskcommon.event.generation.GeneratedTaskDraft;
import ru.usernamedrew.edutaskcommon.event.generation.GeneratedTestCaseDraft;
import ru.usernamedrew.edutaskcommon.event.generation.GeneratedTopicDraft;

@Component
public class LlmResponseValidator {
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_TOPIC_NAME_LENGTH = 255;

    public void validate(GeneratedTaskDraft draft) {
        if (draft == null) {
            throw new LlmResponseValidationException("Generated task draft must not be null");
        }
        requireText(draft.title(), "Generated task title must not be blank");
        requireText(draft.statement(), "Generated task statement must not be blank");
        if (draft.title().length() > MAX_TITLE_LENGTH) {
            throw new LlmResponseValidationException("Generated task title is too long");
        }
        if (draft.difficulty() == null) {
            throw new LlmResponseValidationException("Generated task difficulty must not be null");
        }

        if (draft.topics() != null) {
            for (GeneratedTopicDraft topic : draft.topics()) {
                requireText(topic.name(), "Generated topic name must not be blank");
                if (topic.name().length() > MAX_TOPIC_NAME_LENGTH) {
                    throw new LlmResponseValidationException("Generated topic name is too long");
                }
            }
        }

        if (draft.testCases() != null) {
            for (GeneratedTestCaseDraft testCase : draft.testCases()) {
                if (testCase.inputData() == null) {
                    throw new LlmResponseValidationException("Generated test input must not be null");
                }
                requireText(testCase.expectedOutput(), "Generated expected output must not be blank");
                if (testCase.points() < 0) {
                    throw new LlmResponseValidationException("Generated test points must not be negative");
                }
            }
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new LlmResponseValidationException(message);
        }
    }
}
