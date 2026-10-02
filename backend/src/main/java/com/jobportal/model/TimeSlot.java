package com.jobportal.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A half-open time interval [start, end). An interview from 10:00 to 11:00 and another
 * from 11:00 to 12:00 touch but do not overlap.
 */
public record TimeSlot(LocalDateTime start, LocalDateTime end) {

    public TimeSlot {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("end must be after start: " + start + " - " + end);
        }
    }

    public static TimeSlot of(LocalDateTime start, Duration duration) {
        return new TimeSlot(start, start.plus(duration));
    }

    public Duration duration() {
        return Duration.between(start, end);
    }

    /**
     * Returns true if this slot and {@code other} share any moment in time.
     *
     * TODO(#1): implement interval overlap.
     *   Two half-open intervals overlap when:  thisStart < otherEnd  AND  thisEnd > otherStart
     *   Before coding, draw these cases on paper and decide the expected answer:
     *     10-11 vs 10:30-11:30, 10-11 vs 11-12, 10-12 vs 10:30-11, 10-11 vs 9-10, identical slots.
     *   Then check yourself against TimeSlotTest.
     */
    public boolean overlaps(TimeSlot other) {
        throw new UnsupportedOperationException("TODO(#1): TimeSlot.overlaps");
    }
}
