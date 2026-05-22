package ru.usernamedrew.edutaskcore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.usernamedrew.edutaskcommon.dto.generation.TaskGenerationCreateRequest;
import ru.usernamedrew.edutaskcommon.dto.generation.TaskGenerationResponse;
import ru.usernamedrew.edutaskcommon.dto.generation.TaskGenerationStatus;
import ru.usernamedrew.edutaskcommon.event.generation.TaskGenerationRequestedEvent;
import ru.usernamedrew.edutaskcore.config.TaskGenerationProperties;
import ru.usernamedrew.edutaskcore.entity.GenerationRequest;
import ru.usernamedrew.edutaskcore.entity.UserProfile;
import ru.usernamedrew.edutaskcore.exception.ResourceNotFoundException;
import ru.usernamedrew.edutaskcore.kafka.producer.TaskGenerationRequestProducer;
import ru.usernamedrew.edutaskcore.mapper.TaskGenerationMapper;
import ru.usernamedrew.edutaskcore.repository.GenerationRequestRepository;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskGenerationService {
    private static final String ENQUEUE_ERROR_MESSAGE = "Failed to enqueue task generation request";

    private final GenerationRequestRepository generationRequestRepository;
    private final UserProfileService userProfileService;
    private final TopicService topicService;
    private final TaskGenerationRequestProducer requestProducer;
    private final TaskGenerationStatusService statusService;
    private final TaskGenerationProperties properties;
    private final TaskGenerationMapper taskGenerationMapper;

    @Transactional
    public TaskGenerationResponse createGeneration(TaskGenerationCreateRequest request, Jwt jwt) {
        UserProfile user = userProfileService.resolveCurrentUser(jwt);
        validateTopics(request.topicIds());

        String provider = trimToNull(request.provider());
        String model = trimToNull(request.model());

        GenerationRequest generationRequest = new GenerationRequest();
        generationRequest.setUser(user);
        generationRequest.setUserPrompt(request.prompt().trim());
        generationRequest.setFinalPrompt(request.prompt().trim());
        generationRequest.setStatus(TaskGenerationStatus.QUEUED);
        generationRequest.setModelProvider(provider);
        generationRequest.setModelName(model);

        GenerationRequest savedRequest = generationRequestRepository.saveAndFlush(generationRequest);
        TaskGenerationRequestedEvent event = new TaskGenerationRequestedEvent(
            UUID.randomUUID(),
            savedRequest.getId(),
            user.getId(),
            request.prompt().trim(),
            request.topicIds() == null ? Set.of() : Set.copyOf(request.topicIds()),
            request.difficulty(),
            provider,
            model,
            request.temperature() == null ? properties.getDefaultTemperature() : request.temperature(),
            OffsetDateTime.now(),
            properties.getSchemaVersion()
        );

        enqueueAfterCommit(event);

        return taskGenerationMapper.toResponse(savedRequest);
    }

    @Transactional(readOnly = true)
    public TaskGenerationResponse getGeneration(UUID id, Jwt jwt) {
        UserProfile currentUser = userProfileService.resolveCurrentUser(jwt);
        GenerationRequest generationRequest = findDetailed(id);
        assertCanRead(generationRequest, currentUser);
        return taskGenerationMapper.toResponse(generationRequest);
    }

    @Transactional(readOnly = true)
    public GenerationRequest findDetailed(UUID id) {
        return generationRequestRepository.findDetailedById(id)
            .orElseThrow(() -> new ResourceNotFoundException("GenerationRequest not found: " + id));
    }

    private void validateTopics(Set<UUID> topicIds) {
        // TODO Подумать над валидацией топиков, пока пусто
        if (topicIds != null && !topicIds.isEmpty()) {
            topicService.resolveTopics(topicIds);
        }
    }

    private void enqueueAfterCommit(TaskGenerationRequestedEvent event) {
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

    private void enqueue(TaskGenerationRequestedEvent event) {
        try {
            requestProducer.send(event)
                .orTimeout(properties.getKafkaSendTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        markEnqueueFailed(event.requestId(), exception);
                    }
                });
        } catch (RuntimeException exception) {
            markEnqueueFailed(event.requestId(), exception);
        }
    }

    private void markEnqueueFailed(UUID requestId, Throwable exception) {
        log.error("Failed to enqueue task generation request, requestId={}", requestId, exception);
        statusService.markFailed(requestId, ENQUEUE_ERROR_MESSAGE);
    }

    private void assertCanRead(GenerationRequest generationRequest, UserProfile currentUser) {
        boolean owner = generationRequest.getUser().getId().equals(currentUser.getId());
        boolean admin = currentUser.getRole() == UserProfile.UserRole.ADMIN;
        if (!owner && !admin) {
            throw new AccessDeniedException("Access denied to generation request: " + generationRequest.getId());
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
