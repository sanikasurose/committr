package com.committr.backend.dto.analytics;

import java.time.LocalDate;

public record CommitFrequencyDto(
    LocalDate weekStart,
    int commitCount,
    int linesAdded,
    int linesDeleted
) {}
