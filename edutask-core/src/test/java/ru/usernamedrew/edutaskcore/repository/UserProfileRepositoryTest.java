package ru.usernamedrew.edutaskcore.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.usernamedrew.edutaskcore.entity.UserProfile;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserProfileRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    void havingUserProfile_whenSave_thenSuccessfullyFindById() {
        UserProfile userProfile = new UserProfile();
        userProfile.setKeycloakId("id");
        userProfile.setRole(UserProfile.UserRole.STUDENT);

        userProfileRepository.save(userProfile);
        userProfileRepository.flush();
        entityManager.clear();

        UserProfile found = userProfileRepository.findById(userProfile.getId()).get();

        assertEquals("id", found.getKeycloakId());
        assertEquals(UserProfile.UserRole.STUDENT, found.getRole());
    }

    @Test
    void havingUserProfile_whenSave_thenSuccessfullyFindByKeycloakId() {
        UserProfile userProfile = new UserProfile();
        userProfile.setKeycloakId("keycloakId");
        userProfile.setRole(UserProfile.UserRole.ADMIN);

        userProfileRepository.save(userProfile);
        userProfileRepository.flush();
        entityManager.clear();

        UserProfile found = userProfileRepository.findByKeycloakId("keycloakId").get();

        assertEquals("keycloakId", found.getKeycloakId());
        assertEquals(UserProfile.UserRole.ADMIN, found.getRole());
    }
}
