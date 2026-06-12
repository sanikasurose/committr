package com.committr.backend.dto.analytics;

public record ContributorShareDto(
    String login,
    String avatarUrl,
    int commitCount,
    int linesAdded,
    int linesDeleted,
    double sharePercent
) {}
