package com.clinic.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Doctor(
        Integer id,
        int userId,
        String fullName,
        String phone,
        String email,
        String specialization,
        String licenseNumber,
        LocalDate licenseExpiry,
        String deaNumber,
        String npiNumber,
        String boardCertifications,
        String medicalSchool,
        Integer graduationYear,
        Integer yearsOfExperience,
        String clinicName,
        String clinicAddress,
        String clinicPhone,
        String address,
        String city,
        String postalCode,
        String country,
        String languages,
        boolean acceptingNewPatients,
        int slotMinutes,
        String bio,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static Doctor of(int userId, String fullName, String specialization, int slotMinutes) {
        return new Doctor(
                null, userId, fullName, null, null, specialization,
                null, null, null, null, null, null, null, null,
                null, null, null,
                null, null, null, null,
                null, true, slotMinutes, null,
                null, null);
    }

    @Override
    public String toString() {
        return "Dr. " + fullName + " - " + specialization;
    }
}