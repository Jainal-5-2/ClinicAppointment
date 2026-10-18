package com.clinic.ui;

import com.clinic.dao.DoctorDao;
import com.clinic.model.Doctor;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class DoctorsPanel extends JPanel implements Refreshable {
    private final DefaultTableModel model = UiUtil.model("Name", "Specialization", "Appointment length (min)");
    private final JTable table = Tables.create(model);
    private List<Doctor> doctors = new ArrayList<>();

    public DoctorsPanel() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        FadeButton add = FadeButton.primary("Add doctor", Icons.plus(16, Color.WHITE));
        FadeButton edit = FadeButton.secondary("Edit selected profile");
        FadeButton schedule = FadeButton.secondary("Edit selected doctor's schedule");
        FadeButton reset = FadeButton.secondary("Reset password");
        FadeButton refresh = FadeButton.secondary("Refresh");

        add.addActionListener(e -> {
            if (AccountDialog.show(this, "DOCTOR")) reload();
        });
        edit.addActionListener(e -> editProfile());
        schedule.addActionListener(e -> editSchedule());
        reset.addActionListener(e -> resetPassword());
        refresh.addActionListener(e -> reload());

        JPanel bar = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)));
        bar.add(add);
        bar.add(edit);
        bar.add(schedule);
        bar.add(reset);
        bar.add(refresh);
        add(bar, BorderLayout.NORTH);
        add(Tables.scroll(table), BorderLayout.CENTER);
        reload();
    }

    private Doctor selected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        String name = String.valueOf(model.getValueAt(modelRow, 0));
        for (Doctor d : doctors) {
            if (("Dr. " + d.fullName()).equals(name)) return d;
        }
        return null;
    }

    private void editProfile() {
        Doctor d = selected();
        if (d == null) {
            UiUtil.error(this, "Select a doctor first.");
            return;
        }
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Doctor profile - Dr. " + d.fullName(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.add(ProfilePanel.openDoctor(d.id()));
        dlg.setSize(640, 720);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        reload();
    }

        private void editSchedule() {
        Doctor d = selected();
        if (d == null) {
            UiUtil.error(this, "Select a doctor first.");
            return;
        }
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Schedule - Dr. " + d.fullName(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        // Wrap SchedulePanel so we can add a Maximize/Restore button without
        // touching the panel itself.
        JPanel wrap = new JPanel(new java.awt.BorderLayout(0, 0));
        wrap.add(new SchedulePanel(d), java.awt.BorderLayout.CENTER);

        FadeButton maximize = FadeButton.secondary("Maximize");
        final boolean[] maximized = {false};
        final java.awt.Rectangle[] original = {null};
        maximize.addActionListener(e -> {
            if (!maximized[0]) {
                original[0] = dlg.getBounds();
                java.awt.Rectangle screen = java.awt.GraphicsEnvironment
                        .getLocalGraphicsEnvironment()
                        .getMaximumWindowBounds();
                dlg.setBounds(screen);
                maximized[0] = true;
                maximize.setText("Restore");
            } else {
                if (original[0] != null) dlg.setBounds(original[0]);
                maximized[0] = false;
                maximize.setText("Maximize");
            }
        });
        JPanel bar = Theme.clear(new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 8)));
        bar.add(maximize);

        dlg.setLayout(new java.awt.BorderLayout());
        dlg.add(wrap, java.awt.BorderLayout.CENTER);
        dlg.add(bar, java.awt.BorderLayout.SOUTH);

        dlg.setMinimumSize(new java.awt.Dimension(760, 460));
        dlg.setSize(1120, 620);
        dlg.setResizable(true);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        reload();
    }

    private void resetPassword() {
        Doctor d = selected();
        if (d == null) {
            UiUtil.error(this, "Select a doctor first.");
            return;
        }
        ChangePasswordDialog.forUser(
                SwingUtilities.getWindowAncestor(this),
                d.userId(),
                "Dr. " + d.fullName());
    }

    @Override public void reload() {
        try {
            doctors = DoctorDao.listAll();
            model.setRowCount(0);
            for (Doctor d : doctors) {
                model.addRow(new Object[]{"Dr. " + d.fullName(), d.specialization(), d.slotMinutes()});
            }
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }
}