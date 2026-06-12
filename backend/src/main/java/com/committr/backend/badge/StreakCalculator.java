package com.committr.backend.badge;

import java.time.LocalDate;
import java.util.List;
import java.util.TreeSet;

public final class StreakCalculator {

    private StreakCalculator() {}

    /**
     * Returns the longest run of consecutive weeks (7 days apart).
     * Deduplicates input. Empty list → 0. Single week → 1.
     */
    public static int longestConsecutiveWeeks(List<LocalDate> weekStarts) {
        if (weekStarts == null || weekStarts.isEmpty()) {
            return 0;
        }

        // Deduplicate and sort
        TreeSet<LocalDate> sorted = new TreeSet<>(weekStarts);

        int maxStreak = 1;
        int currentStreak = 1;
        LocalDate prev = null;

        for (LocalDate date : sorted) {
            if (prev != null) {
                if (prev.plusDays(7).equals(date)) {
                    currentStreak++;
                    if (currentStreak > maxStreak) {
                        maxStreak = currentStreak;
                    }
                } else {
                    currentStreak = 1;
                }
            }
            prev = date;
        }

        return maxStreak;
    }
}
