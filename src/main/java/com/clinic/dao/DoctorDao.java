package com.clinic.dao;

import com.clinic.db.Database;
import com.clinic.model.Doctor;
import com.clinic.service.Passwords;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class DoctorDao {
    private DoctorDao() { }

    private static final String BASE =
            "SELECT d.id, d.user_id, u.full_name, u.phone, u.email, " +
            "d.specialization, d.license_number, d.license_expiry, " +
            "d.dea_number, d.npi_number, d.board_certifications, d.medical_school, d.graduation_year, " +
            "d.years_of_experience, d.clinic_name, d.clinic_address, d.clinic_phone, " +
            "d.address, d.city, d.postal_code, d.country, " +
            "d.languages, d.accepting_new_patients, d.slot_minutes, d.bio, d.created_at, d.updated_at " +
            "FROM doctors d JOIN users u ON u.id = d.user_id WHERE u.active = 1";

    private static Doctor map(ResultSet rs) throws SQLException {
        return new Doctor(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("specialization"),
                rs.getString("license_number"),
                rs.getObject("license_expiry", LocalDate.class),
                rs.getString("dea_number"),
                rs.getString("npi_number"),
                rs.getString("board_certifications"),
                rs.getString("medical_school"),
                (Integer) rs.getObject("graduation_year"),
                (Integer) rs.getObject("years_of_experience"),
                rs.getString("clinic_name"),
                rs.getString("clinic_address"),
                rs.getString("clinic_phone"),
                rs.getString("address"),
                rs.getString("city"),
                rs.getString("postal_code"),
                rs.getString("country"),
                rs.getString("languages"),
                rs.getBoolean("accepting_new_patients"),
                rs.getInt("slot_minutes"),
                rs.getString("bio"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class));
    }

    public static List<Doctor> listAll() throws SQLException {
        List<Doctor> out = new ArrayList<>();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(BASE + " ORDER BY u.full_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    public static Doctor findById(int id) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(BASE + " AND d.id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public static Doctor findByUserId(int userId) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(BASE + " AND d.user_id=?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public static void updateSlotMinutes(int doctorId, int minutes) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("UPDATE doctors SET slot_minutes=? WHERE id=?")) {
            ps.setInt(1, minutes);
            ps.setInt(2, doctorId);
            ps.executeUpdate();
        }
    }

    public static void createWithUser(String username, String password, String fullName, String phone,
                                      String email, String specialization, String licenseNumber,
                                      int slotMinutes) throws SQLException {
        createWithUser(
                username, password, fullName, phone, email,
                specialization, licenseNumber,
                null, null, null,
                null, null, null, null,
                null, null, null,
                null, null, null, null,
                null, true, slotMinutes, null);
    }

    public static void createWithUser(String username, String password, String fullName, String phone,
                                      String email, String specialization, String licenseNumber,
                                      LocalDate licenseExpiry, String deaNumber, String npiNumber,
                                      String boardCertifications, String medicalSchool, Integer graduationYear,
                                      Integer yearsOfExperience, String clinicName, String clinicAddress,
                                      String clinicPhone, String address, String city, String postalCode,
                                      String country, String languages, boolean acceptingNewPatients,
                                      int slotMinutes, String bio) throws SQLException {
        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try {
                int userId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO users(username, password_hash, role, full_name, phone, email) " +
                        "VALUES(?,?,'DOCTOR',?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, username.trim());
                    ps.setString(2, Passwords.hash(password));
                    ps.setString(3, fullName.trim());
                    ps.setString(4, blankToNull(phone));
                    ps.setString(5, blankToNull(email));
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) {
                        k.next();
                        userId = k.getInt(1);
                    }
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO doctors(user_id, specialization, license_number, license_expiry, " +
                        "dea_number, npi_number, board_certifications, medical_school, graduation_year, " +
                        "years_of_experience, clinic_name, clinic_address, clinic_phone, " +
                        "address, city, postal_code, country, " +
                        "languages, accepting_new_patients, slot_minutes, bio) " +
                        "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                    ps.setInt(1, userId);
                    ps.setString(2, specialization.trim());
                    ps.setString(3, licenseNumber.trim());
                    ps.setObject(4, licenseExpiry);
                    ps.setString(5, blankToNull(deaNumber));
                    ps.setString(6, blankToNull(npiNumber));
                    ps.setString(7, blankToNull(boardCertifications));
                    ps.setString(8, blankToNull(medicalSchool));
                    setNullableInt(ps, 9, graduationYear);
                    setNullableInt(ps, 10, yearsOfExperience);
                    ps.setString(11, blankToNull(clinicName));
                    ps.setString(12, blankToNull(clinicAddress));
                    ps.setString(13, blankToNull(clinicPhone));
                    ps.setString(14, blankToNull(address));
                    ps.setString(15, blankToNull(city));
                    ps.setString(16, blankToNull(postalCode));
                    ps.setString(17, blankToNull(country));
                    ps.setString(18, blankToNull(languages));
                    ps.setBoolean(19, acceptingNewPatients);
                    ps.setInt(20, slotMinutes);
                    ps.setString(21, blankToNull(bio));
                    ps.executeUpdate();
                }

                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public static void updateProfile(Doctor d) throws SQLException {
        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE users SET phone=?, email=? WHERE id=?")) {
                    ps.setString(1, blankToNull(d.phone()));
                    ps.setString(2, blankToNull(d.email()));
                    ps.setInt(3, d.userId());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE doctors SET specialization=?, license_number=?, license_expiry=?, " +
                        "dea_number=?, npi_number=?, board_certifications=?, medical_school=?, graduation_year=?, " +
                        "years_of_experience=?, clinic_name=?, clinic_address=?, clinic_phone=?, " +
                        "address=?, city=?, postal_code=?, country=?, " +
                        "languages=?, accepting_new_patients=?, slot_minutes=?, bio=? WHERE id=?")) {
                    ps.setString(1, d.specialization());
                    ps.setString(2, d.licenseNumber());
                    ps.setObject(3, d.licenseExpiry());
                    ps.setString(4, blankToNull(d.deaNumber()));
                    ps.setString(5, blankToNull(d.npiNumber()));
                    ps.setString(6, blankToNull(d.boardCertifications()));
                    ps.setString(7, blankToNull(d.medicalSchool()));
                    setNullableInt(ps, 8, d.graduationYear());
                    setNullableInt(ps, 9, d.yearsOfExperience());
                    ps.setString(10, blankToNull(d.clinicName()));
                    ps.setString(11, blankToNull(d.clinicAddress()));
                    ps.setString(12, blankToNull(d.clinicPhone()));
                    ps.setString(13, blankToNull(d.address()));
                    ps.setString(14, blankToNull(d.city()));
                    ps.setString(15, blankToNull(d.postalCode()));
                    ps.setString(16, blankToNull(d.country()));
                    ps.setString(17, blankToNull(d.languages()));
                    ps.setBoolean(18, d.acceptingNewPatients());
                    ps.setInt(19, d.slotMinutes());
                    ps.setString(20, blankToNull(d.bio()));
                    ps.setInt(21, d.id());
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) ps.setNull(index, Types.INTEGER);
        else ps.setInt(index, value);
    }
}