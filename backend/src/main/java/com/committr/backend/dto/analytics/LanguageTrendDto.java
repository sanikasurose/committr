package com.committr.backend.dto.analytics;

import java.time.LocalDate;
import java.util.Map;

public record LanguageTrendDto(
    LocalDate weekStart,
    Map<String, Integer> languageDistribution
) {}
