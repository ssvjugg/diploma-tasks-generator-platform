package ru.usernamedrew.edutaskcore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.usernamedrew.edutaskcommon.dto.user.UserProfileResponse;
import ru.usernamedrew.edutaskcore.entity.UserProfile;
import ru.usernamedrew.edutaskcore.exception.ResourceNotFoundException;
import ru.usernamedrew.edutaskcore.mapper.UserProfileMapper;
import ru.usernamedrew.edutaskcore.repository.UserProfileRepository;
import ru.usernamedrew.edutaskcore.security.KeycloakRoleExtractor;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserProfileRepository userProfileRepository;
    private final KeycloakRoleExtractor keycloakRoleExtractor;
    private final UserProfileMapper userProfileMapper;

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(Jwt jwt) {
        UserProfile userProfile = userProfileRepository.findByKeycloakId(jwt.getSubject())
            .orElseThrow(() -> new ResourceNotFoundException("UserProfile not found for current Keycloak user"));
        return userProfileMapper.toResponse(userProfile);
    }

    @Transactional
    public UserProfileResponse registerOrSyncCurrentUser(Jwt jwt) {
        return userProfileMapper.toResponse(resolveCurrentUser(jwt));
    }

    @Transactional
    public UserProfile resolveCurrentUser(Jwt jwt) {
        UserProfile.UserRole role = resolveEduTaskRole(jwt);
        return userProfileRepository.findByKeycloakId(jwt.getSubject())
            .map(userProfile -> syncRole(userProfile, role))
            .orElseGet(() -> createUserProfile(jwt, role));
    }

    private UserProfile.UserRole resolveEduTaskRole(Jwt jwt) {
        return keycloakRoleExtractor.extractPrimaryEduTaskRole(jwt)
            .orElseThrow(() -> new AccessDeniedException("Missing EduTask role in Keycloak token"));
    }

    private UserProfile syncRole(UserProfile userProfile, UserProfile.UserRole role) {
        if (userProfile.getRole() != role) {
            userProfile.setRole(role);
        }
        return userProfile;
    }

    private UserProfile createUserProfile(Jwt jwt, UserProfile.UserRole role) {
        UserProfile userProfile = new UserProfile();
        userProfile.setKeycloakId(jwt.getSubject());
        userProfile.setRole(role);
        return userProfileRepository.save(userProfile);
    }
}
