package ru.usernamedrew.edutaskcore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import ru.usernamedrew.edutaskcommon.event.judge.JudgeSubmissionStatus;

import java.math.BigDecimal;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id")
    private TestCase testCase;

    @Column(name = "test_case_index", nullable = false)
    private int testCaseIndex;

    @Column(name = "is_hidden", nullable = false)
    private boolean hidden;

    @Column(name = "input_data", columnDefinition = "text")
    private String inputData;

    @Column(name = "expected_output", columnDefinition = "text")
    private String expectedOutput;

    @Column(nullable = false)
    private int points = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private JudgeSubmissionStatus status;

    @Column(name = "actual_output", columnDefinition = "text")
    private String stdout;

    @Column(columnDefinition = "text")
    private String stderr;

    @Column(name = "compile_output", columnDefinition = "text")
    private String compileOutput;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "judge_token")
    private String judge0Token;

    @Column(precision = 10, scale = 3)
    private BigDecimal time;

    @Column
    private Integer memory;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

}
