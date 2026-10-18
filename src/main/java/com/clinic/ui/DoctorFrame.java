package com.clinic.ui;

import com.clinic.model.Doctor;
import com.clinic.model.User;

public class DoctorFrame extends AppFrame {
    public DoctorFrame(User user, Doctor doctor) {
        super(user, "Clinic Appointment System - Doctor");
        addTab("Dashboard", Icons.grid(18, Theme.ACCENT),
                new DashboardPanel(user, AppointmentsPanel.Mode.DOCTOR, doctor.id(), null));
        addTab("My Appointments", Icons.calendar(18, Theme.ACCENT),
                new AppointmentsPanel(AppointmentsPanel.Mode.DOCTOR, doctor.id()));
        addTab("My Schedule", Icons.clock(18, Theme.ACCENT), new SchedulePanel(doctor));
        addTab("Profile", Icons.user(18, Theme.ACCENT), new ProfilePanel(ProfilePanel.Mode.DOCTOR, user.id()));
    }
}
