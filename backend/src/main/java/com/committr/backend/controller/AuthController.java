package com.committr.backend.controller;

import com.committr.backend.dto.github.GithubUserLogPayload;
import com.committr.backend.github.GitHubOAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final GitHubOAuthService gitHubOAuthService;

    public AuthController(GitHubOAuthService gitHubOAuthService) {
        this.gitHubOAuthService = gitHubOAuthService;
    }

    @GetMapping("/api/auth/login")
    public ResponseEntity<Void> login() {
        return ResponseEntity.status(302)
            .location(gitHubOAuthService.buildAuthorizationUri())
            .build();
    }

    @GetMapping("/api/auth/callback")
    public ResponseEntity<GithubUserLogPayload> callback(
        @RequestParam(value = "code", required = false) String code
    ) {
        GithubUserLogPayload payload = gitHubOAuthService.completeLogin(code);
        return ResponseEntity.ok(payload);
    }
}
