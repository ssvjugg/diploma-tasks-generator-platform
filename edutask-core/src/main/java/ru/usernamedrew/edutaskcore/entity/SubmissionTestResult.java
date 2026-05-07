package ru.usernamedrew.edutaskcore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "submission_test_result")
public class SubmissionTestResult {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubmissionTestResultStatus status;

    @Column(name = "actual_output", columnDefinition = "text")
    private String actualOutput;

    @Column(columnDefinition = "text")
    private String stderr;

    @Column(name = "compile_output", columnDefinition = "text")
    private String compileOutput;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "judge_token")
    private String judgeToken;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public enum SubmissionTestResultStatus {
        ACCEPTED, WRONG_ANSWER, TIME_LIMIT, MEMORY_LIMIT, RUNTIME_ERROR, COMPILATION_ERROR
    }
}

