package com.jobportal.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** TODO(#1) */
class TimeSlotTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 10);

    private static TimeSlot slot(String from, String to) {
        return new TimeSlot(DAY.atTime(LocalTime.parse(from)), DAY.atTime(LocalTime.parse(to)));
    }

    @ParameterizedTest(name = "{0}-{1} vs {2}-{3} overlaps = {4}")
    @CsvSource({
            "10:00, 11:00, 10:30, 11:30, true",   // partial overlap at the end
            "10:30, 11:30, 10:00, 11:00, true",   // partial overlap at the start
            "10:00, 12:00, 10:30, 11:00, true",   // other is fully inside
            "10:30, 11:00, 10:00, 12:00, true",   // this is fully inside
            "10:00, 11:00, 10:00, 11:00, true",   // identical
            "10:00, 11:00, 11:00, 12:00, false",  // back-to-back: touching is not overlapping
            "11:00, 12:00, 10:00, 11:00, false",  // back-to-back, other way round
            "10:00, 11:00, 13:00, 14:00, false",  // completely separate
    })
    void overlaps(String aStart, String aEnd, String bStart, String bEnd, boolean expected) {
        assertEquals(expected, slot(aStart, aEnd).overlaps(slot(bStart, bEnd)));
    }

    @Test
    @DisplayName("overlap is symmetric")
    void overlapIsSymmetric() {
        TimeSlot a = slot("09:00", "10:15");
        TimeSlot b = slot("10:00", "11:00");
        assertEquals(a.overlaps(b), b.overlaps(a));
    }

    @Test
    void endMustBeAfterStart() {
        assertThrows(IllegalArgumentException.class, () -> slot("11:00", "10:00"));
        assertThrows(IllegalArgumentException.class, () -> slot("11:00", "11:00"));
    }
}
