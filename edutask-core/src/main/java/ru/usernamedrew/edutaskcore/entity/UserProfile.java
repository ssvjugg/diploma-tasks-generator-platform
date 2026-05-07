package ru.usernamedrew.edutaskcore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_profile")
public class UserProfile extends BaseEntity {
    @Column(name = "keycloak_id", nullable = false, unique = true)
    private String keycloakId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UserRole role;

    public enum UserRole {
        STUDENT, TEACHER, ADMIN
    }
}
