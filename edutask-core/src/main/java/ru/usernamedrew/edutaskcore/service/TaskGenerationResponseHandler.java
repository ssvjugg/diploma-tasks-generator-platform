package ru.usernamedrew.edutaskcore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.usernamedrew.edutaskcommon.dto.generation.TaskGenerationStatus;
import ru.usernamedrew.edutaskcommon.dto.generation.TaskGenerationResponse;
import ru.usernamedrew.edutaskcommon.event.generation.GenerationEventStatus;
import ru.usernamedrew.edutaskcommon.event.generation.TaskGenerationResponseEvent;
import ru.usernamedrew.edutaskcommon.kafka.InvalidKafkaPayloadException;
import ru.usernamedrew.edutaskcore.entity.GenerationRequest;
import ru.usernamedrew.edutaskcore.mapper.TaskGenerationMapper;
import ru.usernamedrew.edutaskcore.repository.GenerationRequestRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskGenerationResponseHandler {
    private final GenerationRequestRepository generationRequestRepository;
    private final TaskGenerationMapper taskGenerationMapper;
    private final TaskGenerationSseService sseService;

    @Transactional
    public void handle(TaskGenerationResponseEvent event) {
        GenerationRequest generationRequest = generationRequestRepository.findDetailedById(event.requestId())
            .orElseThrow(() -> new IllegalArgumentException("GenerationRequest not found: " + event.requestId()));

        if (generationRequest.getStatus().isTerminal()) {
            log.info(
                "Ignoring duplicate task generation response, requestId={}, currentStatus={}",
                event.requestId(),
                generationRequest.getStatus()
            );
            return;
        }

        applyResponse(generationRequest, event);
        generationRequestRepository.flush();

        log.info(
            "Applied task generation response, requestId={}, status={}",
            event.requestId(),
            generationRequest.getStatus()
        );
        publishAfterCommit(taskGenerationMapper.toResponse(generationRequest));
    }

    private void applyResponse(GenerationRequest generationRequest, TaskGenerationResponseEvent event) {
        if (event.status() == GenerationEventStatus.COMPLETED) {
            if (event.result() == null) {
                throw new InvalidKafkaPayloadException("Completed generation response must contain result");
            }
            generationRequest.setStatus(TaskGenerationStatus.COMPLETED);
            generationRequest.setGeneratedDraft(event.result());
            generationRequest.setErrorMessage(null);
        } else if (event.status() == GenerationEventStatus.FAILED) {
            generationRequest.setStatus(TaskGenerationStatus.FAILED);
            generationRequest.setErrorMessage(event.errorMessage());
        } else {
            throw new InvalidKafkaPayloadException("Unsupported generation response status: " + event.status());
        }

        generationRequest.setModelProvider(event.provider());
        generationRequest.setModelName(event.model());
    }

    private void publishAfterCommit(TaskGenerationResponse response) {
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
