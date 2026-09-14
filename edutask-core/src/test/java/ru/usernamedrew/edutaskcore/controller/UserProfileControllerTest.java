package ru.usernamedrew.edutaskcore.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.usernamedrew.edutaskcommon.dto.user.UserProfileResponse;
import ru.usernamedrew.edutaskcore.service.UserProfileService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.usernamedrew.edutaskcore.controller.ControllerTestFixtures.KEYCLOAK_SUBJECT;
import static ru.usernamedrew.edutaskcore.controller.ControllerTestFixtures.jwtWithRoles;

@WebMvcTest(controllers = UserProfileController.class)
@Import(TestConfig.class)
class UserProfileControllerTest {
    private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @Test
    void havingUserProfile_whenGetUserProfile_thenReturnUserProfile() throws Exception {
        when(userProfileService.getCurrentUser(any(Jwt.class)))
            .thenReturn(new UserProfileResponse(USER_ID, KEYCLOAK_SUBJECT, "TEACHER"));

        mockMvc.perform(get("/api/v1/users/me")
                .with(jwtWithRoles("TEACHER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(USER_ID.toString()))
            .andExpect(jsonPath("$.keycloakId").value(KEYCLOAK_SUBJECT))
            .andExpect(jsonPath("$.role").value("TEACHER"));

        ArgumentCaptor<Jwt> jwtCaptor = ArgumentCaptor.forClass(Jwt.class);
        verify(userProfileService).getCurrentUser(jwtCaptor.capture());
        assertThat(jwtCaptor.getValue().getSubject()).isEqualTo(KEYCLOAK_SUBJECT);
    }

    @Test
    void havingUserProfile_whenPostUserProfile_thenSyncIt() throws Exception {
        when(userProfileService.registerOrSyncCurrentUser(any(Jwt.class)))
            .thenReturn(new UserProfileResponse(USER_ID, KEYCLOAK_SUBJECT, "STUDENT"));

        mockMvc.perform(post("/api/v1/users/me/register")
                .with(jwtWithRoles("STUDENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(USER_ID.toString()))
            .andExpect(jsonPath("$.role").value("STUDENT"));

        verify(userProfileService).registerOrSyncCurrentUser(any(Jwt.class));
    }

    @Test
    void havingUnauthorized_whenGetUserProfile_thenStatusUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(userProfileService);
    }
}
