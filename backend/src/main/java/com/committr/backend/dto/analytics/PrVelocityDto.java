package com.committr.backend.dto.analytics;

import java.time.LocalDate;

public record PrVelocityDto(
    LocalDate weekStart,
    double avgMergeHours
) {}
