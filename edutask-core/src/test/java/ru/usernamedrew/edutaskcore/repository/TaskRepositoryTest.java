package ru.usernamedrew.edutaskcore.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.usernamedrew.edutaskcommon.dto.task.TaskDifficulty;
import ru.usernamedrew.edutaskcore.entity.Task;
import ru.usernamedrew.edutaskcore.entity.Topic;
import ru.usernamedrew.edutaskcore.entity.UserProfile;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TaskRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private TopicRepository topicRepository;

    @Test
    void havingTask_whenSave_thenSuccessfullyFindDetailedById() {
        UserProfile userProfile = new UserProfile();
        userProfile.setKeycloakId("id");
        userProfile.setRole(UserProfile.UserRole.STUDENT);
        userProfileRepository.save(userProfile);

        Topic topic1 = new Topic();
        topic1.setName("topic1");
        Topic topic2 = new Topic();
        topic2.setName("topic2");
        topicRepository.save(topic1);
        topicRepository.save(topic2);

        Task task = new Task();
        task.setTitle("title");
        task.setStatement("statement");
        task.setInputFormat("");
        task.setOutputFormat("");
        task.setDifficulty(TaskDifficulty.EASY);
        task.setAuthor(userProfile);
        task.setTopics(Set.of(topic1, topic2));

        taskRepository.save(task);

        Task found = taskRepository.findDetailedById(task.getId()).get();

        assertEquals("title", found.getTitle());
        assertEquals("statement", found.getStatement());
        assertEquals("", found.getInputFormat());
        assertEquals("", found.getOutputFormat());
        assertEquals(TaskDifficulty.EASY, found.getDifficulty());
        assertEquals(userProfile, found.getAuthor());
        assertEquals(Set.of(topic1, topic2), found.getTopics());
    }
}
