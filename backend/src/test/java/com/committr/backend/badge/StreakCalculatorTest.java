package com.committr.backend.badge;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class StreakCalculatorTest {

    @Test
    void emptyList_returnsZero() {
        assertThat(StreakCalculator.longestConsecutiveWeeks(List.of())).isEqualTo(0);
    }

    @Test
    void singleWeek_returnsOne() {
        assertThat(StreakCalculator.longestConsecutiveWeeks(
            List.of(LocalDate.of(2024, 1, 8))
        )).isEqualTo(1);
    }

    @Test
    void twoConsecutiveWeeks_returnsTwo() {
        assertThat(StreakCalculator.longestConsecutiveWeeks(List.of(
            LocalDate.of(2024, 1, 8),
            LocalDate.of(2024, 1, 15)
        ))).isEqualTo(2);
    }

    @Test
    void threeConsecutiveWeeks_returnsThree() {
        assertThat(StreakCalculator.longestConsecutiveWeeks(List.of(
            LocalDate.of(2024, 1, 8),
            LocalDate.of(2024, 1, 15),
            LocalDate.of(2024, 1, 22)
        ))).isEqualTo(3);
    }

    @Test
    void gapBreaksStreak_returnsMaxBeforeGap() {
        // weeks 1,2 then gap then weeks 4,5,6
        assertThat(StreakCalculator.longestConsecutiveWeeks(List.of(
            LocalDate.of(2024, 1, 8),
            LocalDate.of(2024, 1, 15),
            LocalDate.of(2024, 2, 5),  // gap (skipped Jan 22 and Jan 29)
            LocalDate.of(2024, 2, 12),
            LocalDate.of(2024, 2, 19)
        ))).isEqualTo(3);
    }

    @Test
    void allNonConsecutive_returnsOne() {
        assertThat(StreakCalculator.longestConsecutiveWeeks(List.of(
            LocalDate.of(2024, 1, 8),
            LocalDate.of(2024, 1, 29),  // skipped two weeks
            LocalDate.of(2024, 2, 19)   // skipped two weeks
        ))).isEqualTo(1);
    }

    @Test
    void duplicateWeeks_deduplicatedAndCountedOnce() {
        assertThat(StreakCalculator.longestConsecutiveWeeks(List.of(
            LocalDate.of(2024, 1, 8),
            LocalDate.of(2024, 1, 8),  // duplicate
            LocalDate.of(2024, 1, 15)
        ))).isEqualTo(2);
    }

    @Test
    void unorderedInput_sortedCorrectly() {
        assertThat(StreakCalculator.longestConsecutiveWeeks(List.of(
            LocalDate.of(2024, 1, 22),
            LocalDate.of(2024, 1, 8),
            LocalDate.of(2024, 1, 15)
        ))).isEqualTo(3);
    }

    @Test
    void longerSecondStreak_returnsThat() {
        // first streak: 2, second streak: 4
        assertThat(StreakCalculator.longestConsecutiveWeeks(List.of(
            LocalDate.of(2024, 1, 8),
            LocalDate.of(2024, 1, 15),
            LocalDate.of(2024, 2, 12),  // new streak starts here
            LocalDate.of(2024, 2, 19),
            LocalDate.of(2024, 2, 26),
            LocalDate.of(2024, 3, 4)
        ))).isEqualTo(4);
    }
}
