package com.clinic.ui;

import com.clinic.dao.DoctorDao;
import com.clinic.dao.PatientDao;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/** Read / edit profile for a signed-in doctor or patient. Admin can also open any patient or doctor by id. */
public class ProfilePanel extends JPanel implements Refreshable {
    public enum Mode { DOCTOR, PATIENT }

    private final Mode mode;
    private final int userId;
    private final int explicitId;
    private final boolean byId;
    private Doctor doctor;
    private Patient patient;

    private final JTextField fullName = new JTextField();
    private final JTextField phone = new JTextField();
    private final JTextField email = new JTextField();

    private final JTextField address = new JTextField();
    private final JTextField city = new JTextField();
    private final JTextField postal = new JTextField();
    private final JTextField country = new JTextField();

    private final JTextField specialization = new JTextField();
    private final JTextField license = new JTextField();
    private final DateField licenseExpiry = new DateField();
    private final JTextField dea = new JTextField();
    private final JTextField npi = new JTextField();
    private final JTextField boardCertifications = new JTextField();
    private final JTextField medicalSchool = new JTextField();
    private final JTextField graduationYear = new JTextField();
    private final JTextField yearsOfExperience = new JTextField();
    private final JTextField languages = new JTextField();
    private final JCheckBox acceptingNewPatients = new JCheckBox("Accepting new patients");
    private final JTextField bio = new JTextField();

