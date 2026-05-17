package ru.usernamedrew.edutaskcommon.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Профиль пользователя EduTask, связанный с учетной записью Keycloak")
public record UserProfileResponse(
    @Schema(description = "Локальный идентификатор пользователя EduTask")
    UUID id,

    @Schema(description = "Идентификатор пользователя в Keycloak")
    String keycloakId,

    @Schema(description = "Текущая роль пользователя в EduTask", example = "TEACHER")
    String role
) {
}
