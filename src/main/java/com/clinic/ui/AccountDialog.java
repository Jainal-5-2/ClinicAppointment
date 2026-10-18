package com.clinic.ui;

import com.clinic.dao.DoctorDao;
import com.clinic.dao.PatientDao;
import com.clinic.dao.UserDao;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Create account dialog used for patient sign-up and admin-created doctors/patients. */
public final class AccountDialog {
    private AccountDialog() { }

    /** @param role "PATIENT" or "DOCTOR". Returns true if an account was created. */
    public static boolean show(Component parent, String role) {
        boolean doctor = "DOCTOR".equals(role);
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(parent),
                doctor ? "Add doctor" : "Create patient account",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        JTextField name = new JTextField(22);
        JTextField username = new JTextField(22);
        JPasswordField password = new JPasswordField(22);
        JTextField phone = new JTextField(22);
        JTextField email = new JTextField(22);

        JTextField address = new JTextField(22);
        JTextField city = new JTextField(22);
        JTextField postal = new JTextField(22);
        JTextField country = new JTextField(22);

        JTextField spec = new JTextField(22);
        JTextField license = new JTextField(22);
        JTextField clinicAddress = new JTextField(22);
        JComboBox<Integer> slot = new JComboBox<>(new Integer[]{10, 15, 20, 30, 40, 45, 60});
        slot.setSelectedItem(30);

        JPanel form = new JPanel(new GridBagLayout());
        int row = 0;

        JLabel heading = new JLabel(doctor ? "New doctor" : "New patient");
        heading.setFont(Theme.font(Font.BOLD, 20f));
        addRow(form, row++, null, heading);

        addSection(form, row++, "Account");
        addRow(form, row++, "Full name", name);
        addRow(form, row++, "Username", username);
        addRow(form, row++, "Password (min 6)", password);
        addRow(form, row++, "Phone", phone);
        addRow(form, row++, "Email", email);

        addSection(form, row++, "Address");
        if (doctor) {
            addRow(form, row++, "Clinic address", clinicAddress);
        }
        addRow(form, row++, "Street", address);
        addRow(form, row++, "City", city);
        addRow(form, row++, "Postal code", postal);
        addRow(form, row++, "Country", country);

        if (doctor) {
            addSection(form, row++, "Professional");
            addRow(form, row++, "Specialization", spec);
            addRow(form, row++, "License number", license);
            addRow(form, row++, "Appointment length (min)", slot);
        }

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        final boolean[] created = {false};

        FadeButton save = FadeButton.primary(doctor ? "Add doctor" : "Create account");
        FadeButton cancel = FadeButton.secondary("Cancel");
        FadeButton maximize = FadeButton.secondary("Maximize");

        cancel.addActionListener(e -> dlg.dispose());

        final boolean[] maximized = {false};
        final Rectangle[] original = {null};
        maximize.addActionListener(e -> {
            if (!maximized[0]) {
                original[0] = dlg.getBounds();
                Rectangle screen = GraphicsEnvironment
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

        save.addActionListener(e -> {
            String pw = new String(password.getPassword());

            if (name.getText().trim().isEmpty()) { UiUtil.error(dlg, "Full name is required."); return; }
            if (username.getText().trim().length() < 3) { UiUtil.error(dlg, "Username must be at least 3 characters."); return; }
            if (pw.length() < 6) { UiUtil.error(dlg, "Password must be at least 6 characters."); return; }
            if (doctor && spec.getText().trim().isEmpty()) { UiUtil.error(dlg, "Specialization is required."); return; }
            if (doctor && license.getText().trim().isEmpty()) { UiUtil.error(dlg, "License number is required."); return; }
            if (!doctor && address.getText().trim().isEmpty()) { UiUtil.error(dlg, "Street address is required."); return; }

            try {
                if (UserDao.usernameExists(username.getText())) {
                    UiUtil.error(dlg, "That username is already taken.");
                    return;
                }
                if (doctor) {
                    DoctorDao.createWithUser(
                            username.getText(), pw, name.getText(), phone.getText(), email.getText(),
                            spec.getText(), license.getText(),
                            null, null, null,
                            null, null, null, null,
                            null, clinicAddress.getText(), null,
                            address.getText(), city.getText(), postal.getText(), country.getText(),
                            null, true, (Integer) slot.getSelectedItem(), null);
                } else {
                    PatientDao.createWithUser(
                            username.getText(), pw, name.getText(), phone.getText(), email.getText(),
                            null, null, "UNKNOWN", "UNKNOWN",
                            address.getText(), city.getText(), postal.getText(), country.getText(),
                            null, null, null,
                            null, null, null,
                            null, null, null);
                }
                created[0] = true;
                dlg.dispose();
            } catch (SQLException ex) {
                UiUtil.dbError(dlg, ex);
            }
        });

        JPanel actions = Theme.clear(new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)));
        actions.add(maximize);
        actions.add(cancel);
        actions.add(save);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        root.add(scroll, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.pack();
        dlg.setMinimumSize(new java.awt.Dimension(500, 400));
        int prefW = Math.max(640, dlg.getWidth());
        int prefH = doctor ? 720 : 640;
        dlg.setSize(prefW, prefH);
        dlg.setResizable(true);
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
        return created[0];
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

    private static void addRow(JPanel form, int row, String label, Component c) {
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
}