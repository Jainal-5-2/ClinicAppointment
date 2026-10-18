package com.clinic.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;

public record ScheduleRule(
        Integer id,
        int doctorId,
        int dayOfWeek,
        LocalTime start,
        LocalTime end
) {
    public static ScheduleRule of(int doctorId, int dayOfWeek, LocalTime start, LocalTime end) {
        return new ScheduleRule(null, doctorId, dayOfWeek, start, end);
    }

    public DayOfWeek day() { return DayOfWeek.of(dayOfWeek); }

    public String dayName() { return day().getDisplayName(TextStyle.FULL, Locale.ENGLISH); }

    public String shortDay() { return day().getDisplayName(TextStyle.SHORT, Locale.ENGLISH); }

    @Override
    public String toString() { return dayName() + " " + start + "–" + end; }
}