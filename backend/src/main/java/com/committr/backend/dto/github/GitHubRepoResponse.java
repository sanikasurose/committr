package com.committr.backend.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubRepoResponse(
    Long id,
    String name,
    @JsonProperty("full_name") String fullName,
    @JsonProperty("private") boolean isPrivate,
    @JsonProperty("html_url") String htmlUrl,
    Owner owner
) {
    public record Owner(String login) {}
}
