package ru.usernamedrew.edutaskcore.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import ru.usernamedrew.edutaskcommon.dto.task.TaskDifficulty;
import ru.usernamedrew.edutaskcore.entity.Task;
import ru.usernamedrew.edutaskcore.entity.Topic;
import ru.usernamedrew.edutaskcore.entity.UserProfile;
import ru.usernamedrew.edutaskcore.repository.projection.TaskSummaryProjection;
import ru.usernamedrew.edutaskcore.repository.specification.TaskSpecifications;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TaskRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    private UUID taskId;
    private UUID topicId;
    private UUID userId;

    @BeforeEach
    public void setUp() {
        UserProfile userProfile = new UserProfile();
        userProfile.setKeycloakId("id");
        userProfile.setRole(UserProfile.UserRole.STUDENT);
        entityManager.persist(userProfile);

        userId = userProfile.getId();

        Topic topic1 = new Topic();
        topic1.setName("Math");
        entityManager.persist(topic1);

        Topic topic2 = new Topic();
        topic2.setName("Java");
        entityManager.persist(topic2);

        topicId = topic2.getId();

        Task task1 = new Task();
        task1.setTitle("Intro to Algebra");
        task1.setStatement("Solve x + 2 = 5");
        task1.setDifficulty(TaskDifficulty.EASY);
        task1.setAuthor(userProfile);
        task1.setTopics(Set.of(topic1));
        entityManager.persist(task1);

        taskId = task1.getId();

        Task task2 = new Task();
        task2.setTitle("Advanced Algorithms");
        task2.setStatement("Find the shortest path in a graph using Java");
        task2.setDifficulty(TaskDifficulty.HARD);
        task2.setAuthor(userProfile);
        task2.setTopics(Set.of(topic1, topic2));
        entityManager.persist(task2);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Поиск задачи по id")
    void havingTasks_whenFindDetailedById_thenReturnCorrectTask() {
        Task found = taskRepository.findDetailedById(taskId).get();

        assertEquals("Intro to Algebra", found.getTitle());
        assertEquals("Solve x + 2 = 5", found.getStatement());
        assertEquals(TaskDifficulty.EASY, found.getDifficulty());
        assertEquals("id", found.getAuthor().getKeycloakId());
        assertEquals(1, found.getTopics().size());
    }

    @Test
    @DisplayName("Фильтрация по заголовку/условию и сложности")
    void havingTasks_whenFindByTitleOrStatementAndDifficulty_thenReturnProjectedTask() {
        Specification<Task> spec = Specification
            .where(TaskSpecifications.titleOrStatementContains("Intro"))
            .and(TaskSpecifications.hasDifficulty(TaskDifficulty.EASY));

        List<TaskSummaryProjection> found = taskRepository.findBy(
            spec,
            query -> query
                .as(TaskSummaryProjection.class)
                .all()
        );

        assertEquals(1, found.size());

        TaskSummaryProjection foundProjected = found.get(0);
        assertEquals("Intro to Algebra", foundProjected.getTitle());
        assertEquals(TaskDifficulty.EASY, foundProjected.getDifficulty());
    }

    @Test
    @DisplayName("Фильтрация по теме с отсутствием другого фильтра")
    void havingTasks_whenFindByTopic_thenReturnProjectedTask() {
        Specification<Task> spec = Specification
            .where(TaskSpecifications.hasTopic(topicId))
            .and(TaskSpecifications.hasDifficulty(null));

        List<TaskSummaryProjection> found = taskRepository.findBy(
            spec,
            query -> query
                .as(TaskSummaryProjection.class)
                .all()
        );

        assertEquals(1, found.size());

        TaskSummaryProjection foundProjected = found.get(0);
        assertEquals("Advanced Algorithms", foundProjected.getTitle());
        assertEquals(TaskDifficulty.HARD, foundProjected.getDifficulty());
    }

    @Test
    @DisplayName("Фильтрация по всем полям с пагинацией")
    void havingTasks_whenFindByAllFiltersAndPagination_thenReturnProjectedTask() {
        Specification<Task> spec = Specification
            .where(TaskSpecifications.titleOrStatementContains("Advanced"))
            .and(TaskSpecifications.hasDifficulty(TaskDifficulty.HARD))
            .and(TaskSpecifications.hasTopic(topicId))
            .and(TaskSpecifications.hasAuthor(userId));

        PageRequest pageable = PageRequest.of(0, 10);

        Page<TaskSummaryProjection> found = taskRepository.findBy(
            spec,
            query -> query
                .as(TaskSummaryProjection.class)
                .page(pageable)
        );

        assertEquals(1, found.getContent().size());
        assertEquals("Advanced Algorithms", found.getContent().get(0).getTitle());
    }
}
