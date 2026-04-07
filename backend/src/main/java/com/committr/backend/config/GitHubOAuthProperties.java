package com.committr.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github.oauth")
public record GitHubOAuthProperties(
    String clientId,
    String clientSecret,
    String redirectUri,
    String scope,
    String userAgent,
    String authorizeUrl,
    String accessTokenUrl,
    String userApiUrl
) {
    public GitHubOAuthProperties {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException(
                "GITHUB_CLIENT_ID is missing or blank: set github.oauth.client-id / GITHUB_CLIENT_ID."
            );
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException(
                "GITHUB_CLIENT_SECRET is missing or blank: set github.oauth.client-secret / GITHUB_CLIENT_SECRET."
            );
        }
    }
}
