package ru.usernamedrew.edutaskcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.usernamedrew.edutaskcore.entity.TestCase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {
    List<TestCase> findByTaskIdOrderByCreatedAtAscIdAsc(UUID taskId);

    List<TestCase> findByTaskIdAndHiddenFalseOrderByCreatedAtAscIdAsc(UUID taskId);

    Optional<TestCase> findByIdAndTaskId(UUID id, UUID taskId);
}
