package com.committr.backend.dto.github;

import java.time.Instant;

public record PullRequestSummary(
    int number,
    Instant openedAt,
    Instant mergedAt
) {}
