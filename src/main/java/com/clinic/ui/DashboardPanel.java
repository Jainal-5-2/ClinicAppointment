package com.clinic.ui;

import com.clinic.dao.AppointmentDao;
import com.clinic.dao.DoctorDao;
import com.clinic.dao.UserDao;
import com.clinic.model.Appointment;
import com.clinic.model.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

/** Home tab: greeting, summary tiles and today's / upcoming appointments. */
public class DashboardPanel extends JPanel implements Refreshable {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter LONG = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH);

    private final AppointmentsPanel.Mode mode;
    private final int ownerId;
    private final StatCard[] cards;
    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel tableTitle = new JLabel();

    public DashboardPanel(User user, AppointmentsPanel.Mode mode, int ownerId, Runnable onBook) {
        this.mode = mode;
        this.ownerId = ownerId;
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel head = Theme.clear(new JPanel(new BorderLayout()));
        JPanel text = Theme.clear(new JPanel());
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        String first = user.fullName().trim().split("\\s+")[0];
        JLabel hi = new JLabel(greeting() + ", " + first);
        hi.setFont(Theme.font(Font.BOLD, 24f));
        JLabel date = Theme.muted(LONG.format(LocalDate.now()));
        text.add(hi);
        text.add(date);
        head.add(text, BorderLayout.WEST);
        if (onBook != null) {
            FadeButton book = FadeButton.primary("Book appointment", Icons.plus(16, Color.WHITE));
            book.addActionListener(e -> onBook.run());
            JPanel right = Theme.clear(new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)));
            right.add(book);
            head.add(right, BorderLayout.EAST);
        }

        String[] titles;
        Icon[] icons;
        if (mode == AppointmentsPanel.Mode.ADMIN) {
            titles = new String[]{"Today's appointments", "Next 7 days", "Doctors", "Patients"};
            icons = new Icon[]{Icons.calendar(16, Theme.ACCENT), Icons.clock(16, Theme.ACCENT),
                    Icons.cross(16, Theme.ACCENT), Icons.user(16, Theme.ACCENT)};
        } else if (mode == AppointmentsPanel.Mode.DOCTOR) {
            titles = new String[]{"Today's appointments", "Completed today", "Next 7 days", "Next appointment"};
            icons = new Icon[]{Icons.calendar(16, Theme.ACCENT), Icons.cross(16, Theme.ACCENT),
                    Icons.clock(16, Theme.ACCENT), Icons.user(16, Theme.ACCENT)};
        } else {
            titles = new String[]{"Upcoming appointments", "Next appointment"};
            icons = new Icon[]{Icons.calendar(16, Theme.ACCENT), Icons.clock(16, Theme.ACCENT)};
        }
        cards = new StatCard[titles.length];
        JPanel row = Theme.clear(new JPanel(new GridLayout(1, 0, 14, 14)));
        for (int i = 0; i < titles.length; i++) {
            cards[i] = new StatCard(titles[i], icons[i]);
            row.add(cards[i]);
        }

        if (mode == AppointmentsPanel.Mode.PATIENT) {
            model = UiUtil.model("Date", "Time", "Doctor", "Status");
        } else {
            model = UiUtil.model("Time", "Doctor", "Patient", "Status");
        }
        table = Tables.create(model);
        table.getColumnModel().getColumn(3).setCellRenderer(new Tables.StatusRenderer());
        tableTitle.setFont(Theme.font(Font.BOLD, 16f));
        Card section = new Card(new BorderLayout(0, 10));
        section.add(tableTitle, BorderLayout.NORTH);
        section.add(Tables.scroll(table), BorderLayout.CENTER);

        JPanel center = Theme.clear(new JPanel(new BorderLayout(0, 16)));
        center.add(row, BorderLayout.NORTH);
        center.add(section, BorderLayout.CENTER);

        add(head, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        reload();

        Timer timer = new Timer(30000, e -> {
            if (!isDisplayable()) ((Timer) e.getSource()).stop();
            else if (isShowing()) reload();
        });
        timer.start();
    }

    private static String greeting() {
        int h = LocalDateTime.now().getHour();
        return h < 12 ? "Good morning" : (h < 18 ? "Good afternoon" : "Good evening");
    }

    @Override public void reload() {
        try {
            List<Appointment> all;
            if (mode == AppointmentsPanel.Mode.ADMIN) all = AppointmentDao.listAll(null);
            else if (mode == AppointmentsPanel.Mode.DOCTOR) all = AppointmentDao.listForDoctor(ownerId, null);
            else all = AppointmentDao.listForPatient(ownerId);

            LocalDate today = LocalDate.now();
            LocalDateTime now = LocalDateTime.now();
            int todayCount = 0;
            int todayDone = 0;
            int week = 0;
            int upcoming = 0;
            Appointment next = null;
            model.setRowCount(0);

            for (Appointment a : all) {
                if (a.date().isBefore(today)) continue;
                boolean cancelled = "CANCELLED".equals(a.status());
                boolean isToday = a.date().equals(today);
                boolean future = !LocalDateTime.of(a.date(), a.start()).isBefore(now);
                boolean active = a.isPending() || a.isApproved();

                if (!cancelled) {
                    if (isToday) {
                        todayCount++;
                        if (a.isCompleted()) todayDone++;
                    } else if (!a.date().isAfter(today.plusDays(7))) {
                        week++;
                    }
                }
                if (a.isApproved() && future) {
                    upcoming++;
                    if (next == null) next = a;
                }
                if (mode == AppointmentsPanel.Mode.PATIENT) {
                    if (active && future && model.getRowCount() < 15) {
                        model.addRow(new Object[]{DAY.format(a.date()), a.start() + " - " + a.end(),
                                "Dr. " + a.doctorName(), a.status()});
                    }
                } else if (isToday) {
                    model.addRow(new Object[]{a.start() + " - " + a.end(), "Dr. " + a.doctorName(),
                            a.patientName(), a.status()});
                }
            }

            String nextValue = next == null ? "None"
                    : (next.date().equals(today) ? "Today " : DAY.format(next.date()) + " ") + next.start();
            String nextSub = next == null ? "" : (mode == AppointmentsPanel.Mode.DOCTOR
                    ? next.patientName() : "Dr. " + next.doctorName());

            if (mode == AppointmentsPanel.Mode.ADMIN) {
                cards[0].set(String.valueOf(todayCount), "not cancelled");
                cards[1].set(String.valueOf(week), "after today");
                cards[2].set(String.valueOf(DoctorDao.listAll().size()), "active");
                cards[3].set(String.valueOf(UserDao.listByRole("PATIENT").size()), "registered");
                tableTitle.setText("Today's appointments (" + model.getRowCount() + ")");
            } else if (mode == AppointmentsPanel.Mode.DOCTOR) {
                cards[0].set(String.valueOf(todayCount), "not cancelled");
                cards[1].set(String.valueOf(todayDone), "today");
                cards[2].set(String.valueOf(week), "after today");
                cards[3].set(nextValue, nextSub);
                tableTitle.setText("Today's appointments (" + model.getRowCount() + ")");
            } else {
                cards[0].set(String.valueOf(upcoming), "approved");
                cards[1].set(nextValue, nextSub);
                tableTitle.setText("Your upcoming appointments (" + model.getRowCount() + ")");
            }
        } catch (SQLException ex) {
            tableTitle.setText("Could not load data: " + ex.getMessage());
        }
    }
}