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
}
