package ru.usernamedrew.edutaskcore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.usernamedrew.edutaskcommon.dto.submission.CodeSubmissionCreateRequest;
import ru.usernamedrew.edutaskcommon.dto.submission.CodeSubmissionResponse;
import ru.usernamedrew.edutaskcommon.event.judge.CodeSubmissionRequestedEvent;
import ru.usernamedrew.edutaskcommon.event.judge.JudgeSubmissionStatus;
import ru.usernamedrew.edutaskcommon.event.judge.JudgeTestCasePayload;
import ru.usernamedrew.edutaskcore.config.CodeSubmissionProperties;
import ru.usernamedrew.edutaskcore.entity.ProgrammingLanguage;
import ru.usernamedrew.edutaskcore.entity.Submission;
import ru.usernamedrew.edutaskcore.entity.SubmissionTestResult;
import ru.usernamedrew.edutaskcore.entity.Task;
import ru.usernamedrew.edutaskcore.entity.TestCase;
import ru.usernamedrew.edutaskcore.entity.UserProfile;
import ru.usernamedrew.edutaskcore.exception.BadRequestException;
import ru.usernamedrew.edutaskcore.exception.ResourceNotFoundException;
import ru.usernamedrew.edutaskcore.kafka.producer.CodeSubmissionRequestProducer;
import ru.usernamedrew.edutaskcore.mapper.CodeSubmissionMapper;
import ru.usernamedrew.edutaskcore.repository.ProgrammingLanguageRepository;
import ru.usernamedrew.edutaskcore.repository.SubmissionRepository;
import ru.usernamedrew.edutaskcore.repository.TaskRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeSubmissionService {
    private static final String ENQUEUE_ERROR_MESSAGE = "Failed to enqueue code submission";

    private final SubmissionRepository submissionRepository;
    private final TaskRepository taskRepository;
    private final ProgrammingLanguageRepository languageRepository;
    private final TestCaseService testCaseService;
    private final UserProfileService userProfileService;
    private final CodeSubmissionRequestProducer requestProducer;
    private final CodeSubmissionProperties properties;
    private final CodeSubmissionMapper submissionMapper;

    @Transactional
    public CodeSubmissionResponse createSubmission(UUID taskId, CodeSubmissionCreateRequest request, Jwt jwt) {
        UserProfile user = userProfileService.resolveCurrentUser(jwt);
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        ProgrammingLanguage language = languageRepository.findByCodeIgnoreCase(request.language().trim())
            .orElseThrow(() -> new BadRequestException("Unsupported programming language: " + request.language()));

        List<TestCase> testCases = testCaseService.getActualTestCasesForJudging(taskId);
        if (testCases.isEmpty()) {
            throw new BadRequestException("Task has no test cases: " + taskId);
        }

        Submission submission = new Submission();
        submission.setUser(user);
        submission.setTask(task);
        submission.setLanguage(language);
        submission.setSourceCode(request.sourceCode());
        submission.setStatus(JudgeSubmissionStatus.QUEUED);
        submission.setTotalTests(testCases.size());
        submission.setMaxScore(sumPoints(testCases));

        IntStream.range(0, testCases.size())
            .mapToObj(index -> createPendingResult(testCases.get(index), index))
            .forEach(submission::addTestResult);

        Submission savedSubmission = submissionRepository.saveAndFlush(submission);
        CodeSubmissionRequestedEvent event = createEvent(savedSubmission, testCases);
        enqueueAfterCommit(event);

        return submissionMapper.toResponse(savedSubmission, true);
    }

    @Transactional(readOnly = true)
    public CodeSubmissionResponse getSubmission(UUID submissionId, Jwt jwt) {
        UserProfile currentUser = userProfileService.resolveCurrentUser(jwt);
        Submission submission = findDetailed(submissionId);
        assertCanRead(submission, currentUser);
        return submissionMapper.toResponse(submission, true);
    }

    @Transactional(readOnly = true)
    public Submission findDetailed(UUID submissionId) {
        return submissionRepository.findDetailedById(submissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + submissionId));
    }

    private SubmissionTestResult createPendingResult(TestCase testCase, int index) {
        SubmissionTestResult result = new SubmissionTestResult();
        result.setTestCase(testCase);
        result.setTestCaseIndex(index);
        result.setHidden(testCase.isHidden());
        result.setInputData(testCase.getInputData());
        result.setExpectedOutput(testCase.getExpectedOutput());
        result.setPoints(testCase.getPoints());
        result.setStatus(JudgeSubmissionStatus.QUEUED);
        return result;
    }

    private CodeSubmissionRequestedEvent createEvent(Submission submission, List<TestCase> testCases) {
        List<JudgeTestCasePayload> payloads = IntStream.range(0, testCases.size())
            .mapToObj(index -> {
                TestCase testCase = testCases.get(index);
                return new JudgeTestCasePayload(
                    testCase.getId(),
                    index,
                    testCase.getInputData(),
                    testCase.getExpectedOutput(),
                    testCase.isHidden(),
                    testCase.getPoints(),
                    null,
                    null
                );
            })
            .toList();

        return new CodeSubmissionRequestedEvent(
            UUID.randomUUID(),
            submission.getId(),
            submission.getTask().getId(),
            submission.getUser().getId(),
            submission.getLanguage().getCode(),
            submission.getSourceCode(),
            payloads,
            OffsetDateTime.now(),
            properties.getSchemaVersion()
        );
    }

    private void enqueueAfterCommit(CodeSubmissionRequestedEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            enqueue(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                enqueue(event);
            }
        });
    }

    private void enqueue(CodeSubmissionRequestedEvent event) {
        try {
            requestProducer.send(event)
                .orTimeout(properties.getKafkaSendTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        markEnqueueFailed(event.submissionId(), exception);
                    }
                });
        } catch (RuntimeException exception) {
            markEnqueueFailed(event.submissionId(), exception);
        }
    }

    private void markEnqueueFailed(UUID submissionId, Throwable exception) {
        log.error("Failed to enqueue code submission, submissionId={}", submissionId, exception);
        submissionRepository.findById(submissionId).ifPresent(submission -> {
            submission.setStatus(JudgeSubmissionStatus.FAILED);
            submission.setErrorMessage(ENQUEUE_ERROR_MESSAGE);
            submissionRepository.save(submission);
        });
    }

    private void assertCanRead(Submission submission, UserProfile currentUser) {
        boolean owner = submission.getUser().getId().equals(currentUser.getId());
        boolean admin = currentUser.getRole() == UserProfile.UserRole.ADMIN;
        if (!owner && !admin) {
            throw new AccessDeniedException("Access denied to submission: " + submission.getId());
        }
    }

    private int sumPoints(List<TestCase> testCases) {
        return testCases.stream()
            .mapToInt(TestCase::getPoints)
            .sum();
    }
}
