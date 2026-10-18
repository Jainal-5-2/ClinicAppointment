package com.clinic.dao;

import com.clinic.db.Database;
import com.clinic.model.Appointment;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AppointmentDao {
    private AppointmentDao() { }

    private static final String SELECT =
            "SELECT a.id, a.doctor_id, du.full_name AS doctor_name, a.patient_id, pu.full_name AS patient_name, " +
            "a.appt_date, a.start_time, a.end_time, a.status, a.notes " +
            "FROM appointments a " +
            "JOIN doctors d ON d.id = a.doctor_id " +
            "JOIN users du ON du.id = d.user_id " +
            "JOIN users pu ON pu.id = a.patient_id ";

    /**
     * Status values that occupy a slot. Mirrors the {@code active_slot}
     * generated column in the schema.
     */
    private static final String ACTIVE_STATUSES = "('PENDING','APPROVED')";

    private static List<Appointment> query(String where, Object... params) throws SQLException {
        List<Appointment> out = new ArrayList<>();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     SELECT + where + " ORDER BY a.appt_date, a.start_time")) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Appointment(
                            rs.getInt("id"),
                            rs.getInt("doctor_id"),
                            rs.getString("doctor_name"),
                            rs.getInt("patient_id"),
                            rs.getString("patient_name"),
                            rs.getObject("appt_date", LocalDate.class),
                            rs.getObject("start_time", LocalTime.class),
                            rs.getObject("end_time", LocalTime.class),
                            rs.getString("status"),
                            rs.getString("notes")));
                }
            }
        }
        return out;
    }

    // ---------------------------------------------------------------------
    // Listings
    // ---------------------------------------------------------------------

    public static List<Appointment> listAll(LocalDate dateOrNull) throws SQLException {
        return dateOrNull == null ? query("WHERE 1=1") : query("WHERE a.appt_date = ?", dateOrNull);
    }

    public static List<Appointment> listForDoctor(int doctorId, LocalDate dateOrNull) throws SQLException {
        return dateOrNull == null
                ? query("WHERE a.doctor_id = ?", doctorId)
                : query("WHERE a.doctor_id = ? AND a.appt_date = ?", doctorId, dateOrNull);
    }

    public static List<Appointment> listForPatient(int patientId) throws SQLException {
        return query("WHERE a.patient_id = ?", patientId);
    }

    /** Pending requests for a doctor — the doctor's "approval queue". */
    public static List<Appointment> listPendingForDoctor(int doctorId) throws SQLException {
        return query("WHERE a.doctor_id = ? AND a.status = 'PENDING'", doctorId);
    }

    // ---------------------------------------------------------------------
    // Availability checks
    // ---------------------------------------------------------------------

    /** Start times already taken (pending or approved) for a doctor on a date. */
    public static Set<LocalTime> takenStarts(int doctorId, LocalDate date) throws SQLException {
        Set<LocalTime> out = new HashSet<>();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT start_time FROM appointments " +
                     "WHERE doctor_id=? AND appt_date=? AND status IN " + ACTIVE_STATUSES)) {
            ps.setInt(1, doctorId);
            ps.setObject(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getObject(1, LocalTime.class));
            }
        }
        return out;
    }

    /** Number of appointments occupying a slot on a date (pending + approved). */
    public static int countActive(int doctorId, LocalDate date) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM appointments " +
                     "WHERE doctor_id=? AND appt_date=? AND status IN " + ACTIVE_STATUSES)) {
            ps.setInt(1, doctorId);
            ps.setObject(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** One active appointment per patient per day (pending or approved). */
    public static boolean patientHasOverlap(int patientId, LocalDate date,
                                            LocalTime start, LocalTime end) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM appointments " +
                     "WHERE patient_id=? AND appt_date=? AND status IN " + ACTIVE_STATUSES)) {
            ps.setInt(1, patientId);
            ps.setObject(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ---------------------------------------------------------------------
    // Writes
    // ---------------------------------------------------------------------

    /**
     * Creates a new booking in PENDING state (DB default).
     * Returns false if the slot was taken by someone else (unique-key violation).
     */
    public static boolean book(int doctorId, int patientId, LocalDate date,
                               LocalTime start, LocalTime end, String notes) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO appointments(doctor_id, patient_id, appt_date, start_time, end_time, notes) " +
                     "VALUES(?,?,?,?,?,?)")) {
            ps.setInt(1, doctorId);
            ps.setInt(2, patientId);
            ps.setObject(3, date);
            ps.setObject(4, start);
            ps.setObject(5, end);
            ps.setString(6, notes == null || notes.trim().isEmpty() ? null : notes.trim());
            ps.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException dup) {
            return false;
        }
    }

    /** Generic status update. Caller is responsible for valid transitions. */
    public static void setStatus(int id, String status) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE appointments SET status=? WHERE id=?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------------
    // Status transition helpers (enforce the flow)
    // ---------------------------------------------------------------------

    public static boolean approve(int id) throws SQLException {
        return transition(id, "PENDING", "APPROVED");
    }

    public static boolean cancel(int id) throws SQLException {
        return transition(id, "PENDING", "CANCELLED")
                || transition(id, "APPROVED", "CANCELLED");
    }

    public static boolean complete(int id) throws SQLException {
        return transition(id, "APPROVED", "COMPLETED");
    }

    /**
     * Updates status only if the current status matches {@code from}.
     * Returns true if exactly one row was updated.
     */
    private static boolean transition(int id, String from, String to) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE appointments SET status=? WHERE id=? AND status=?")) {
            ps.setString(1, to);
            ps.setInt(2, id);
            ps.setString(3, from);
            return ps.executeUpdate() == 1;
        }
    }
}