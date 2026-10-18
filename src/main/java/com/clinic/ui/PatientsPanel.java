package com.clinic.ui;

import com.clinic.dao.PatientDao;
import com.clinic.dao.UserDao;
import com.clinic.model.Patient;
import com.clinic.model.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public class PatientsPanel extends JPanel implements Refreshable {
    private final DefaultTableModel model = UiUtil.model("Name", "Username", "Phone", "Email");
    private final JTable table = Tables.create(model);
    private final TableRowSorter<DefaultTableModel> sorter = Tables.sortable(table, model);
    private final JTextField search = new JTextField(20);

    public PatientsPanel() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        search.putClientProperty("JTextField.placeholderText", "Search patients...");
        search.putClientProperty("JTextField.leadingIcon", Icons.search(16, Theme.MUTED));
        search.putClientProperty("JTextField.showClearButton", true);
        Tables.onChange(search, () -> {
            String q = search.getText().trim();
            if (q.isEmpty()) sorter.setRowFilter(null);
            else sorter.setRowFilter(Tables.contains(q));
        });

        FadeButton add = FadeButton.primary("Add patient", Icons.plus(16, Color.WHITE));
        FadeButton edit = FadeButton.secondary("Edit selected");
        FadeButton reset = FadeButton.secondary("Reset password");
        FadeButton refresh = FadeButton.secondary("Refresh");
        add.addActionListener(e -> {
            if (AccountDialog.show(this, "PATIENT")) reload();
        });
        edit.addActionListener(e -> editSelected());
        reset.addActionListener(e -> resetPassword());
        refresh.addActionListener(e -> reload());

        JPanel bar = Theme.clear(new JPanel(new BorderLayout()));
        JPanel left = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)));
        left.add(search);
        JPanel right = Theme.clear(new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)));
        right.add(reset);
        right.add(refresh);
        right.add(edit);
        right.add(add);
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);

        add(bar, BorderLayout.NORTH);
        add(Tables.scroll(table), BorderLayout.CENTER);
        reload();
    }

    @Override public void reload() {
        try {
            model.setRowCount(0);
            for (User u : UserDao.listByRole("PATIENT")) {
                model.addRow(new Object[]{u.fullName(), u.username(),
                        u.phone() == null ? "" : u.phone(), u.email() == null ? "" : u.email()});
            }
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    private User selected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        String username = String.valueOf(model.getValueAt(modelRow, 1));
        try {
            for (User u : UserDao.listByRole("PATIENT")) {
                if (u.username().equals(username)) return u;
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    private void editSelected() {
        User target = selected();
        if (target == null) {
            UiUtil.error(this, "Select a patient first.");
            return;
        }
        Patient p;
        try {
            p = PatientDao.findByUserId(target.id());
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
            return;
        }
        if (p == null) {
            UiUtil.error(this, "No patient profile found for " + target.fullName() + ".");
            return;
        }
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Patient profile - " + target.fullName(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.add(ProfilePanel.openPatient(p.id()));
        dlg.setSize(640, 720);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        reload();
    }

    private void resetPassword() {
        User target = selected();
        if (target == null) {
            UiUtil.error(this, "Select a patient first.");
            return;
        }
        ChangePasswordDialog.forUser(
                SwingUtilities.getWindowAncestor(this),
                target.id(),
                target.fullName());
    }
}