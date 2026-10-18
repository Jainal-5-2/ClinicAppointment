package com.clinic.ui;

import com.clinic.dao.UserDao;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

/** Change password for the signed-in user, or (admin) reset another user's password. */
public final class ChangePasswordDialog extends JDialog {

    private ChangePasswordDialog(java.awt.Window parent, int userId, String who, boolean requireOld) {
        super(parent, requireOld ? "Change password" : "Reset password",
                ModalityType.APPLICATION_MODAL);

        JPasswordField oldPw = new JPasswordField(20);
        JPasswordField newPw = new JPasswordField(20);
        JPasswordField confirmPw = new JPasswordField(20);

        JLabel heading = new JLabel(requireOld ? "Change your password" : "Reset password for " + who);
        heading.setFont(Theme.font(Font.BOLD, 18f));

        JPanel fields = new JPanel(new GridLayout(0, 2, 6, 6));
        if (requireOld) {
            fields.add(new JLabel("Current password:"));
            fields.add(oldPw);
        }
        fields.add(new JLabel("New password (min 6):"));
        fields.add(newPw);
        fields.add(new JLabel("Confirm new password:"));
        fields.add(confirmPw);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        root.add(heading, BorderLayout.NORTH);
        root.add(fields, BorderLayout.CENTER);

        FadeButton save = FadeButton.primary("Save");
        FadeButton cancel = FadeButton.secondary("Cancel");
        JPanel buttons = Theme.clear(new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)));
        buttons.add(cancel);
        buttons.add(save);
        root.add(buttons, BorderLayout.SOUTH);

        save.addActionListener(e -> {
            String oldVal = new String(oldPw.getPassword());
            String newVal = new String(newPw.getPassword());
            String confirmVal = new String(confirmPw.getPassword());

            if (newVal.length() < 6) {
                UiUtil.error(this, "New password must be at least 6 characters.");
                return;
            }
            if (!newVal.equals(confirmVal)) {
                UiUtil.error(this, "New password and confirmation do not match.");
                return;
            }
            if (requireOld) {
                if (oldVal.isEmpty()) {
                    UiUtil.error(this, "Enter your current password.");
                    return;
                }
            }

            try {
                if (requireOld && !UserDao.verifyPassword(userId, oldVal)) {
                    UiUtil.error(this, "Current password is incorrect.");
                    return;
                }
                UserDao.changePassword(userId, newVal);
                UiUtil.info(this, "Password updated.");
                dispose();
            } catch (SQLException ex) {
                UiUtil.dbError(this, ex);
            }
        });
        cancel.addActionListener(e -> dispose());

        setContentPane(root);
        setSize(420, requireOld ? 260 : 220);
        setLocationRelativeTo(parent);
    }

    /** Self-service: asks for the current password first. */
    public static void forSelf(java.awt.Window parent, int userId) {
        new ChangePasswordDialog(parent, userId, null, true).setVisible(true);
    }

    /** Admin override: no current-password check. */
    public static void forUser(java.awt.Window parent, int userId, String who) {
        new ChangePasswordDialog(parent, userId, who, false).setVisible(true);
    }
}