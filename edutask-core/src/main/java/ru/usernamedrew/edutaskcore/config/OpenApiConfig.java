package ru.usernamedrew.edutaskcore.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
@OpenAPIDefinition(
    info = @Info(
        title = "EduTask Core API",
        version = "0.0.1",
        description = "API основной логики банка задач по программированию",
        contact = @Contact(name = "EduTask"),
        license = @License(name = "Internal educational project")
    ),
    servers = @Server(url = "/", description = "Current environment"),
    security = @SecurityRequirement(name = "bearerAuth")
)
public class OpenApiConfig {
}
