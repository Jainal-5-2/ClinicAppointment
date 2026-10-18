package com.clinic.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Patient(
        Integer id,
        int userId,
        String fullName,
        String phone,
        String email,
        String medicalRecordNumber,
        LocalDate dateOfBirth,
        String gender,
        String bloodGroup,
        String address,
        String city,
        String postalCode,
        String country,
        String emergencyContactName,
        String emergencyContactPhone,
        String emergencyContactRelation,
        String allergies,
        String chronicConditions,
        String currentMedications,
        String insuranceProvider,
        String insurancePolicyNumber,
        LocalDate insuranceExpiry,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static Patient of(int userId, String fullName) {
        return new Patient(
                null, userId, fullName, null, null,
                null, null, "UNKNOWN", "UNKNOWN",
                null, null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null);
    }

    @Override
    public String toString() { return fullName; }
}