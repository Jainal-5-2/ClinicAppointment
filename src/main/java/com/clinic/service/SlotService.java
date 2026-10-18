package com.clinic.service;

import com.clinic.dao.AppointmentDao;
import com.clinic.dao.ScheduleDao;
import com.clinic.model.Doctor;
import com.clinic.model.ScheduleRule;
import com.clinic.model.Slot;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Builds real-time availability from the doctor's weekly hours, days off and existing bookings. */
public final class SlotService {
    private SlotService() { }

    public static List<Slot> available(Doctor doctor, LocalDate date) throws SQLException {
        List<Slot> out = new ArrayList<>();
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) return out;
        if (ScheduleDao.isDayOff(doctor.id(), date)) return out;

        Set<LocalTime> taken = AppointmentDao.takenStarts(doctor.id(), date);
        LocalTime now = LocalTime.now();
        int minutes = doctor.slotMinutes();

        for (ScheduleRule r : ScheduleDao.rulesForDay(doctor.id(), date.getDayOfWeek().getValue())) {
            LocalTime t = r.start();
            while (true) {
                LocalTime e = t.plusMinutes(minutes);
                if (!e.isAfter(t) || e.isAfter(r.end())) break; // stop at end of hours / midnight wrap
                boolean past = date.equals(today) && !t.isAfter(now);
                if (!past && !taken.contains(t)) out.add(new Slot(t, e));
                t = e;
            }
        }
        return out;
    }

    public static String describeHours(Doctor doctor) throws SQLException {
        List<ScheduleRule> rules = ScheduleDao.rulesFor(doctor.id());
        if (rules.isEmpty()) return "This doctor has not set working hours yet.";
        StringBuilder sb = new StringBuilder("Working hours: ");
        for (int i = 0; i < rules.size(); i++) {
            ScheduleRule r = rules.get(i);
            if (i > 0) sb.append(", ");
            sb.append(r.shortDay()).append(' ').append(r.start()).append('-').append(r.end());
        }
        return sb.toString();
    }

    /** Returns null on success, otherwise a user-friendly error message. */
    public static String book(Doctor doctor, int patientId, LocalDate date, Slot slot, String notes)
            throws SQLException {
        if (!available(doctor, date).contains(slot)) {
            return "That time slot is no longer available. Please pick another one.";
        }
        if (AppointmentDao.patientHasOverlap(patientId, date, slot.start(), slot.end())) {
            return "This patient already has an appointment on that date (limit: one per day).";
        }
        if (!AppointmentDao.book(doctor.id(), patientId, date, slot.start(), slot.end(), notes)) {
            return "Someone just booked that slot. Please pick another one.";
        }
        return null;
    }
}
