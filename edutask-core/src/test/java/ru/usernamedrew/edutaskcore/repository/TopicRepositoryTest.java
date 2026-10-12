package ru.usernamedrew.edutaskcore.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.usernamedrew.edutaskcore.entity.Topic;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TopicRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private TopicRepository topicRepository;

    @Test
    void havingTopics_whenSave_thenSuccessfullyFindById() {
        Topic topic1 = new Topic();
        topic1.setName("topic1");
        topic1.setParent(null);
        Topic topic2 = new Topic();
        topic2.setName("topic2");
        topic2.setParent(topic1);

        topicRepository.saveAndFlush(topic1);
        topicRepository.saveAndFlush(topic2);
        entityManager.clear();

        Topic found1 = topicRepository.findById(topic1.getId()).get();
        Topic found2 = topicRepository.findById(topic2.getId()).get();

        assertEquals(topic1.getName(), found1.getName());
        assertEquals(topic2.getName(), found2.getName());
        assertEquals(topic2.getParent().getId(), found2.getParent().getId());
    }
}
