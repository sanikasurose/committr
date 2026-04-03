package com.committr.backend.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Normalized user fields logged and returned after OAuth callback.
 */
public record GithubUserLogPayload(
    @JsonProperty("githubId") String githubId,
    @JsonProperty("username") String username,
    @JsonProperty("avatarUrl") String avatarUrl
) {
}
