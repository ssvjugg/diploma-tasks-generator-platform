package ru.usernamedrew.edutaskcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.usernamedrew.edutaskcore.entity.Topic;

import java.util.UUID;

public interface TopicRepository extends JpaRepository<Topic, UUID> {
}
