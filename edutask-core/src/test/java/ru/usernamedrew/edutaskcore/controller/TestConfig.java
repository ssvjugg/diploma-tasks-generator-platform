package ru.usernamedrew.edutaskcore.controller;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import ru.usernamedrew.edutaskcore.config.SecurityConfig;
import ru.usernamedrew.edutaskcore.security.KeycloakRoleExtractor;

@TestConfiguration
@ImportAutoConfiguration({
    SecurityAutoConfiguration.class,
    ServletWebSecurityAutoConfiguration.class
})
@Import({SecurityConfig.class})
public class TestConfig {
    @Bean
    public KeycloakRoleExtractor keycloakRoleExtractor() {
        return new KeycloakRoleExtractor("TEST_CLIENT");
    }
}
