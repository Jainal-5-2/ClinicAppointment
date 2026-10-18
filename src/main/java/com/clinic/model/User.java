package com.clinic.model;

public record User(
        Integer id,
        String username,
        String role,
        String fullName,
        String phone,
        String email
) {
    public static User of(String username, String role, String fullName,
                          String phone, String email) {
        return new User(null, username, role, fullName, phone, email);
    }

    public boolean isAdmin()   { return "ADMIN".equals(role); }
    public boolean isDoctor()  { return "DOCTOR".equals(role); }
    public boolean isPatient() { return "PATIENT".equals(role); }

    @Override
    public String toString() { return fullName; }
}