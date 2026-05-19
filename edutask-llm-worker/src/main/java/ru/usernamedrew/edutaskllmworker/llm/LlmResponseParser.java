package ru.usernamedrew.edutaskllmworker.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import ru.usernamedrew.edutaskcommon.event.generation.GeneratedTaskDraft;

@Component
public class LlmResponseParser {
    private final ObjectMapper llmResponseObjectMapper;

    public LlmResponseParser(ObjectMapper objectMapper) {
        this.llmResponseObjectMapper = objectMapper.copy()
            .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
    }

    public GeneratedTaskDraft parseTaskDraft(String content) {
        String json = extractJson(content);
        try {
            return llmResponseObjectMapper.readValue(json, GeneratedTaskDraft.class);
        } catch (JsonProcessingException exception) {
            throw new LlmResponseValidationException("LLM response is not a valid task draft JSON", exception);
        }
    }

    private String extractJson(String content) {
        if (content == null || content.isBlank()) {
            throw new LlmResponseValidationException("LLM response is empty");
        }

        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = removeCodeFence(trimmed);
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new LlmResponseValidationException("LLM response does not contain JSON object");
        }
        return trimmed.substring(start, end + 1);
    }

    private String removeCodeFence(String content) {
        String withoutOpeningFence = content.replaceFirst("^```[a-zA-Z]*\\s*", "");
        return withoutOpeningFence.replaceFirst("\\s*```$", "").trim();
    }
}
