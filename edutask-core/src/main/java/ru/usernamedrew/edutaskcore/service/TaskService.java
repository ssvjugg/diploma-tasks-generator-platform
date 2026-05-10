package ru.usernamedrew.edutaskcore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.usernamedrew.edutaskcommon.dto.task.TaskCreateRequest;
import ru.usernamedrew.edutaskcommon.dto.task.TaskResponse;
import ru.usernamedrew.edutaskcommon.dto.task.TaskSearchRequest;
import ru.usernamedrew.edutaskcommon.dto.task.TaskSummary;
import ru.usernamedrew.edutaskcommon.dto.task.TaskUpdateRequest;
import ru.usernamedrew.edutaskcore.entity.ProgrammingLanguage;
import ru.usernamedrew.edutaskcore.entity.Task;
import ru.usernamedrew.edutaskcore.entity.Topic;
import ru.usernamedrew.edutaskcore.exception.ResourceNotFoundException;
import ru.usernamedrew.edutaskcore.mapper.TaskMapper;
import ru.usernamedrew.edutaskcore.repository.ProgrammingLanguageRepository;
import ru.usernamedrew.edutaskcore.repository.TaskRepository;
import ru.usernamedrew.edutaskcore.repository.TopicRepository;
import ru.usernamedrew.edutaskcore.repository.UserProfileRepository;
import ru.usernamedrew.edutaskcore.repository.projection.TaskSummaryProjection;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static ru.usernamedrew.edutaskcore.repository.specification.TaskSpecifications.hasAuthor;
import static ru.usernamedrew.edutaskcore.repository.specification.TaskSpecifications.hasDifficulty;
import static ru.usernamedrew.edutaskcore.repository.specification.TaskSpecifications.hasTopic;
import static ru.usernamedrew.edutaskcore.repository.specification.TaskSpecifications.supportsLanguage;
import static ru.usernamedrew.edutaskcore.repository.specification.TaskSpecifications.titleOrStatementContains;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserProfileRepository userProfileRepository;
    private final TopicRepository topicRepository;
    private final ProgrammingLanguageRepository programmingLanguageRepository;
    private final TaskMapper taskMapper;

    @Transactional(readOnly = true)
    public Page<TaskSummary> findTasks(TaskSearchRequest request, Pageable pageable) {
        Specification<Task> specification = Specification
            .where(titleOrStatementContains(request.query()))
            .and(hasDifficulty(request.difficulty()))
            .and(hasAuthor(request.authorId()))
            .and(hasTopic(request.topicId()))
            .and(supportsLanguage(request.languageCode()));

        Page<TaskSummaryProjection> taskPage = taskRepository.findBy(
            specification,
            query -> query
                .as(TaskSummaryProjection.class)
                .project("id", "title", "difficulty")
                .page(pageable)
        );

        return taskPage.map(task -> new TaskSummary(
            task.getId(),
            task.getTitle(),
            task.getDifficulty()
        ));
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID id) {
        Task task = findDetailedTask(id);
        return taskMapper.toResponse(task);
    }

    @Transactional
    public TaskResponse createTask(TaskCreateRequest request) {
        Task task = new Task();
        task.setTitle(request.title().trim());
        task.setStatement(request.statement().trim());
        task.setInputFormat(request.inputFormat());
        task.setOutputFormat(request.outputFormat());
        task.setDifficulty(request.difficulty());
        task.setAuthor(userProfileRepository.findById(request.authorId())
            .orElseThrow(() -> new ResourceNotFoundException("UserProfile not found: " + request.authorId())));
        task.setTopics(resolveTopics(request.topicIds()));
        task.setSupportedLanguages(resolveLanguages(request.languageIds()));

        Task savedTask = taskRepository.saveAndFlush(task);
        return taskMapper.toResponse(savedTask);
    }

    @Transactional
    public TaskResponse patchTask(UUID id, TaskUpdateRequest request) {
        Task task = findDetailedTask(id);

        if (request.title() != null) {
            task.setTitle(request.title().trim());
        }
        if (request.statement() != null) {
            task.setStatement(request.statement().trim());
        }
        if (request.inputFormat() != null) {
            task.setInputFormat(request.inputFormat());
        }
        if (request.outputFormat() != null) {
            task.setOutputFormat(request.outputFormat());
        }
        if (request.difficulty() != null) {
            task.setDifficulty(request.difficulty());
        }
        if (request.authorId() != null) {
            task.setAuthor(userProfileRepository.findById(request.authorId())
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile not found: " + request.authorId())));
        }
        if (request.topicIds() != null) {
            task.setTopics(resolveTopics(request.topicIds()));
        }
        if (request.languageIds() != null) {
            task.setSupportedLanguages(resolveLanguages(request.languageIds()));
        }

        taskRepository.flush();
        return taskMapper.toResponse(task);
    }

    @Transactional
    public void deleteTask(UUID id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task not found: " + id);
        }
        taskRepository.deleteById(id);
    }

    private Task findDetailedTask(UUID id) {
        return taskRepository.findDetailedById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
    }

    private Set<Topic> resolveTopics(Set<UUID> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Topic> topics = new HashSet<>(topicRepository.findAllById(topicIds));
        if (topics.size() != topicIds.size()) {
            throw new ResourceNotFoundException("One or more topics were not found");
        }
        return topics;
    }

    private Set<ProgrammingLanguage> resolveLanguages(Set<Integer> languageIds) {
        if (languageIds == null || languageIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<ProgrammingLanguage> languages = new HashSet<>(programmingLanguageRepository.findAllById(languageIds));
        if (languages.size() != languageIds.size()) {
            throw new ResourceNotFoundException("One or more programming languages were not found");
        }
        return languages;
    }
}
