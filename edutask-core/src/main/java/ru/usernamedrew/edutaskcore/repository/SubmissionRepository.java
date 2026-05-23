package ru.usernamedrew.edutaskcore.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.usernamedrew.edutaskcore.entity.Submission;

import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    @EntityGraph(attributePaths = {
        "user",
        "task",
        "language",
        "testResults",
        "testResults.testCase"
    })
    Optional<Submission> findDetailedById(UUID id);
}
