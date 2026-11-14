package com.auth.service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Endpoints about the signed-in user. The login itself is handled by Spring Security.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

// ------ GET /api/auth/me ------
    // Returns the signed-in GitHub user.
    @GetMapping("/me")
    public CurrentUser me(@AuthenticationPrincipal OAuth2User user) {
        // GitHub's user JSON uses these attribute names.
        String login = user.getAttribute("login");
        String name = user.getAttribute("name");
        String avatarUrl = user.getAttribute("avatar_url");

        return new CurrentUser(login, name, avatarUrl);
    }

    // Only the fields the frontend needs. Never return the GitHub token.
    public record CurrentUser(String login, String name, String avatarUrl) {
    }
}
