package com.clinic.ui;

import com.clinic.dao.DoctorDao;
import com.clinic.dao.UserDao;
import com.clinic.model.Doctor;
import com.clinic.model.Slot;
import com.clinic.model.User;
import com.clinic.service.SlotService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Timer;

/** Pick a doctor + date, see free slots as chips, and book one. Used by patients and by staff. */
public class BookingPanel extends JPanel implements Refreshable {
    private final User user;
    private final boolean staff;
    private final JComboBox<Doctor> doctorBox = new JComboBox<>();
    private final JComboBox<User> patientBox = new JComboBox<>();
    private final DateField dateField = new DateField();
    private final JPanel chipGrid = new JPanel(new GridLayout(0, 4, 8, 8));
    private ButtonGroup group = new ButtonGroup();
    private Slot selected;
    private final JTextField notes = new JTextField();
    private final JLabel status = new JLabel(" ");
    private final JLabel hours = new JLabel(" ");
    private final FadeButton bookBtn = FadeButton.primary("Book appointment", Icons.calendar(16, Color.WHITE));
    private boolean loading;

    public BookingPanel(User user, boolean staff) {
        this.user = user;
        this.staff = staff;
        setLayout(new BorderLayout(16, 0));
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        Card left = new Card(new GridBagLayout());
        left.setPreferredSize(new Dimension(340, 10));
        int y = 0;
        if (staff) field(left, y++, "Patient", patientBox);
        field(left, y++, "Doctor", doctorBox);
        dateField.setMinDate(LocalDate.now());
        field(left, y++, "Date", dateField);
        notes.putClientProperty("JTextField.placeholderText", "Optional");
        field(left, y++, "Reason / notes", notes);

        hours.setForeground(Theme.MUTED);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = y * 2;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        g.insets = new Insets(12, 0, 0, 0);
        left.add(hours, g);

        bookBtn.setEnabled(false);
        bookBtn.addActionListener(e -> book());
        g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = y * 2 + 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        g.insets = new Insets(16, 0, 0, 0);
        left.add(bookBtn, g);

        g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = y * 2 + 2;
        g.weighty = 1;
        left.add(Theme.clear(new JPanel()), g);

        Card right = new Card(new BorderLayout(0, 10));
        JLabel title = new JLabel("Available times");
        title.setFont(Theme.font(Font.BOLD, 16f));
        status.setForeground(Theme.MUTED);
        JPanel header = Theme.clear(new JPanel(new BorderLayout()));
        header.add(title, BorderLayout.WEST);
        header.add(status, BorderLayout.EAST);
        JPanel holder = Theme.clear(new JPanel(new BorderLayout()));
        chipGrid.setOpaque(false);
        holder.add(chipGrid, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        right.add(header, BorderLayout.NORTH);
        right.add(scroll, BorderLayout.CENTER);

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.CENTER);

        doctorBox.addActionListener(e -> { if (!loading) refresh(); });
        dateField.addChangeListener(() -> { if (!loading) refresh(); });

        loadPeople();
        refresh();

        Timer timer = new Timer(15000, e -> {
            if (!isDisplayable()) ((Timer) e.getSource()).stop();
            else if (isShowing()) refresh();
        });
        timer.start();
    }

    private static void field(JPanel p, int index, String label, Component c) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = index * 2;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(index == 0 ? 0 : 12, 0, 3, 0);
        JLabel l = new JLabel(label);
        l.setForeground(Theme.MUTED);
        p.add(l, g);
        g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = index * 2 + 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(c, g);
    }

    @Override public void reload() {
        loadPeople();
        refresh();
    }

    private void loadPeople() {
        loading = true;
        try {
            Doctor selDoc = (Doctor) doctorBox.getSelectedItem();
            doctorBox.removeAllItems();
            for (Doctor d : DoctorDao.listAll()) {
                doctorBox.addItem(d);
                if (selDoc != null && selDoc.id() == d.id()) doctorBox.setSelectedItem(d);
            }
            if (staff) {
                User selPat = (User) patientBox.getSelectedItem();
                patientBox.removeAllItems();
                for (User p : UserDao.listByRole("PATIENT")) {
                    patientBox.addItem(p);
                    if (selPat != null && selPat.id() == p.id()) patientBox.setSelectedItem(p);
                }
            }
        } catch (SQLException ex) {
            status.setText("Database error: " + ex.getMessage());
        } finally {
            loading = false;
        }
    }

    private void refresh() {
        Doctor chosen = (Doctor) doctorBox.getSelectedItem();
        Slot keep = selected;
        selected = null;
        group = new ButtonGroup();
        chipGrid.removeAll();
        try {
            if (chosen == null) {
                status.setText("No doctors available yet.");
                hours.setText(" ");
                return;
            }
            Doctor d = DoctorDao.findById(chosen.id());
            if (d == null) return;
            LocalDate date = dateField.getDate();
            List<Slot> slots = SlotService.available(d, date);
            for (Slot s : slots) {
                ChipButton chip = new ChipButton(s.start().toString());
                chip.addActionListener(e -> {
                    selected = s;
                    bookBtn.setEnabled(true);
                });
                group.add(chip);
                chipGrid.add(chip);
                if (s.equals(keep)) {
                    chip.setSelected(true);
                    selected = s;
                }
            }
            hours.setText("<html><body style='width:270px'>" + SlotService.describeHours(d)
                    + "<br>Each appointment is " + d.slotMinutes() + " minutes.</body></html>");
            if (slots.isEmpty()) {
                status.setText("No free slots (day off, not working, or fully booked)");
            } else {
                status.setText(slots.size() + " free - updated " + LocalTime.now().withNano(0));
            }
        } catch (SQLException ex) {
            status.setText("Database error: " + ex.getMessage());
        } finally {
            bookBtn.setEnabled(selected != null);
            chipGrid.revalidate();
            chipGrid.repaint();
        }
    }

    private void book() {
        Doctor chosen = (Doctor) doctorBox.getSelectedItem();
        Slot slot = selected;
        if (chosen == null) { UiUtil.error(this, "Choose a doctor."); return; }
        if (slot == null) { UiUtil.error(this, "Select a time slot first."); return; }
        int patientId = user.id();
        if (staff) {
            User p = (User) patientBox.getSelectedItem();
            if (p == null) { UiUtil.error(this, "Choose a patient (add one in the Patients tab)."); return; }
            patientId = p.id();
        }
        LocalDate date = dateField.getDate();
        if (!UiUtil.confirm(this, "Book " + date + " at " + slot + " with Dr. " + chosen.fullName() + "?")) return;
        try {
            Doctor d = DoctorDao.findById(chosen.id());
            String err = SlotService.book(d, patientId, date, slot, notes.getText());
            if (err != null) {
                UiUtil.error(this, err);
            } else {
                UiUtil.info(this, "Appointment requested. It will appear once approved.");
                notes.setText("");
            }
            refresh();
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }
}