package com.clinic.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record Appointment(
        Integer id,
        int doctorId,
        String doctorName,
        int patientId,
        String patientName,
        LocalDate date,
        LocalTime start,
        LocalTime end,
        String status,
        String notes
) {
    public static Appointment newBooking(
            int doctorId, String doctorName,
            int patientId, String patientName,
            LocalDate date, LocalTime start, LocalTime end,
            String notes) {
        return new Appointment(null, doctorId, doctorName, patientId, patientName,
                date, start, end, "PENDING", notes);
    }

    public boolean isPending()   { return "PENDING".equals(status); }
    public boolean isApproved()  { return "APPROVED".equals(status); }
    public boolean isCancelled() { return "CANCELLED".equals(status); }
    public boolean isCompleted() { return "COMPLETED".equals(status); }
}