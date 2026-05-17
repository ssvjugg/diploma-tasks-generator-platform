package ru.usernamedrew.edutaskcore.security;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private final KeycloakRoleExtractor keycloakRoleExtractor;

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        return keycloakRoleExtractor.extractRoleNames(jwt).stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toUnmodifiableSet());
    }
}
