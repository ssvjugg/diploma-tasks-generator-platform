package ru.usernamedrew.edutaskcore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.usernamedrew.edutaskcommon.dto.generation.TaskGenerationResponse;
import ru.usernamedrew.edutaskcommon.dto.generation.TaskGenerationStatus;
import ru.usernamedrew.edutaskcore.entity.GenerationRequest;
import ru.usernamedrew.edutaskcore.mapper.TaskGenerationMapper;
import ru.usernamedrew.edutaskcore.repository.GenerationRequestRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskGenerationStatusService {
    private final GenerationRequestRepository generationRequestRepository;
    private final TaskGenerationMapper taskGenerationMapper;
    private final TaskGenerationSseService sseService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID requestId, String errorMessage) {
        GenerationRequest generationRequest = generationRequestRepository.findDetailedById(requestId)
            .orElse(null);
        if (generationRequest == null) {
            log.warn("Cannot mark generation request as failed because it was not found, requestId={}", requestId);
            return;
        }
        if (generationRequest.getStatus().isTerminal()) {
            log.info(
                "Skip marking terminal generation request as failed, requestId={}, currentStatus={}",
                requestId,
                generationRequest.getStatus()
            );
            return;
        }

        generationRequest.setStatus(TaskGenerationStatus.FAILED);
        generationRequest.setErrorMessage(errorMessage);
        generationRequestRepository.flush();
        publishAfterCommit(taskGenerationMapper.toResponse(generationRequest));
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
