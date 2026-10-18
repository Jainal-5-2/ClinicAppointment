package com.clinic.dao;

import com.clinic.db.Database;
import com.clinic.model.Patient;
import com.clinic.service.Passwords;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class PatientDao {
    private PatientDao() { }

    private static final String BASE =
            "SELECT p.id, p.user_id, u.full_name, u.phone, u.email, p.medical_record_number, p.date_of_birth, " +
            "p.gender, p.blood_group, p.address, p.city, p.postal_code, p.country, " +
            "p.emergency_contact_name, p.emergency_contact_phone, p.emergency_contact_relation, " +
            "p.allergies, p.chronic_conditions, p.current_medications, " +
            "p.insurance_provider, p.insurance_policy_number, p.insurance_expiry, " +
            "p.created_at, p.updated_at " +
            "FROM patients p JOIN users u ON u.id = p.user_id WHERE u.active = 1";

    private static Patient map(ResultSet rs) throws SQLException {
        return new Patient(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("medical_record_number"),
                rs.getObject("date_of_birth", LocalDate.class),
                rs.getString("gender"),
                rs.getString("blood_group"),
                rs.getString("address"),
                rs.getString("city"),
                rs.getString("postal_code"),
                rs.getString("country"),
                rs.getString("emergency_contact_name"),
                rs.getString("emergency_contact_phone"),
                rs.getString("emergency_contact_relation"),
                rs.getString("allergies"),
                rs.getString("chronic_conditions"),
                rs.getString("current_medications"),
                rs.getString("insurance_provider"),
                rs.getString("insurance_policy_number"),
                rs.getObject("insurance_expiry", LocalDate.class),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class));
    }

    public static List<Patient> listAll() throws SQLException {
        List<Patient> out = new ArrayList<>();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(BASE + " ORDER BY u.full_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    public static Patient findById(int id) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(BASE + " AND p.id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public static Patient findByUserId(int userId) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(BASE + " AND p.user_id=?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public static void createWithUser(String username, String password, String fullName, String phone,
                                      String email, String medicalRecordNumber, LocalDate dateOfBirth,
                                      String gender, String bloodGroup) throws SQLException {
        createWithUser(
                username, password, fullName, phone, email,
                medicalRecordNumber, dateOfBirth, gender, bloodGroup,
                null, null, null, null,
                null, null, null,
                null, null, null,
                null, null, null);
    }

    public static void createWithUser(String username, String password, String fullName, String phone,
                                      String email, String medicalRecordNumber, LocalDate dateOfBirth,
                                      String gender, String bloodGroup, String address, String city,
                                      String postalCode, String country, String emergencyContactName,
                                      String emergencyContactPhone, String emergencyContactRelation,
                                      String allergies, String chronicConditions, String currentMedications,
                                      String insuranceProvider, String insurancePolicyNumber,
                                      LocalDate insuranceExpiry) throws SQLException {
        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try {
                int userId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO users(username, password_hash, role, full_name, phone, email) " +
                        "VALUES(?,?,'PATIENT',?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
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
                        "INSERT INTO patients(user_id, medical_record_number, date_of_birth, gender, " +
                        "blood_group, address, city, postal_code, country, emergency_contact_name, " +
                        "emergency_contact_phone, emergency_contact_relation, allergies, chronic_conditions, " +
                        "current_medications, insurance_provider, insurance_policy_number, insurance_expiry) " +
                        "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                    ps.setInt(1, userId);
                    ps.setString(2, blankToNull(medicalRecordNumber));
                    ps.setObject(3, dateOfBirth);
                    ps.setString(4, gender == null ? "UNKNOWN" : gender);
                    ps.setString(5, bloodGroup == null ? "UNKNOWN" : bloodGroup);
                    ps.setString(6, blankToNull(address));
                    ps.setString(7, blankToNull(city));
                    ps.setString(8, blankToNull(postalCode));
                    ps.setString(9, blankToNull(country));
                    ps.setString(10, blankToNull(emergencyContactName));
                    ps.setString(11, blankToNull(emergencyContactPhone));
                    ps.setString(12, blankToNull(emergencyContactRelation));
                    ps.setString(13, blankToNull(allergies));
                    ps.setString(14, blankToNull(chronicConditions));
                    ps.setString(15, blankToNull(currentMedications));
                    ps.setString(16, blankToNull(insuranceProvider));
                    ps.setString(17, blankToNull(insurancePolicyNumber));
                    ps.setObject(18, insuranceExpiry);
                    ps.executeUpdate();
                }

                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public static void updateProfile(Patient p) throws SQLException {
        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE users SET phone=?, email=? WHERE id=?")) {
                    ps.setString(1, blankToNull(p.phone()));
                    ps.setString(2, blankToNull(p.email()));
                    ps.setInt(3, p.userId());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE patients SET medical_record_number=?, date_of_birth=?, gender=?, blood_group=?, " +
                        "address=?, city=?, postal_code=?, country=?, emergency_contact_name=?, " +
                        "emergency_contact_phone=?, emergency_contact_relation=?, allergies=?, " +
                        "chronic_conditions=?, current_medications=?, insurance_provider=?, " +
                        "insurance_policy_number=?, insurance_expiry=? WHERE id=?")) {
                    ps.setString(1, blankToNull(p.medicalRecordNumber()));
                    ps.setObject(2, p.dateOfBirth());
                    ps.setString(3, p.gender() == null ? "UNKNOWN" : p.gender());
                    ps.setString(4, p.bloodGroup() == null ? "UNKNOWN" : p.bloodGroup());
                    ps.setString(5, blankToNull(p.address()));
                    ps.setString(6, blankToNull(p.city()));
                    ps.setString(7, blankToNull(p.postalCode()));
                    ps.setString(8, blankToNull(p.country()));
                    ps.setString(9, blankToNull(p.emergencyContactName()));
                    ps.setString(10, blankToNull(p.emergencyContactPhone()));
                    ps.setString(11, blankToNull(p.emergencyContactRelation()));
                    ps.setString(12, blankToNull(p.allergies()));
                    ps.setString(13, blankToNull(p.chronicConditions()));
                    ps.setString(14, blankToNull(p.currentMedications()));
                    ps.setString(15, blankToNull(p.insuranceProvider()));
                    ps.setString(16, blankToNull(p.insurancePolicyNumber()));
                    ps.setObject(17, p.insuranceExpiry());
                    ps.setInt(18, p.id());
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
}