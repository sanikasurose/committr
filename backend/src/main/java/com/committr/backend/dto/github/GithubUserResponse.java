package com.committr.backend.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubUserResponse(
    Long id,
    String login,
    @JsonProperty("avatar_url") String avatarUrl
) {
}
