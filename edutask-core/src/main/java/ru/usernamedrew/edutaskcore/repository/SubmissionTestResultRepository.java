package ru.usernamedrew.edutaskcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.usernamedrew.edutaskcore.entity.SubmissionTestResult;

import java.util.UUID;

public interface SubmissionTestResultRepository extends JpaRepository<SubmissionTestResult, UUID> {
}
