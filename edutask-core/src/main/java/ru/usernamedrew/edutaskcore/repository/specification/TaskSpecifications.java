package ru.usernamedrew.edutaskcore.repository.specification;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import ru.usernamedrew.edutaskcommon.dto.task.TaskDifficulty;
import ru.usernamedrew.edutaskcore.entity.Task;

import java.util.UUID;

public final class TaskSpecifications {
    private TaskSpecifications() {
    }

    public static Specification<Task> titleOrStatementContains(String query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (query == null || query.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            String pattern = "%" + query.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("statement")), pattern)
            );
        };
    }

    public static Specification<Task> hasDifficulty(TaskDifficulty difficulty) {
        return (root, criteriaQuery, criteriaBuilder) -> difficulty == null
            ? criteriaBuilder.conjunction()
            : criteriaBuilder.equal(root.get("difficulty"), difficulty);
    }

    public static Specification<Task> hasAuthor(UUID authorId) {
        return (root, criteriaQuery, criteriaBuilder) -> authorId == null
            ? criteriaBuilder.conjunction()
            : criteriaBuilder.equal(root.get("author").get("id"), authorId);
    }

    public static Specification<Task> hasTopic(UUID topicId) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (topicId == null) {
                return criteriaBuilder.conjunction();
            }
            criteriaQuery.distinct(true);
            return criteriaBuilder.equal(root.join("topics", JoinType.INNER).get("id"), topicId);
        };
    }

    public static Specification<Task> supportsLanguage(String languageCode) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (languageCode == null || languageCode.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            criteriaQuery.distinct(true);
            return criteriaBuilder.equal(
                criteriaBuilder.lower(root.join("supportedLanguages", JoinType.INNER).get("code")),
                languageCode.trim().toLowerCase()
            );
        };
    }
}
