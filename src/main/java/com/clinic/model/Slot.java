package com.clinic.model;

import java.time.Duration;
import java.time.LocalTime;

public record Slot(LocalTime start, LocalTime end) {

    public Slot {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Slot start/end must not be null");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException(
                    "Slot start (" + start + ") must be before end (" + end + ")");
        }
    }

    public long durationMinutes() {
        return Duration.between(start, end).toMinutes();
    }

    public boolean overlaps(Slot other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    @Override
    public String toString() { return start + " - " + end; }
}