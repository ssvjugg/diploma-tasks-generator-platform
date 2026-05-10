package ru.usernamedrew.edutaskcore.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.usernamedrew.edutaskcore.entity.Task;

import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {
    @EntityGraph(attributePaths = {"author", "topics", "topics.parent", "supportedLanguages"})
    @Query("select t from Task t where t.id = :id")
    Optional<Task> findDetailedById(@Param("id") UUID id);
}
