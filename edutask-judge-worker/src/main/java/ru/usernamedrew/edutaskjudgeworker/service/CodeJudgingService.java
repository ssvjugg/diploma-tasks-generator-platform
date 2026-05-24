package ru.usernamedrew.edutaskjudgeworker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.usernamedrew.edutaskcommon.event.judge.CodeSubmissionRequestedEvent;
import ru.usernamedrew.edutaskcommon.event.judge.CodeSubmissionResultEvent;
import ru.usernamedrew.edutaskcommon.event.judge.JudgeSubmissionStatus;
import ru.usernamedrew.edutaskcommon.event.judge.JudgeTestCasePayload;
import ru.usernamedrew.edutaskcommon.event.judge.JudgeTestCaseResult;
import ru.usernamedrew.edutaskjudgeworker.config.Judge0Properties;
import ru.usernamedrew.edutaskjudgeworker.config.JudgeWorkerProperties;
import ru.usernamedrew.edutaskjudgeworker.exception.Judge0ClientException;
import ru.usernamedrew.edutaskjudgeworker.exception.UnsupportedJudgeLanguageException;
import ru.usernamedrew.edutaskjudgeworker.judge.Judge0Client;
import ru.usernamedrew.edutaskjudgeworker.judge.Judge0LanguageMapper;
import ru.usernamedrew.edutaskjudgeworker.judge.Judge0SubmissionRequest;
import ru.usernamedrew.edutaskjudgeworker.judge.Judge0SubmissionResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeJudgingService {
    private final Judge0Client judge0Client;
    private final Judge0LanguageMapper languageMapper;
    private final Judge0Properties judge0Properties;
    private final JudgeWorkerProperties workerProperties;

    public CodeSubmissionResultEvent judge(CodeSubmissionRequestedEvent event) {
        try {
            int languageId = languageMapper.resolveLanguageId(event.language());
            List<PendingJudgeSubmission> pendingSubmissions = createJudge0Submissions(event, languageId);
            List<JudgeTestCaseResult> testResults = waitForResults(pendingSubmissions);
            return completed(event, testResults);
        } catch (UnsupportedJudgeLanguageException exception) {
            return failed(event, "Unsupported programming language", exception);
        } catch (RuntimeException exception) {
            return failed(event, "Code judging failed", exception);
        }
    }

    public CodeSubmissionResultEvent failedResponse(CodeSubmissionRequestedEvent event, String errorMessage) {
        return new CodeSubmissionResultEvent(
            UUID.randomUUID(),
            event.submissionId(),
            event.taskId(),
            event.userId(),
            JudgeSubmissionStatus.FAILED,
            List.of(),
            0,
            event.testCases() == null ? 0 : event.testCases().size(),
            0,
            event.testCases() == null ? 0 : sumMaxScore(event.testCases()),
            errorMessage == null || errorMessage.isBlank() ? "Code judging failed" : errorMessage,
            OffsetDateTime.now(),
            workerProperties.getSchemaVersion()
        );
    }

    private List<PendingJudgeSubmission> createJudge0Submissions(CodeSubmissionRequestedEvent event, int languageId) {
        List<PendingJudgeSubmission> pending = new ArrayList<>();
        for (JudgeTestCasePayload testCase : event.testCases()) {
            Judge0SubmissionRequest request = new Judge0SubmissionRequest(
                event.sourceCode(),
                languageId,
                testCase.inputData(),
                testCase.expectedOutput(),
                testCase.cpuTimeLimit() == null ? judge0Properties.getDefaultCpuTimeLimit() : testCase.cpuTimeLimit(),
                testCase.memoryLimit() == null ? judge0Properties.getDefaultMemoryLimit() : testCase.memoryLimit()
            );
            try {
                String token = judge0Client.createSubmission(request);
                pending.add(new PendingJudgeSubmission(testCase, token, null));
            } catch (Judge0ClientException exception) {
                pending.add(new PendingJudgeSubmission(testCase, null, exception.getMessage()));
            }
        }
        return pending;
    }

    private List<JudgeTestCaseResult> waitForResults(List<PendingJudgeSubmission> pendingSubmissions) {
        List<JudgeTestCaseResult> results = new ArrayList<>();
        for (PendingJudgeSubmission pending : pendingSubmissions) {
            if (pending.creationError() != null) {
                results.add(failedTestResult(pending.testCase(), null, pending.creationError()));
                continue;
            }
            try {
                Judge0SubmissionResult judge0Result = judge0Client.waitForResult(pending.token());
                results.add(toTestCaseResult(pending.testCase(), judge0Result));
            } catch (Judge0ClientException exception) {
                results.add(failedTestResult(pending.testCase(), pending.token(), exception.getMessage()));
            }
        }
        return results.stream()
            .sorted(Comparator.comparingInt(JudgeTestCaseResult::index))
            .toList();
    }

    private CodeSubmissionResultEvent completed(
        CodeSubmissionRequestedEvent event,
        List<JudgeTestCaseResult> testResults
    ) {
        JudgeSubmissionStatus status = aggregateStatus(testResults);
        int passedCount = (int) testResults.stream()
            .filter(result -> result.status() == JudgeSubmissionStatus.ACCEPTED)
            .count();
        int maxScore = sumMaxScore(event.testCases());
        Map<Integer, Integer> pointsByIndex = event.testCases().stream()
            .collect(Collectors.toMap(JudgeTestCasePayload::index, JudgeTestCasePayload::points));
        int score = testResults.stream()
            .filter(result -> result.status() == JudgeSubmissionStatus.ACCEPTED)
            .mapToInt(result -> pointsByIndex.getOrDefault(result.index(), 0))
            .sum();

        return new CodeSubmissionResultEvent(
            UUID.randomUUID(),
            event.submissionId(),
            event.taskId(),
            event.userId(),
            status,
            testResults,
            passedCount,
            event.testCases().size(),
            score,
            maxScore,
            status == JudgeSubmissionStatus.FAILED ? "Code judging failed" : null,
            OffsetDateTime.now(),
            workerProperties.getSchemaVersion()
        );
    }

    private CodeSubmissionResultEvent failed(
        CodeSubmissionRequestedEvent event,
        String errorMessage,
        RuntimeException exception
    ) {
        log.warn(
            "Code judging failed, submissionId={}, taskId={}, userId={}, language={}",
            event.submissionId(),
            event.taskId(),
            event.userId(),
            event.language(),
            exception
        );
        return failedResponse(event, errorMessage);
    }

    private JudgeTestCaseResult toTestCaseResult(JudgeTestCasePayload testCase, Judge0SubmissionResult judge0Result) {
        JudgeSubmissionStatus status = mapStatus(judge0Result);
        return new JudgeTestCaseResult(
            testCase.testCaseId(),
            testCase.index(),
            judge0Result.token(),
            status,
            judge0Result.stdout(),
            judge0Result.stderr(),
            judge0Result.compileOutput(),
            errorMessage(status, judge0Result),
            parseTime(judge0Result.time()),
            judge0Result.memory()
        );
    }

    private JudgeTestCaseResult failedTestResult(JudgeTestCasePayload testCase, String token, String errorMessage) {
        return new JudgeTestCaseResult(
            testCase.testCaseId(),
            testCase.index(),
            token,
            JudgeSubmissionStatus.FAILED,
            null,
            null,
            null,
            errorMessage,
            null,
            null
        );
    }

    private JudgeSubmissionStatus mapStatus(Judge0SubmissionResult result) {
        if (result.status() == null || result.status().id() == null) {
            return JudgeSubmissionStatus.FAILED;
        }

        String description = result.status().description() == null
            ? ""
            : result.status().description().toLowerCase();
        if (description.contains("memory")) {
            return JudgeSubmissionStatus.MEMORY_LIMIT_EXCEEDED;
        }

        return switch (result.status().id()) {
            case 3 -> JudgeSubmissionStatus.ACCEPTED;
            case 4 -> JudgeSubmissionStatus.WRONG_ANSWER;
            case 5 -> JudgeSubmissionStatus.TIME_LIMIT_EXCEEDED;
            case 6 -> JudgeSubmissionStatus.COMPILATION_ERROR;
            case 7, 8, 9, 10, 11, 12, 14 -> JudgeSubmissionStatus.RUNTIME_ERROR;
            default -> JudgeSubmissionStatus.FAILED;
        };
    }

    private JudgeSubmissionStatus aggregateStatus(List<JudgeTestCaseResult> results) {
        if (results.isEmpty()) {
            return JudgeSubmissionStatus.FAILED;
        }

        List<JudgeSubmissionStatus> priorities = List.of(
            JudgeSubmissionStatus.COMPILATION_ERROR,
            JudgeSubmissionStatus.TIME_LIMIT_EXCEEDED,
            JudgeSubmissionStatus.MEMORY_LIMIT_EXCEEDED,
            JudgeSubmissionStatus.RUNTIME_ERROR,
            JudgeSubmissionStatus.WRONG_ANSWER
        );
        for (JudgeSubmissionStatus status : priorities) {
            if (hasStatus(results, status)) {
                return status;
            }
        }
        if (results.stream().allMatch(result -> result.status() == JudgeSubmissionStatus.ACCEPTED)) {
            return JudgeSubmissionStatus.ACCEPTED;
        }
        return JudgeSubmissionStatus.FAILED;
    }

    private boolean hasStatus(List<JudgeTestCaseResult> results, JudgeSubmissionStatus status) {
        return results.stream().anyMatch(result -> result.status() == status);
    }

    private String errorMessage(JudgeSubmissionStatus status, Judge0SubmissionResult result) {
        if (status == JudgeSubmissionStatus.ACCEPTED) {
            return null;
        }
        if (result.message() != null && !result.message().isBlank()) {
            return result.message();
        }
        if (result.status() != null && result.status().description() != null) {
            return result.status().description();
        }
        return status.name();
    }

    private BigDecimal parseTime(String time) {
        if (time == null || time.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(time);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private int sumMaxScore(List<JudgeTestCasePayload> testCases) {
        return testCases.stream()
            .mapToInt(JudgeTestCasePayload::points)
            .sum();
    }

    private record PendingJudgeSubmission(
        JudgeTestCasePayload testCase,
        String token,
        String creationError
    ) {
    }
}
