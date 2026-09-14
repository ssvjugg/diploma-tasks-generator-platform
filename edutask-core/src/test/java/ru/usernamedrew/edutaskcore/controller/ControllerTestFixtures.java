package ru.usernamedrew.edutaskcore.controller;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class ControllerTestFixtures {
    public static final String KEYCLOAK_SUBJECT = "keycloak-user";
    public static final OffsetDateTime TIMESTAMP = OffsetDateTime.parse("2026-01-15T10:30:00Z");

    public static JwtRequestPostProcessor jwtWithRoles(String... roles) {
        List<GrantedAuthority> authorities = Arrays.stream(roles)
            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
            .map(SimpleGrantedAuthority::new)
            .map(GrantedAuthority.class::cast)
            .toList();

        return jwt()
            .jwt(jwt -> jwt.subject(KEYCLOAK_SUBJECT))
            .authorities(authorities);
    }
}
