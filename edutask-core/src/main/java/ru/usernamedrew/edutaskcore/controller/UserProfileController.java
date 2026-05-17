package ru.usernamedrew.edutaskcore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.usernamedrew.edutaskcommon.dto.user.UserProfileResponse;
import ru.usernamedrew.edutaskcore.service.UserProfileService;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Операции с профилем пользователя EduTask")
public class UserProfileController {
    private final UserProfileService userProfileService;

    @Operation(summary = "Получить текущий профиль", description = "Возвращает локальный профиль для текущего пользователя Keycloak.")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserProfileResponse getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return userProfileService.getCurrentUser(jwt);
    }

    @Operation(
        summary = "Зарегистрировать или синхронизировать текущий профиль",
        description = "Создает локальный профиль EduTask по Keycloak subject или обновляет роль по текущему JWT."
    )
    @PostMapping("/me/register")
    @PreAuthorize("isAuthenticated()")
    public UserProfileResponse registerCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return userProfileService.registerOrSyncCurrentUser(jwt);
    }
}
