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
@Table(
    name = "chat_message",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_chat_message_session_order",
        columnNames = {"session_id", "message_order"}
    )
)
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChatMessageRole role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "message_order", nullable = false)
    private int messageOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChatMessageStatus status;

    @Column(name = "model_provider", length = 50)
    private String modelProvider;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public enum ChatMessageRole {
        USER, ASSISTANT, SYSTEM
    }

    public enum ChatMessageStatus {
        CREATED, SENT, COMPLETED, FAILED
    }
}

