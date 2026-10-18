package com.clinic.ui;

import com.clinic.dao.UserDao;
import com.clinic.model.User;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Admin's own account page: edit username/name/phone/email, and change password. Role is read-only. */
public class AdminAccountPanel extends JPanel implements Refreshable {
    private User user;
    private final JTextField username = new JTextField(24);
    private final JTextField fullName = new JTextField(24);
    private final JTextField phone = new JTextField(24);
    private final JTextField email = new JTextField(24);

    public AdminAccountPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel heading = new JLabel("My account");
        heading.setFont(Theme.font(Font.BOLD, 20f));

        JPanel form = new JPanel(new GridBagLayout());
        int row = 0;
        addReadOnly(form, row++, "Role", "Administrator");
        addField(form, row++, "Username", username);
        addField(form, row++, "Full name", fullName);
        addField(form, row++, "Phone", phone);
        addField(form, row++, "Email", email);

        FadeButton save = FadeButton.primary("Save changes");
        FadeButton changePw = FadeButton.secondary("Change password");
        save.addActionListener(e -> save());
        changePw.addActionListener(e ->
                ChangePasswordDialog.forSelf(
                        javax.swing.SwingUtilities.getWindowAncestor(this),
                        user.id()));

        JPanel actions = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)));
        actions.add(save);
        actions.add(changePw);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.add(form, BorderLayout.NORTH);
        content.add(actions, BorderLayout.CENTER);

        add(heading, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        reload();
    }

    private static void addReadOnly(JPanel form, int row, String label, String value) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = row;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(6, 0, 6, 16);
        JLabel l = new JLabel(label);
        l.setForeground(Theme.MUTED);
        form.add(l, g);

        g = new GridBagConstraints();
        g.gridx = 1;
        g.gridy = row;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        form.add(new JLabel(value), g);
    }

    private static void addField(JPanel form, int row, String label, JTextField field) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = row;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(6, 0, 6, 16);
        JLabel l = new JLabel(label);
        l.setForeground(Theme.MUTED);
        form.add(l, g);

        g = new GridBagConstraints();
        g.gridx = 1;
        g.gridy = row;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, g);
    }

    @Override public void reload() {
        username.setText(user.username());
        fullName.setText(user.fullName());
        phone.setText(nz(user.phone()));
        email.setText(nz(user.email()));
    }

    private void save() {
        String uname = username.getText().trim();
        String name = fullName.getText().trim();
        if (uname.length() < 3) {
            UiUtil.error(this, "Username must be at least 3 characters.");
            return;
        }
        if (name.isEmpty()) {
            UiUtil.error(this, "Full name is required.");
            return;
        }
        try {
            // If the username was changed, make sure the new one isn't taken.
            if (!uname.equals(user.username()) && UserDao.usernameExists(uname)) {
                UiUtil.error(this, "That username is already taken.");
                return;
            }
            UserDao.updateProfile(user.id(), uname, name, phone.getText(), email.getText());
            User fresh = UserDao.findById(user.id());
            AppFrame frame = AppFrame.enclosing(this);
            if (frame != null && fresh != null) frame.refreshHeader(fresh);
            this.user = fresh != null ? fresh : user;
            reload();
            UiUtil.info(this, "Account updated.");
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }
}