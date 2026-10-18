package com.clinic.dao;

import com.clinic.db.Database;
import com.clinic.model.DayOff;
import com.clinic.model.ScheduleRule;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class ScheduleDao {

    private ScheduleDao() {
    }

    private static ScheduleRule mapRule(ResultSet rs) throws SQLException {
        return new ScheduleRule(rs.getInt("id"), rs.getInt("doctor_id"), rs.getInt("day_of_week"),
                rs.getObject("start_time", LocalTime.class), rs.getObject("end_time", LocalTime.class));
    }

    public static List<ScheduleRule> rulesFor(int doctorId) throws SQLException {
        List<ScheduleRule> out = new ArrayList<>();
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM schedule_rules WHERE doctor_id=? ORDER BY day_of_week, start_time")) {
            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapRule(rs));
                }
            }
        }
        return out;
    }

    public static List<ScheduleRule> rulesForDay(int doctorId, int dayOfWeek) throws SQLException {
        List<ScheduleRule> out = new ArrayList<>();
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM schedule_rules WHERE doctor_id=? AND day_of_week=? ORDER BY start_time")) {
            ps.setInt(1, doctorId);
            ps.setInt(2, dayOfWeek);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapRule(rs));
                }
            }
        }
        return out;
    }

    public static void addRule(int doctorId, int dayOfWeek, LocalTime start, LocalTime end) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO schedule_rules(doctor_id, day_of_week, start_time, end_time) VALUES(?,?,?,?)")) {
            ps.setInt(1, doctorId);
            ps.setInt(2, dayOfWeek);
            ps.setObject(3, start);
            ps.setObject(4, end);
            ps.executeUpdate();
        }
    }

    public static void deleteRule(int ruleId) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM schedule_rules WHERE id=?")) {
            ps.setInt(1, ruleId);
            ps.executeUpdate();
        }
    }

    public static List<DayOff> daysOffFor(int doctorId) throws SQLException {
        List<DayOff> out = new ArrayList<>();
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM schedule_exceptions WHERE doctor_id=? AND off_date >= CURDATE() ORDER BY off_date")) {
            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new DayOff(rs.getInt("id"), rs.getInt("doctor_id"),
                            rs.getObject("off_date", LocalDate.class), rs.getString("reason")));
                }
            }
        }
        return out;
    }

    public static boolean isDayOff(int doctorId, LocalDate date) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM schedule_exceptions WHERE doctor_id=? AND off_date=?")) {
            ps.setInt(1, doctorId);
            ps.setObject(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Returns false if that date is already marked as a day off.
     */
    public static boolean addDayOff(int doctorId, LocalDate date, String reason) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO schedule_exceptions(doctor_id, off_date, reason) VALUES(?,?,?)")) {
            ps.setInt(1, doctorId);
            ps.setObject(2, date);
            ps.setString(3, reason == null || reason.trim().isEmpty() ? null : reason.trim());
            ps.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException dup) {
            return false;
        }
    }

    public static void deleteDayOff(int id) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM schedule_exceptions WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * True if the doctor has at least one working rule for the given day of
     * week (1=Mon..7=Sun).
     */
    public static boolean isWorkingDay(int doctorId, int dayOfWeek) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM schedule_rules WHERE doctor_id=? AND day_of_week=? LIMIT 1")) {
            ps.setInt(1, doctorId);
            ps.setInt(2, dayOfWeek);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
