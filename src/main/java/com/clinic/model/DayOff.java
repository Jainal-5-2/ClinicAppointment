package com.clinic.model;

import java.time.LocalDate;

public record DayOff(
        Integer id,
        int doctorId,
        LocalDate date,
        String reason
) {
    public static DayOff of(int doctorId, LocalDate date, String reason) {
        return new DayOff(null, doctorId, date, reason);
    }
}