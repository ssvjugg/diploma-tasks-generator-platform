package ru.usernamedrew.edutaskcore.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.usernamedrew.edutaskcore.entity.UserProfile;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class UserProfileRepositoryTest extends BaseRepositoryTest {

    @Autowired
    UserProfileRepository userProfileRepository;

    @Test
    void saveUserProfile() {
        UserProfile userProfile = new UserProfile();
        userProfile.setKeycloakId("id");
        userProfile.setRole(UserProfile.UserRole.STUDENT);

        UserProfile saved = userProfileRepository.save(userProfile);

        assertThat(saved.getKeycloakId()).isEqualTo("id");
    }
}