    private final JTextField mrn = new JTextField();
    private final DateField dob = new DateField();
    private final JComboBox<String> gender =
            new JComboBox<>(new String[]{"MALE", "FEMALE", "OTHER", "UNKNOWN"});
    private final JComboBox<String> bloodGroup =
            new JComboBox<>(new String[]{"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "UNKNOWN"});
    private final JTextField emergencyName = new JTextField();
    private final JTextField emergencyPhone = new JTextField();
    private final JTextField emergencyRelation = new JTextField();
    private final JTextField allergies = new JTextField();
    private final JTextField chronic = new JTextField();
    private final JTextField medications = new JTextField();
    private final JTextField insuranceProvider = new JTextField();
    private final JTextField insurancePolicy = new JTextField();
    private final DateField insuranceExpiry = new DateField();

    public ProfilePanel(Mode mode, int userId) {
        this(mode, userId, -1, false);
    }

    public static ProfilePanel openPatient(int patientId) {
        return new ProfilePanel(Mode.PATIENT, -1, patientId, true);
    }

    public static ProfilePanel openDoctor(int doctorId) {
        return new ProfilePanel(Mode.DOCTOR, -1, doctorId, true);
    }

    private ProfilePanel(Mode mode, int userId, int explicitId, boolean byId) {
        this.mode = mode;
        this.userId = userId;
        this.explicitId = explicitId;
        this.byId = byId;
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel form = new JPanel(new GridBagLayout());
        int row = 0;

        JLabel heading = new JLabel(mode == Mode.DOCTOR ? "Doctor profile" : "Patient profile");
        heading.setFont(Theme.font(Font.BOLD, 20f));
        addRow(form, row++, null, heading);

        addRow(form, row++, "Full name", fullName);
        addRow(form, row++, "Phone", phone);
        addRow(form, row++, "Email", email);

        addSection(form, row++, "Address");
        addRow(form, row++, "Street", address);
        addRow(form, row++, "City", city);
        addRow(form, row++, "Postal code", postal);
        addRow(form, row++, "Country", country);

        if (mode == Mode.DOCTOR) {
            addSection(form, row++, "Professional");
            addRow(form, row++, "Specialization", specialization);
            addRow(form, row++, "License number", license);
            addRow(form, row++, "License expiry", licenseExpiry);
            addRow(form, row++, "DEA number", dea);
            addRow(form, row++, "NPI number", npi);
            addRow(form, row++, "Board certifications", boardCertifications);
            addRow(form, row++, "Medical school", medicalSchool);
            addRow(form, row++, "Graduation year", graduationYear);
            addRow(form, row++, "Years of experience", yearsOfExperience);
            addRow(form, row++, "Languages", languages);
            addRow(form, row++, null, acceptingNewPatients);
            addRow(form, row++, "Bio", bio);
        } else {
            addSection(form, row++, "Personal");
            addRow(form, row++, "Medical record number", mrn);
            addRow(form, row++, "Date of birth", dob);
            addRow(form, row++, "Gender", gender);
            addRow(form, row++, "Blood group", bloodGroup);

            addSection(form, row++, "Emergency contact");
            addRow(form, row++, "Name", emergencyName);
            addRow(form, row++, "Phone", emergencyPhone);
            addRow(form, row++, "Relation", emergencyRelation);

            addSection(form, row++, "Clinical");
            addRow(form, row++, "Allergies", allergies);
            addRow(form, row++, "Chronic conditions", chronic);
            addRow(form, row++, "Current medications", medications);

            addSection(form, row++, "Insurance");
            addRow(form, row++, "Provider", insuranceProvider);
            addRow(form, row++, "Policy number", insurancePolicy);
            addRow(form, row++, "Expiry", insuranceExpiry);
        }

        fullName.setEditable(false);
        if (mode == Mode.DOCTOR) {
            license.setEditable(false);
        } else {
            mrn.setEditable(false);
        }

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        FadeButton save = FadeButton.primary("Save changes");
        FadeButton refresh = FadeButton.secondary("Refresh");
        FadeButton changePw = FadeButton.secondary("Change password");
        save.addActionListener(e -> save());
        refresh.addActionListener(e -> reload());
        changePw.addActionListener(e ->
                ChangePasswordDialog.forSelf(
                        javax.swing.SwingUtilities.getWindowAncestor(this),
                        userId));
        changePw.setVisible(!byId);

        JPanel actions = Theme.clear(new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)));
        actions.add(changePw);
        actions.add(refresh);
        actions.add(save);

        add(scroll, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
        reload();
    }

    private static void addSection(JPanel form, int row, String label) {
        JLabel l = new JLabel(label);
        l.setFont(Theme.font(Font.BOLD, 14f));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = row * 2;
        g.gridwidth = 2;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(14, 0, 4, 0);
        form.add(l, g);
    }

    private static void addRow(JPanel form, int row, String label, java.awt.Component c) {
        GridBagConstraints g = new GridBagConstraints();
        if (label != null) {
            g.gridx = 0;
            g.gridy = row * 2;
            g.anchor = GridBagConstraints.WEST;
            g.insets = new Insets(4, 0, 4, 12);
            JLabel l = new JLabel(label);
            l.setForeground(Theme.MUTED);
            form.add(l, g);
            g = new GridBagConstraints();
        } else {
            g.gridwidth = 2;
        }
        g.gridx = 1;
        g.gridy = label == null ? row * 2 - 1 : row * 2;
        if (label == null) {
            g.gridx = 0;
            g.gridy = row * 2;
        }
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 0, 4, 0);
        form.add(c, g);
    }

