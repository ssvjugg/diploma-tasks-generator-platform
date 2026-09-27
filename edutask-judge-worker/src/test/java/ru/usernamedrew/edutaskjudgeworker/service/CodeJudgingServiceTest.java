package ru.usernamedrew.edutaskjudgeworker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.usernamedrew.edutaskcommon.event.judge.CodeSubmissionRequestedEvent;
import ru.usernamedrew.edutaskcommon.event.judge.CodeSubmissionResultEvent;
import ru.usernamedrew.edutaskcommon.event.judge.JudgeSubmissionStatus;
import ru.usernamedrew.edutaskcommon.event.judge.JudgeTestCasePayload;
import ru.usernamedrew.edutaskjudgeworker.config.Judge0Properties;
import ru.usernamedrew.edutaskjudgeworker.config.JudgeWorkerProperties;
import ru.usernamedrew.edutaskjudgeworker.exception.UnsupportedJudgeLanguageException;
import ru.usernamedrew.edutaskjudgeworker.judge.Judge0Client;
import ru.usernamedrew.edutaskjudgeworker.judge.Judge0LanguageMapper;
import ru.usernamedrew.edutaskjudgeworker.judge.Judge0Status;
import ru.usernamedrew.edutaskjudgeworker.judge.Judge0SubmissionResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodeJudgingServiceTest {
    @Mock
    private Judge0Client judge0Client;

    @Mock
    private Judge0LanguageMapper languageMapper;

    private CodeJudgingService service;

    @BeforeEach
    void setUp() {
        Judge0Properties judge0Properties = new Judge0Properties();
        judge0Properties.setDefaultCpuTimeLimit(BigDecimal.valueOf(2));
        judge0Properties.setDefaultMemoryLimit(131072);

        JudgeWorkerProperties workerProperties = new JudgeWorkerProperties();
        workerProperties.setSchemaVersion(7);

        service = new CodeJudgingService(
            judge0Client,
            languageMapper,
            judge0Properties,
            workerProperties
        );
    }

    @Test
    void havingValidSubmission_whenJudge_thenReturnsOrderedAcceptedResultWithScore() {
        CodeSubmissionRequestedEvent event = event(List.of(
            testCase(1, 30),
            testCase(0, 70)
        ));
        when(languageMapper.resolveLanguageId("java")).thenReturn(62);
        when(judge0Client.createSubmission(any())).thenReturn("t1", "t0");
        when(judge0Client.waitForResult("t1")).thenReturn(accepted("t1"));
        when(judge0Client.waitForResult("t0")).thenReturn(accepted("t0"));

        CodeSubmissionResultEvent result = service.judge(event);

        assertEquals(JudgeSubmissionStatus.ACCEPTED, result.status());
        assertEquals(2, result.passedCount());
        assertEquals(2, result.totalCount());
        assertEquals(100, result.score());
        assertEquals(100, result.maxScore());
        assertEquals(0, result.testResults().get(0).index());
        assertEquals(1, result.testResults().get(1).index());
        assertNull(result.errorMessage());
    }

    @Test
    void havingNoTestCases_whenJudge_thenReturnsFailedWithClearMessage() {
        CodeSubmissionRequestedEvent event = event(List.of());

        CodeSubmissionResultEvent result = service.judge(event);

        assertEquals(JudgeSubmissionStatus.FAILED, result.status());
        assertEquals("No test cases provided", result.errorMessage());
        assertEquals(0, result.totalCount());
        assertEquals(0, result.maxScore());
    }

    @Test
    void havingUnsupportedLanguage_whenJudge_thenReturnsFailedResponse() {
        CodeSubmissionRequestedEvent event = event(List.of(testCase(0, 50)));
        when(languageMapper.resolveLanguageId("java"))
            .thenThrow(new UnsupportedJudgeLanguageException("Unsupported programming language"));

        CodeSubmissionResultEvent result = service.judge(event);

        assertEquals(JudgeSubmissionStatus.FAILED, result.status());
        assertEquals("Unsupported programming language", result.errorMessage());
        assertEquals(1, result.totalCount());
        assertEquals(50, result.maxScore());
    }

    private CodeSubmissionRequestedEvent event(List<JudgeTestCasePayload> testCases) {
        return new CodeSubmissionRequestedEvent(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "java",
            "class Main {}",
            testCases,
            OffsetDateTime.now(),
            1
        );
    }

    private JudgeTestCasePayload testCase(int index, int points) {
        return new JudgeTestCasePayload(
            UUID.randomUUID(),
            index,
            "in",
            "out",
            false,
            points,
            null,
            null
        );
    }

    private Judge0SubmissionResult accepted(String token) {
        return new Judge0SubmissionResult(
            token,
            "ok",
            null,
            null,
            null,
            new Judge0Status(3, "Accepted"),
            "0.001",
            256
        );
    }
}
