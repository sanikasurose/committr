package com.committr.backend.dto.github;

import java.time.Instant;

public record CommitSummary(
    String sha,
    String authorLogin,
    String authorAvatarUrl,
    Instant committedAt,
    String message,
    int linesAdded,
    int linesDeleted,
    String language
) {}