    @Override public void reload() {
        try {
            if (mode == Mode.DOCTOR) {
                doctor = byId ? DoctorDao.findById(explicitId) : DoctorDao.findByUserId(userId);
                if (doctor == null) return;
                fullName.setText(doctor.fullName());
                phone.setText(nz(doctor.phone()));
                email.setText(nz(doctor.email()));
                address.setText(nz(doctor.address()));
                city.setText(nz(doctor.city()));
                postal.setText(nz(doctor.postalCode()));
                country.setText(nz(doctor.country()));

                specialization.setText(nz(doctor.specialization()));
                license.setText(nz(doctor.licenseNumber()));
                licenseExpiry.setDate(doctor.licenseExpiry());
                dea.setText(nz(doctor.deaNumber()));
                npi.setText(nz(doctor.npiNumber()));
                boardCertifications.setText(nz(doctor.boardCertifications()));
                medicalSchool.setText(nz(doctor.medicalSchool()));
                graduationYear.setText(doctor.graduationYear() == null ? "" : doctor.graduationYear().toString());
                yearsOfExperience.setText(doctor.yearsOfExperience() == null ? "" : doctor.yearsOfExperience().toString());
                languages.setText(nz(doctor.languages()));
                acceptingNewPatients.setSelected(doctor.acceptingNewPatients());
                bio.setText(nz(doctor.bio()));
            } else {
                patient = byId ? PatientDao.findById(explicitId) : PatientDao.findByUserId(userId);
                if (patient == null) return;
                fullName.setText(patient.fullName());
                phone.setText(nz(patient.phone()));
                email.setText(nz(patient.email()));
                address.setText(nz(patient.address()));
                city.setText(nz(patient.city()));
                postal.setText(nz(patient.postalCode()));
                country.setText(nz(patient.country()));

                mrn.setText(nz(patient.medicalRecordNumber()));
                dob.setDate(patient.dateOfBirth());
                gender.setSelectedItem(patient.gender() == null ? "UNKNOWN" : patient.gender());
                bloodGroup.setSelectedItem(patient.bloodGroup() == null ? "UNKNOWN" : patient.bloodGroup());
                emergencyName.setText(nz(patient.emergencyContactName()));
                emergencyPhone.setText(nz(patient.emergencyContactPhone()));
                emergencyRelation.setText(nz(patient.emergencyContactRelation()));
                allergies.setText(nz(patient.allergies()));
                chronic.setText(nz(patient.chronicConditions()));
                medications.setText(nz(patient.currentMedications()));
                insuranceProvider.setText(nz(patient.insuranceProvider()));
                insurancePolicy.setText(nz(patient.insurancePolicyNumber()));
                insuranceExpiry.setDate(patient.insuranceExpiry());
            }
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    private void save() {
        try {
            if (mode == Mode.DOCTOR) {
                if (doctor == null) return;
                Doctor updated = new Doctor(
                        doctor.id(),
                        doctor.userId(),
                        doctor.fullName(),
                        blank(phone.getText()),
                        blank(email.getText()),
                        specialization.getText().trim(),
                        license.getText().trim(),
                        licenseExpiry.getDate(),
                        blank(dea.getText()),
                        blank(npi.getText()),
                        blank(boardCertifications.getText()),
                        blank(medicalSchool.getText()),
                        parseInt(graduationYear.getText()),
                        parseInt(yearsOfExperience.getText()),
                        doctor.clinicName(),
                        doctor.clinicAddress(),
                        doctor.clinicPhone(),
                        blank(address.getText()),
                        blank(city.getText()),
                        blank(postal.getText()),
                        blank(country.getText()),
                        blank(languages.getText()),
                        acceptingNewPatients.isSelected(),
                        doctor.slotMinutes(),
                        blank(bio.getText()),
                        doctor.createdAt(),
                        doctor.updatedAt());
                DoctorDao.updateProfile(updated);
                doctor = updated;
            } else {
                if (patient == null) return;
                Patient updated = new Patient(
                        patient.id(),
                        patient.userId(),
                        patient.fullName(),
                        blank(phone.getText()),
                        blank(email.getText()),
                        patient.medicalRecordNumber(),
                        dob.getDate(),
                        gender.getSelectedItem().toString(),
                        bloodGroup.getSelectedItem().toString(),
                        blank(address.getText()),
                        blank(city.getText()),
                        blank(postal.getText()),
                        blank(country.getText()),
                        blank(emergencyName.getText()),
                        blank(emergencyPhone.getText()),
                        blank(emergencyRelation.getText()),
                        blank(allergies.getText()),
                        blank(chronic.getText()),
                        blank(medications.getText()),
                        blank(insuranceProvider.getText()),
                        blank(insurancePolicy.getText()),
                        insuranceExpiry.getDate(),
                        patient.createdAt(),
                        patient.updatedAt());
                PatientDao.updateProfile(updated);
                patient = updated;
            }
            UiUtil.info(this, "Profile saved.");
            reload();
        } catch (IllegalArgumentException ex) {
            UiUtil.error(this, ex.getMessage());
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static String blank(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static Integer parseInt(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Not a number: " + s); }
    }
}