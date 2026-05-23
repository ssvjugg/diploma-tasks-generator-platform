package ru.usernamedrew.edutaskjudgeworker.judge;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.usernamedrew.edutaskjudgeworker.config.Judge0Properties;
import ru.usernamedrew.edutaskjudgeworker.exception.UnsupportedJudgeLanguageException;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class Judge0LanguageMapper {
    private final Judge0Properties properties;

    public int resolveLanguageId(String language) {
        if (language == null || language.isBlank()) {
            throw new UnsupportedJudgeLanguageException("Programming language is required");
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        Integer languageId = properties.getLanguages().get(normalized);
        if (languageId == null) {
            throw new UnsupportedJudgeLanguageException("Unsupported programming language: " + language);
        }
        return languageId;
    }
}
