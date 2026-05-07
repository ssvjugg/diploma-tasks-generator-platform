package ru.usernamedrew.edutaskcore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "task")
public class Task extends BaseEntity {
    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String statement;

    @Column(name = "input_format", columnDefinition = "text")
    private String inputFormat;

    @Column(name = "output_format", columnDefinition = "text")
    private String outputFormat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskDifficulty difficulty;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserProfile author;

    @ManyToMany
    @JoinTable(
        name = "task_topic",
        joinColumns = @JoinColumn(name = "task_id"),
        inverseJoinColumns = @JoinColumn(name = "topic_id")
    )
    private Set<Topic> topics = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "task_language",
        joinColumns = @JoinColumn(name = "task_id"),
        inverseJoinColumns = @JoinColumn(name = "language_id")
    )
    private Set<ProgrammingLanguage> supportedLanguages = new HashSet<>();

    public enum TaskDifficulty {
        EASY, MEDIUM, HARD
    }
}
