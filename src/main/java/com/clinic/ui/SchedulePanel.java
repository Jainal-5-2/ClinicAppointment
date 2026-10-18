package com.clinic.ui;

import com.clinic.dao.AppointmentDao;
import com.clinic.dao.DoctorDao;
import com.clinic.dao.ScheduleDao;
import com.clinic.model.DayOff;
import com.clinic.model.Doctor;
import com.clinic.model.ScheduleRule;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Lets a doctor (or admin) set weekly recurring hours, appointment length and
 * days off.
 */
public class SchedulePanel extends JPanel {

    private static final String[] DAYS
            = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    private Doctor doctor;
    private final DefaultTableModel ruleModel = UiUtil.model("Day", "Start", "End");
    private final DefaultTableModel offModel = UiUtil.model("Date", "Reason");
    private final JTable ruleTable = Tables.create(ruleModel);
    private final JTable offTable = Tables.create(offModel);
    private List<ScheduleRule> rules = new ArrayList<>();
    private List<DayOff> offs = new ArrayList<>();

    private final JComboBox<Integer> slotBox = new JComboBox<>(new Integer[]{10, 15, 20, 30, 40, 45, 60});
    private final JComboBox<String> dayBox = new JComboBox<>(DAYS);
    private final JSpinner startSp = UiUtil.timeSpinner(9, 0);
    private final JSpinner endSp = UiUtil.timeSpinner(17, 0);
    private final DateField offDate = new DateField();
    private final JTextField offReason = new JTextField(14);

