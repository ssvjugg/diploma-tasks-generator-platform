package ru.usernamedrew.edutaskcore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.usernamedrew.edutaskcommon.dto.submission.CodeSubmissionResponse;
import ru.usernamedrew.edutaskcommon.event.judge.CodeSubmissionResultEvent;
import ru.usernamedrew.edutaskcommon.event.judge.JudgeSubmissionStatus;
import ru.usernamedrew.edutaskcommon.event.judge.JudgeTestCaseResult;
import ru.usernamedrew.edutaskcommon.kafka.InvalidKafkaPayloadException;
import ru.usernamedrew.edutaskcore.entity.Submission;
import ru.usernamedrew.edutaskcore.entity.SubmissionTestResult;
import ru.usernamedrew.edutaskcore.mapper.CodeSubmissionMapper;
import ru.usernamedrew.edutaskcore.repository.SubmissionRepository;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeSubmissionResultHandler {
    private final SubmissionRepository submissionRepository;
    private final CodeSubmissionMapper submissionMapper;
    private final CodeSubmissionSseService sseService;

    @Transactional
    public void handle(CodeSubmissionResultEvent event) {
        Submission submission = submissionRepository.findDetailedById(event.submissionId())
            .orElseThrow(() -> new InvalidKafkaPayloadException(
                "Submission not found for result submissionId: " + event.submissionId()
            ));

        validateEventTarget(submission, event);
        if (submission.getStatus().isTerminal()) {
            log.info(
                "Ignoring duplicate code submission result, submissionId={}, currentStatus={}",
                event.submissionId(),
                submission.getStatus()
            );
            return;
        }

        applyResult(submission, event);
        recomputeTotals(submission);
        submissionRepository.flush();

        log.info(
            "Applied code submission result, submissionId={}, status={}, passedTests={}, totalTests={}, score={}, maxScore={}",
            submission.getId(),
            submission.getStatus(),
            submission.getPassedTests(),
            submission.getTotalTests(),
            submission.getScore(),
            submission.getMaxScore()
        );
        publishAfterCommit(submissionMapper.toResponse(submission, false));
    }

    private void validateEventTarget(Submission submission, CodeSubmissionResultEvent event) {
        if (!submission.getTask().getId().equals(event.taskId())) {
            throw new InvalidKafkaPayloadException("Code submission result has mismatched taskId");
        }
        if (!submission.getUser().getId().equals(event.userId())) {
            throw new InvalidKafkaPayloadException("Code submission result has mismatched userId");
        }
    }

    private void applyResult(Submission submission, CodeSubmissionResultEvent event) {
        submission.setStatus(event.status());
        submission.setErrorMessage(event.errorMessage());

        if (event.testResults() == null || event.testResults().isEmpty()) {
            if (event.status() == JudgeSubmissionStatus.FAILED) {
                markAllTestsFailed(submission, event.errorMessage());
                return;
            }
            throw new InvalidKafkaPayloadException("Code submission result must contain test results");
        }

        Map<Integer, SubmissionTestResult> resultsByIndex = submission.getTestResults().stream()
            .collect(Collectors.toMap(SubmissionTestResult::getTestCaseIndex, Function.identity()));

        for (JudgeTestCaseResult eventResult : event.testResults()) {
            SubmissionTestResult result = resultsByIndex.get(eventResult.index());
            if (result == null) {
                throw new InvalidKafkaPayloadException("Unknown test result index: " + eventResult.index());
            }
            applyTestResult(result, eventResult);
        }
    }

    private void applyTestResult(SubmissionTestResult result, JudgeTestCaseResult eventResult) {
        result.setStatus(eventResult.status());
        result.setJudge0Token(eventResult.judge0Token());
        result.setStdout(eventResult.stdout());
        result.setStderr(eventResult.stderr());
        result.setCompileOutput(eventResult.compileOutput());
        result.setErrorMessage(eventResult.errorMessage());
        result.setTime(eventResult.time());
        result.setMemory(eventResult.memory());
    }

    private void markAllTestsFailed(Submission submission, String errorMessage) {
        for (SubmissionTestResult result : submission.getTestResults()) {
            result.setStatus(JudgeSubmissionStatus.FAILED);
            result.setErrorMessage(errorMessage);
        }
    }

    private void recomputeTotals(Submission submission) {
        int total = submission.getTestResults().size();
        int passed = 0;
        int score = 0;
        int maxScore = 0;

        for (SubmissionTestResult result : submission.getTestResults()) {
            maxScore += result.getPoints();
            if (result.getStatus() == JudgeSubmissionStatus.ACCEPTED) {
                passed++;
                score += result.getPoints();
            }
        }

        submission.setTotalTests(total);
        submission.setPassedTests(passed);
        submission.setScore(score);
        submission.setMaxScore(maxScore);
    }

    private void publishAfterCommit(CodeSubmissionResponse response) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sseService.publish(response);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sseService.publish(response);
            }
        });
    }
}
