package com.clinic.dao;

import com.clinic.db.Database;
import com.clinic.model.User;
import com.clinic.service.Passwords;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class UserDao {

    private UserDao() {
    }

    private static final String COLS = "id, username, role, full_name, phone, email";

    private static User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"), rs.getString("username"), rs.getString("role"),
                rs.getString("full_name"), rs.getString("phone"), rs.getString("email"));
    }

    private static String blankToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    public static int create(String username, String password, String role, String fullName,
            String phone, String email) throws SQLException {
        String sql = "INSERT INTO users(username, password_hash, role, full_name, phone, email) VALUES(?,?,?,?,?,?)";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username.trim());
            ps.setString(2, Passwords.hash(password));
            ps.setString(3, role);
            ps.setString(4, fullName.trim());
            ps.setString(5, blankToNull(phone));
            ps.setString(6, blankToNull(email));
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return k.getInt(1);
            }
        }
    }

    /**
     * Returns the user if credentials are valid, otherwise null.
     */
    public static User authenticate(String username, String password) throws SQLException {
        String sql = "SELECT " + COLS + ", password_hash FROM users WHERE username=? AND active=1";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && Passwords.verify(password, rs.getString("password_hash"))) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    public static boolean usernameExists(String username) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE username=?")) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static int countByRole(String role) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM users WHERE role=?")) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public static List<User> listByRole(String role) throws SQLException {
        List<User> out = new ArrayList<>();
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT " + COLS + " FROM users WHERE role=? AND active=1 ORDER BY full_name")) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        }
        return out;
    }

    /**
     * Updates the password hash for the given user id.
     */
    public static void changePassword(int userId, String newPassword) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET password_hash=? WHERE id=?")) {
            ps.setString(1, Passwords.hash(newPassword));
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Verifies the given plaintext against the stored hash for the user.
     */
    public static boolean verifyPassword(int userId, String password) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT password_hash FROM users WHERE id=?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && Passwords.verify(password, rs.getString("password_hash"));
            }
        }
    }

    public static void updateProfile(int userId, String username, String fullName,
            String phone, String email) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET username=?, full_name=?, phone=?, email=? WHERE id=?")) {
            ps.setString(1, username.trim());
            ps.setString(2, fullName.trim());
            ps.setString(3, blankToNull(phone));
            ps.setString(4, blankToNull(email));
            ps.setInt(5, userId);
            ps.executeUpdate();
        }
    }

    public static User findById(int id) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT " + COLS + " FROM users WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }
}
