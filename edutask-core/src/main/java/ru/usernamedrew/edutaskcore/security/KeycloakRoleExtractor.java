package ru.usernamedrew.edutaskcore.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import ru.usernamedrew.edutaskcore.entity.UserProfile;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class KeycloakRoleExtractor {
    private final String clientId;

    public KeycloakRoleExtractor(
        @Value("${edutask.security.keycloak.client-id:edutask-frontend}") String clientId
    ) {
        this.clientId = clientId;
    }

    public Set<String> extractRoleNames(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();
        collectRealmRoles(jwt, roles);
        collectClientRoles(jwt, roles);
        return roles.stream()
            .map(String::toUpperCase)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Optional<UserProfile.UserRole> extractPrimaryEduTaskRole(Jwt jwt) {
        Set<String> roles = extractRoleNames(jwt);
        if (roles.contains(UserProfile.UserRole.ADMIN.name())) {
            return Optional.of(UserProfile.UserRole.ADMIN);
        }
        if (roles.contains(UserProfile.UserRole.TEACHER.name())) {
            return Optional.of(UserProfile.UserRole.TEACHER);
        }
        if (roles.contains(UserProfile.UserRole.STUDENT.name())) {
            return Optional.of(UserProfile.UserRole.STUDENT);
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private void collectRealmRoles(Jwt jwt, Set<String> roles) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return;
        }
        Object realmRoles = realmAccess.get("roles");
        if (realmRoles instanceof Collection<?> collection) {
            collection.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .forEach(roles::add);
        }
    }

    private void collectClientRoles(Jwt jwt, Set<String> roles) {
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null) {
            return;
        }
        Object clientAccess = resourceAccess.get(clientId);
        if (!(clientAccess instanceof Map<?, ?> clientAccessMap)) {
            return;
        }
        Object clientRoles = clientAccessMap.get("roles");
        if (clientRoles instanceof Collection<?> collection) {
            collection.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .forEach(roles::add);
        }
    }
}
