package ru.usernamedrew.edutaskjudgeworker.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.usernamedrew.edutaskcommon.event.judge.CodeSubmissionRequestedEvent;
import ru.usernamedrew.edutaskcommon.event.judge.CodeSubmissionResultEvent;
import ru.usernamedrew.edutaskjudgeworker.config.JudgeWorkerProperties;
import ru.usernamedrew.edutaskjudgeworker.exception.Judge0ClientException;
import ru.usernamedrew.edutaskjudgeworker.kafka.CodeSubmissionResultProducer;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeJudgingQueueManager {
    private final CodeJudgingService judgingService;
    private final CodeSubmissionResultProducer resultProducer;
    private final JudgeWorkerProperties properties;

    private Semaphore judgingSemaphore;

    @PostConstruct
    void init() {
        judgingSemaphore = new Semaphore(properties.getMaxConcurrentSubmissions());
    }

    public void process(CodeSubmissionRequestedEvent event) {
        long startedAt = System.nanoTime();
        boolean permitAcquired = false;
        try {
            logReceived(event);
            permitAcquired = tryAcquirePermit();
            CodeSubmissionResultEvent response = permitAcquired
                ? judgingService.judge(event)
                : judgingService.failedResponse(event, "Judge worker is overloaded");
            resultProducer.send(response)
                .get(properties.getResponseSendTimeout().toMillis(), TimeUnit.MILLISECONDS);
            long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            log.info(
                "Processed code submission, submissionId={}, taskId={}, userId={}, language={}, verdict={}, latencyMs={}",
                event.submissionId(),
                event.taskId(),
                event.userId(),
                event.language(),
                response.status(),
                latencyMs
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new Judge0ClientException("Interrupted while processing code submission", exception);
        } catch (Exception exception) {
            throw new Judge0ClientException("Failed to process code submission", exception);
        } finally {
            if (permitAcquired) {
                judgingSemaphore.release();
            }
        }
    }

    private boolean tryAcquirePermit() {
        try {
            return judgingSemaphore.tryAcquire(
                properties.getConcurrencyAcquireTimeout().toMillis(),
                TimeUnit.MILLISECONDS
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new Judge0ClientException("Interrupted while waiting for judge worker capacity", exception);
        }
    }

    private void logReceived(CodeSubmissionRequestedEvent event) {
        log.info(
            "Received code submission, submissionId={}, taskId={}, userId={}, language={}, testCount={}",
            event.submissionId(),
            event.taskId(),
            event.userId(),
            event.language(),
            event.testCases().size()
        );
    }
}
