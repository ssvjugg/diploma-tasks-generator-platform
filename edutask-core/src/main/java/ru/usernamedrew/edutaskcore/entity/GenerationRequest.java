package ru.usernamedrew.edutaskcore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "generation_request")
public class GenerationRequest extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile user;

    @Column(name = "user_prompt", columnDefinition = "text")
    private String userPrompt;

    @Column(name = "final_prompt", columnDefinition = "text")
    private String finalPrompt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GenerationRequestStatus status;

    @Column(name = "model_provider", length = 50)
    private String modelProvider;

    @Column(name = "model_name")
    private String modelName;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_task_id", unique = true)
    private Task generatedTask;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    public enum GenerationRequestStatus {
        QUEUED, PROCESSING, DONE, FAILED
    }
}

