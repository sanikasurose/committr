package com.committr.backend.dto.github;

public record ContributorSummary(
    String login,
    String avatarUrl,
    int contributions
) {}