    public SchedulePanel(Doctor doctor) {
        this.doctor = doctor;
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // top card: appointment length
        Card top = new Card(new FlowLayout(FlowLayout.LEFT, 10, 0));
        top.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        JLabel who = new JLabel("Dr. " + doctor.fullName());
        who.setFont(Theme.font(Font.BOLD, 16f));
        FadeButton saveLen = FadeButton.secondary("Save length");
        slotBox.setSelectedItem(doctor.slotMinutes());
        saveLen.addActionListener(e -> saveSlotLength());
        top.add(who);
        top.add(Theme.muted("   Appointment length (min):"));
        top.add(slotBox);
        top.add(saveLen);

        // left card: weekly hours
        Card left = new Card(new BorderLayout(0, 10));
        left.add(cardTitle("Weekly working hours"), BorderLayout.NORTH);
        left.add(Tables.scroll(ruleTable), BorderLayout.CENTER);
        FadeButton addRule = FadeButton.primary("Add hours", Icons.plus(16, java.awt.Color.WHITE));
        FadeButton delRule = FadeButton.danger("Delete selected");
        addRule.addActionListener(e -> addRule());
        delRule.addActionListener(e -> deleteRule());
        JPanel ruleRow1 = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)));
        ruleRow1.add(dayBox);
        ruleRow1.add(startSp);
        ruleRow1.add(new JLabel("to"));
        ruleRow1.add(endSp);
        JPanel ruleRow2 = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)));
        ruleRow2.add(addRule);
        ruleRow2.add(delRule);
        JPanel ruleBar = Theme.clear(new JPanel(new GridLayout(2, 1, 0, 8)));
        ruleBar.add(ruleRow1);
        ruleBar.add(ruleRow2);
        left.add(ruleBar, BorderLayout.SOUTH);

        // right card: days off
        Card right = new Card(new BorderLayout(0, 10));
        right.add(cardTitle("Days off (upcoming)"), BorderLayout.NORTH);
        right.add(Tables.scroll(offTable), BorderLayout.CENTER);
        FadeButton addOff = FadeButton.primary("Add day off", Icons.plus(16, java.awt.Color.WHITE));
        FadeButton delOff = FadeButton.danger("Remove selected");
        addOff.addActionListener(e -> addDayOff());
        delOff.addActionListener(e -> deleteDayOff());
        offDate.setMinDate(LocalDate.now());
        JPanel offRow1 = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)));
        offRow1.add(offDate);
        offRow1.add(new JLabel("Reason:"));
        offRow1.add(offReason);
        JPanel offRow2 = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)));
        offRow2.add(addOff);
        offRow2.add(delOff);
        JPanel offBar = Theme.clear(new JPanel(new GridLayout(2, 1, 0, 8)));
        offBar.add(offRow1);
        offBar.add(offRow2);
        right.add(offBar, BorderLayout.SOUTH);

        JPanel center = Theme.clear(new JPanel(new GridLayout(1, 2, 14, 14)));
        center.add(left);
        center.add(right);

        JLabel note = Theme.muted("Existing bookings are never removed automatically when you change hours or add a day off.");
        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(note, BorderLayout.SOUTH);
        reload();
    }

    private static JLabel cardTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(Font.BOLD, 16f));
        return l;
    }

    private void reload() {
        try {
            rules = ScheduleDao.rulesFor(doctor.id());
            ruleModel.setRowCount(0);
            for (ScheduleRule r : rules) {
                ruleModel.addRow(new Object[]{r.dayName(), r.start(), r.end()});
            }
            offs = ScheduleDao.daysOffFor(doctor.id());
            offModel.setRowCount(0);
            for (DayOff o : offs) {
                offModel.addRow(new Object[]{o.date(), o.reason() == null ? "" : o.reason()});
            }
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    private void saveSlotLength() {
        int minutes = (Integer) slotBox.getSelectedItem();
        try {
            DoctorDao.updateSlotMinutes(doctor.id(), minutes);
            Doctor fresh = DoctorDao.findById(doctor.id());
            if (fresh != null) {
                doctor = fresh;
            }
            UiUtil.info(this, "Appointment length saved: " + minutes + " minutes.");
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    private void addRule() {
        int dow = dayBox.getSelectedIndex() + 1;
        LocalTime s = UiUtil.time(startSp);
        LocalTime e = UiUtil.time(endSp);
        if (!s.isBefore(e)) {
            UiUtil.error(this, "Start time must be before end time.");
            return;
        }
        if (Duration.between(s, e).toMinutes() < doctor.slotMinutes()) {
            UiUtil.error(this, "That range is shorter than one appointment (" + doctor.slotMinutes() + " min).");
            return;
        }
        for (ScheduleRule r : rules) {
            if (r.dayOfWeek() == dow && s.isBefore(r.end()) && e.isAfter(r.start())) {
                UiUtil.error(this, "This overlaps existing hours on " + r.dayName() + " (" + r.start() + "-" + r.end() + ").");
                return;
            }
        }
        try {
            ScheduleDao.addRule(doctor.id(), dow, s, e);
            reload();
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    private void deleteRule() {
        int row = ruleTable.getSelectedRow();
        if (row < 0) {
            UiUtil.error(this, "Select a row to delete.");
            return;
        }
        if (!UiUtil.confirm(this, "Delete these working hours? Already booked appointments stay booked.")) {
            return;
        }
        try {
            ScheduleDao.deleteRule(rules.get(row).id());
            reload();
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    private void addDayOff() {
        LocalDate d = offDate.getDate();
        if (d == null) {
            UiUtil.error(this, "Pick a date first.");
            return;
        }
        if (d.isBefore(LocalDate.now())) {
            UiUtil.error(this, "Choose today or a future date.");
            return;
        }
        try {
            int dow = d.getDayOfWeek().getValue();  // 1=Mon .. 7=Sun
            if (ScheduleDao.isWorkingDay(doctor.id(), dow)) {
                UiUtil.error(this,
                        "Dr. " + doctor.fullName() + " is scheduled to work on "
                        + d.getDayOfWeek() + "s. Remove the working hours for that day first.");
                return;
            }
            int booked = AppointmentDao.countActive(doctor.id(), d);
            if (booked > 0 && !UiUtil.confirm(this, booked + " appointment(s) are already booked on " + d
                    + ". They will stay booked until cancelled (Appointments tab). Add the day off anyway?")) {
                return;
            }
            if (!ScheduleDao.addDayOff(doctor.id(), d, offReason.getText())) {
                UiUtil.error(this, "That date is already a day off.");
                return;
            }
            offReason.setText("");
            offDate.clear();
            reload();
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    private void deleteDayOff() {
        int row = offTable.getSelectedRow();
        if (row < 0) {
            UiUtil.error(this, "Select a day off to remove.");
            return;
        }
        try {
            ScheduleDao.deleteDayOff(offs.get(row).id());
            reload();
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }
}
