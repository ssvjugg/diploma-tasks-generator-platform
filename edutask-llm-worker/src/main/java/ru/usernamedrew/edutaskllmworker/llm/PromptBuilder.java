package ru.usernamedrew.edutaskllmworker.llm;

import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {
    public String buildSystemPrompt() {
        return """
            You are an assistant that generates educational programming tasks for school students.
            Return only valid JSON. Do not wrap JSON in Markdown. Do not add explanations.
            The response must match this structure:
            {
              "title": "short task title",
              "statement": "problem statement",
              "inputFormat": "input format",
              "outputFormat": "output format",
              "difficulty": "EASY | MEDIUM | HARD",
              "topics": [{"name": "topic name"}],
              "testCases": [
                {
                  "inputData": "sample input",
                  "expectedOutput": "sample output",
                  "hidden": false,
                  "points": 1
                }
              ]
            }
            Use Russian language for title, statement, inputFormat, outputFormat and topic names.
            Keep statements precise and suitable for a programming contest task.
            """;
    }

    public String buildUserPrompt(LlmGenerationRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("User request:\n").append(request.prompt().trim()).append("\n\n");
        if (request.difficulty() != null) {
            prompt.append("Requested difficulty: ").append(request.difficulty()).append("\n");
        }
        if (request.topicIds() != null && !request.topicIds().isEmpty()) {
            prompt.append("Selected topic ids for context: ").append(request.topicIds()).append("\n");
        }
        prompt.append("""

            Generate one task draft.
            Provide at least one visible test case.
            Do not include supported programming languages.
            """);
        return prompt.toString();
    }
}
