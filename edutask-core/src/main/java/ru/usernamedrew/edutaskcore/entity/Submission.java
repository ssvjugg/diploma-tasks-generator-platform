package ru.usernamedrew.edutaskcore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.usernamedrew.edutaskcommon.event.judge.JudgeSubmissionStatus;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "submission")
public class Submission extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "language_id", nullable = false)
    private ProgrammingLanguage language;

    @Column(name = "source_code", nullable = false, columnDefinition = "text")
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private JudgeSubmissionStatus status;

    @Column(nullable = false)
    private int score = 0;

    @Column(name = "max_score", nullable = false)
    private int maxScore = 0;

    @Column(name = "passed_tests", nullable = false)
    private int passedTests = 0;

    @Column(name = "total_tests", nullable = false)
    private int totalTests = 0;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubmissionTestResult> testResults = new ArrayList<>();

    public void addTestResult(SubmissionTestResult testResult) {
        testResults.add(testResult);
        testResult.setSubmission(this);
    }
}
