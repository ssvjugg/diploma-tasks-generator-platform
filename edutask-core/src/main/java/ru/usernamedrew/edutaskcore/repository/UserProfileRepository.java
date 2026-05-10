package ru.usernamedrew.edutaskcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.usernamedrew.edutaskcore.entity.UserProfile;

import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
}
