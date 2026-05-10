package ru.usernamedrew.edutaskcore.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "EduTask Core API",
        version = "0.0.1",
        description = "API основной логики банка задач по программированию",
        contact = @Contact(name = "EduTask"),
        license = @License(name = "Internal educational project")
    ),
    servers = @Server(url = "/", description = "Current environment")
)
public class OpenApiConfig {
}
