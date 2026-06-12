package com.committr.backend.badge;

// Internal record — not exposed as JSON
record BadgeData(String username, long totalLinesShipped, int longestStreak,
                 String topLanguage, double teamSharePercent) {}
