package com.clinic.ui;

import com.clinic.model.User;

public class AdminFrame extends AppFrame {
    public AdminFrame(User user) {
        super(user, "Clinic Appointment System - Admin / Reception");
        addTab("Dashboard", Icons.grid(18, Theme.ACCENT),
                new DashboardPanel(user, AppointmentsPanel.Mode.ADMIN, 0, () -> tabs.setSelectedIndex(2)));
        addTab("Appointments", Icons.calendar(18, Theme.ACCENT), new AppointmentsPanel(AppointmentsPanel.Mode.ADMIN, 0));
        addTab("Book Appointment", Icons.plus(18, Theme.ACCENT), new BookingPanel(user, true));
        addTab("Doctors", Icons.cross(18, Theme.ACCENT), new DoctorsPanel());
        addTab("Patients", Icons.user(18, Theme.ACCENT), new PatientsPanel());
        addTab("My Account", Icons.user(18, Theme.ACCENT), new AdminAccountPanel(user));
    }
}