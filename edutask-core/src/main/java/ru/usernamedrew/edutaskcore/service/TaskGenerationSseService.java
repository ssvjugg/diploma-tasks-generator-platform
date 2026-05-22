package ru.usernamedrew.edutaskcore.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.usernamedrew.edutaskcommon.dto.generation.TaskGenerationResponse;
import ru.usernamedrew.edutaskcommon.dto.generation.TaskGenerationStatus;
import ru.usernamedrew.edutaskcore.config.TaskGenerationProperties;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class TaskGenerationSseService {
    private final TaskGenerationProperties properties;
    private final ConcurrentMap<UUID, Set<SseEmitter>> emittersByRequestId = new ConcurrentHashMap<>();

    public TaskGenerationSseService(TaskGenerationProperties properties) {
        this.properties = properties;
    }

    public SseEmitter subscribe(TaskGenerationResponse initialState) {
        SseEmitter emitter = new SseEmitter(properties.getSseTimeout().toMillis());
        UUID requestId = initialState.requestId();

        emitter.onCompletion(() -> removeEmitter(requestId, emitter));
        emitter.onTimeout(() -> removeEmitter(requestId, emitter));
        emitter.onError(exception -> removeEmitter(requestId, emitter));

        if (!initialState.status().isTerminal()) {
            emittersByRequestId.computeIfAbsent(requestId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);
        }

        sendEvent(emitter, initialState);

        if (initialState.status().isTerminal()) {
            emitter.complete();
        }

        return emitter;
    }

    public void publish(TaskGenerationResponse response) {
        boolean terminal = response.status().isTerminal();
        Set<SseEmitter> emitters = terminal
            ? emittersByRequestId.remove(response.requestId())
            : emittersByRequestId.get(response.requestId());
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            sendEvent(emitter, response);
            if (terminal) {
                emitter.complete();
            }
        }
    }

    public void synchronize(TaskGenerationResponse previousState, TaskGenerationResponse latestState) {
        if (!previousState.status().equals(latestState.status())
            || !Objects.equals(previousState.result(), latestState.result())
            || !Objects.equals(previousState.errorMessage(), latestState.errorMessage())) {
            publish(latestState);
        }
    }

    private void sendEvent(SseEmitter emitter, TaskGenerationResponse response) {
        try {
            emitter.send(SseEmitter.event()
                .name(eventName(response.status()))
                .id(response.requestId().toString())
                .data(response));
        } catch (IOException | IllegalStateException exception) {
            log.debug("Failed to send generation SSE event, requestId={}", response.requestId(), exception);
            emitter.completeWithError(exception);
        }
    }

    private void removeEmitter(UUID requestId, SseEmitter emitter) {
        Set<SseEmitter> emitters = emittersByRequestId.get(requestId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByRequestId.remove(requestId, emitters);
        }
    }

    private String eventName(TaskGenerationStatus status) {
        return switch (status) {
            case QUEUED, PROCESSING -> "generation-status";
            case COMPLETED -> "generation-completed";
            case FAILED -> "generation-failed";
        };
    }
}
