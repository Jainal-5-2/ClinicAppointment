package com.clinic.ui;

import com.clinic.model.User;

public class PatientFrame extends AppFrame {

    public PatientFrame(User user) {
        super(user, "Clinic Appointment System - Patient");
        addTab("Home", Icons.grid(18, Theme.ACCENT),
                new DashboardPanel(user, AppointmentsPanel.Mode.PATIENT, user.id(), () -> tabs.setSelectedIndex(1)));
        addTab("Book Appointment", Icons.plus(18, Theme.ACCENT), new BookingPanel(user, false));
        addTab("My Appointments", Icons.calendar(18, Theme.ACCENT),
                new AppointmentsPanel(AppointmentsPanel.Mode.PATIENT, user.id()));
        addTab("Profile", Icons.user(18, Theme.ACCENT), new ProfilePanel(ProfilePanel.Mode.PATIENT, user.id()));
    }
}
