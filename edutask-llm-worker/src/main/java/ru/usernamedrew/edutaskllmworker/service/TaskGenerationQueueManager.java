package ru.usernamedrew.edutaskllmworker.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.usernamedrew.edutaskcommon.event.generation.TaskGenerationRequestedEvent;
import ru.usernamedrew.edutaskcommon.event.generation.TaskGenerationResponseEvent;
import ru.usernamedrew.edutaskllmworker.config.LlmWorkerProperties;
import ru.usernamedrew.edutaskllmworker.kafka.TaskGenerationResponseProducer;
import ru.usernamedrew.edutaskllmworker.llm.LlmClientException;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskGenerationQueueManager {
    private final TaskGenerationWorkerService workerService;
    private final TaskGenerationResponseProducer responseProducer;
    private final LlmWorkerProperties properties;

    private Semaphore generationSemaphore;

    @PostConstruct
    void init() {
        generationSemaphore = new Semaphore(properties.getMaxConcurrentGenerations());
    }

    public void process(TaskGenerationRequestedEvent event) {
        long startedAt = System.nanoTime();
        boolean permitAcquired = false;
        try {
            logReceived(event);
            permitAcquired = tryAcquirePermit();
            TaskGenerationResponseEvent response = permitAcquired
                ? workerService.generate(event)
                : workerService.failedResponse(event, "LLM worker is overloaded");
            responseProducer.send(response)
                .get(properties.getResponseSendTimeout().toMillis(), TimeUnit.MILLISECONDS);
            log.info(
                "Processed task generation request, requestId={}, status={}, durationMs={}",
                event.requestId(),
                response.status(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LlmClientException("Interrupted while processing task generation request", exception);
        } catch (Exception exception) {
            throw new LlmClientException("Failed to process task generation request", exception);
        } finally {
            if (permitAcquired) {
                generationSemaphore.release();
            }
        }
    }

    private boolean tryAcquirePermit() {
        try {
            return generationSemaphore.tryAcquire(
                properties.getConcurrencyAcquireTimeout().toMillis(),
                TimeUnit.MILLISECONDS
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LlmClientException("Interrupted while waiting for LLM worker capacity", exception);
        }
    }

    private void logReceived(TaskGenerationRequestedEvent event) {
        log.info(
            "Received task generation request, requestId={}, provider={}, model={}",
            event.requestId(),
            event.provider(),
            event.model()
        );
    }
}
