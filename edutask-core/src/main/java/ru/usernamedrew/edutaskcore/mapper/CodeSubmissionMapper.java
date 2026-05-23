package ru.usernamedrew.edutaskcore.mapper;

import org.springframework.stereotype.Component;
import ru.usernamedrew.edutaskcommon.dto.submission.CodeSubmissionResponse;
import ru.usernamedrew.edutaskcommon.dto.submission.CodeSubmissionTestResultResponse;
import ru.usernamedrew.edutaskcore.entity.Submission;
import ru.usernamedrew.edutaskcore.entity.SubmissionTestResult;

import java.util.Comparator;
import java.util.List;

@Component
public class CodeSubmissionMapper {
    public CodeSubmissionResponse toResponse(Submission submission, boolean includeSourceCode) {
        List<CodeSubmissionTestResultResponse> testResults = submission.getTestResults().stream()
            .sorted(Comparator.comparingInt(SubmissionTestResult::getTestCaseIndex))
            .map(this::toTestResultResponse)
            .toList();

        return new CodeSubmissionResponse(
            submission.getId(),
            submission.getTask().getId(),
            submission.getUser().getId(),
            submission.getLanguage().getCode(),
            includeSourceCode ? submission.getSourceCode() : null,
            submission.getStatus(),
            testResults,
            submission.getPassedTests(),
            submission.getTotalTests(),
            submission.getScore(),
            submission.getMaxScore(),
            submission.getErrorMessage(),
            submission.getCreatedAt(),
            submission.getUpdatedAt()
        );
    }

    private CodeSubmissionTestResultResponse toTestResultResponse(SubmissionTestResult result) {
        boolean hidden = result.isHidden();
        return new CodeSubmissionTestResultResponse(
            result.getId(),
            hidden || result.getTestCase() == null ? null : result.getTestCase().getId(),
            result.getTestCaseIndex(),
            hidden,
            result.getStatus(),
            hidden ? null : result.getInputData(),
            hidden ? null : result.getExpectedOutput(),
            hidden ? null : result.getStdout(),
            hidden ? null : result.getStderr(),
            hidden ? null : result.getCompileOutput(),
            abbreviate(result.getErrorMessage(), 500),
            result.getTime(),
            result.getMemory(),
            result.getPoints(),
            result.getCreatedAt()
        );
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
