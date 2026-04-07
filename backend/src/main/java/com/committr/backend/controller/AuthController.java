package com.committr.backend.controller;

import com.committr.backend.config.SessionProperties;
import com.committr.backend.dto.auth.AuthUserResponse;
import com.committr.backend.dto.github.GithubUserLogPayload;
import com.committr.backend.github.GitHubOAuthService;
import com.committr.backend.session.RedisSessionService;
import com.committr.backend.session.SessionAuthenticationToken;
import com.committr.backend.session.SessionCookieFactory;
import com.committr.backend.session.SessionUserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.WebUtils;

@RestController
public class AuthController {

    private final GitHubOAuthService gitHubOAuthService;
    private final RedisSessionService redisSessionService;
    private final SessionCookieFactory sessionCookieFactory;
    private final SessionProperties sessionProperties;

    public AuthController(
        GitHubOAuthService gitHubOAuthService,
        RedisSessionService redisSessionService,
        SessionCookieFactory sessionCookieFactory,
        SessionProperties sessionProperties
    ) {
        this.gitHubOAuthService = gitHubOAuthService;
        this.redisSessionService = redisSessionService;
        this.sessionCookieFactory = sessionCookieFactory;
        this.sessionProperties = sessionProperties;
    }

    @GetMapping("/api/auth/login")
    public ResponseEntity<Void> login() {
        return ResponseEntity.status(302)
            .location(gitHubOAuthService.buildAuthorizationUri())
            .build();
    }

    @GetMapping("/api/auth/callback")
    public ResponseEntity<GithubUserLogPayload> callback(
        @RequestParam(value = "code", required = false) String code,
        HttpServletResponse response
    ) {
        var completion = gitHubOAuthService.completeLogin(code);
        String sessionId = redisSessionService.createSession(completion.sessionUser());
        ResponseCookie cookie = sessionCookieFactory.createSessionCookie(sessionId);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookieFactory.createJSessionIdDeleteCookie().toString());
        return ResponseEntity.ok(completion.payload());
    }

    @GetMapping("/api/auth/me")
    public AuthUserResponse me(Authentication authentication) {
        SessionUserDto user = resolveSessionUser(authentication);
        return new AuthUserResponse(user.username(), user.avatarUrl());
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        var cookie = WebUtils.getCookie(request, sessionProperties.cookieName());
        if (cookie != null && cookie.getValue() != null && !cookie.getValue().isBlank()) {
            redisSessionService.deleteSession(cookie.getValue());
        }
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookieFactory.createDeleteCookie().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookieFactory.createJSessionIdDeleteCookie().toString());
        return ResponseEntity.ok().build();
    }

    private static SessionUserDto resolveSessionUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (authentication instanceof SessionAuthenticationToken sessionToken) {
            return (SessionUserDto) sessionToken.getPrincipal();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
}
