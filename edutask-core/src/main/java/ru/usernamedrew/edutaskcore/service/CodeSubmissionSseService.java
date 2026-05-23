package ru.usernamedrew.edutaskcore.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.usernamedrew.edutaskcommon.dto.submission.CodeSubmissionResponse;
import ru.usernamedrew.edutaskcommon.event.judge.JudgeSubmissionStatus;
import ru.usernamedrew.edutaskcore.config.CodeSubmissionProperties;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class CodeSubmissionSseService {
    private final CodeSubmissionProperties properties;
    private final ConcurrentMap<UUID, Set<SseEmitter>> emittersBySubmissionId = new ConcurrentHashMap<>();

    public CodeSubmissionSseService(CodeSubmissionProperties properties) {
        this.properties = properties;
    }

    public SseEmitter subscribe(CodeSubmissionResponse initialState) {
        SseEmitter emitter = new SseEmitter(properties.getSseTimeout().toMillis());
        UUID submissionId = initialState.submissionId();

        emitter.onCompletion(() -> removeEmitter(submissionId, emitter));
        emitter.onTimeout(() -> removeEmitter(submissionId, emitter));
        emitter.onError(exception -> removeEmitter(submissionId, emitter));

        if (!initialState.status().isTerminal()) {
            emittersBySubmissionId.computeIfAbsent(submissionId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);
        }

        sendEvent(emitter, initialState);

        if (initialState.status().isTerminal()) {
            emitter.complete();
        }

        return emitter;
    }

    public void publish(CodeSubmissionResponse response) {
        boolean terminal = response.status().isTerminal();
        Set<SseEmitter> emitters = terminal
            ? emittersBySubmissionId.remove(response.submissionId())
            : emittersBySubmissionId.get(response.submissionId());
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

    public void synchronize(CodeSubmissionResponse previousState, CodeSubmissionResponse latestState) {
        if (!previousState.status().equals(latestState.status())
            || !Objects.equals(previousState.testResults(), latestState.testResults())
            || !Objects.equals(previousState.errorMessage(), latestState.errorMessage())) {
            publish(latestState);
        }
    }

    private void sendEvent(SseEmitter emitter, CodeSubmissionResponse response) {
        try {
            emitter.send(SseEmitter.event()
                .name(eventName(response.status()))
                .id(response.submissionId().toString())
                .data(response));
        } catch (IOException | IllegalStateException exception) {
            log.debug("Failed to send submission SSE event, submissionId={}", response.submissionId(), exception);
            emitter.completeWithError(exception);
        }
    }

    private void removeEmitter(UUID submissionId, SseEmitter emitter) {
        Set<SseEmitter> emitters = emittersBySubmissionId.get(submissionId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersBySubmissionId.remove(submissionId, emitters);
        }
    }

    private String eventName(JudgeSubmissionStatus status) {
        return switch (status) {
            case QUEUED, PROCESSING -> "submission-status";
            case ACCEPTED -> "submission-accepted";
            case WRONG_ANSWER,
                 COMPILATION_ERROR,
                 RUNTIME_ERROR,
                 TIME_LIMIT_EXCEEDED,
                 MEMORY_LIMIT_EXCEEDED,
                 FAILED -> "submission-failed";
        };
    }
}
