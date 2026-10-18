package com.clinic.ui;

import com.clinic.dao.DoctorDao;
import com.clinic.dao.UserDao;
import com.clinic.model.Doctor;
import com.clinic.model.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.sql.SQLException;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class LoginFrame extends JFrame {
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();

    public LoginFrame() {
        super("Clinic Appointment System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // left: branding panel
        JPanel left = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, Theme.ACCENT, 0, getHeight(), Theme.ACCENT_DARK));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setPreferredSize(new Dimension(320, 460));
        JLabel logo = new JLabel(Icons.cross(72, Color.WHITE));
        JLabel name = new JLabel("Clinic Appointments");
        name.setFont(Theme.font(Font.BOLD, 24f));
        name.setForeground(Color.WHITE);
        JLabel tag = new JLabel("<html><div style='text-align:center;width:220px'>"
                + "Book visits, manage schedules,<br>and never double-book again.</div></html>");
        tag.setForeground(new Color(0xCCFBF1));
        logo.setAlignmentX(CENTER_ALIGNMENT);
        name.setAlignmentX(CENTER_ALIGNMENT);
        tag.setAlignmentX(CENTER_ALIGNMENT);
        left.add(Box.createVerticalGlue());
        left.add(logo);
        left.add(Box.createVerticalStrut(18));
        left.add(name);
        left.add(Box.createVerticalStrut(8));
        left.add(tag);
        left.add(Box.createVerticalGlue());

        // right: sign-in form
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(BorderFactory.createEmptyBorder(36, 44, 36, 44));
        right.setPreferredSize(new Dimension(440, 460));

        JLabel heading = new JLabel("Welcome back");
        heading.setFont(Theme.font(Font.BOLD, 26f));
        JLabel sub = Theme.muted("Sign in to continue");
        JLabel userLbl = new JLabel("Username");
        JLabel passLbl = new JLabel("Password");
        username.putClientProperty("JTextField.placeholderText", "Enter your username");
        password.putClientProperty("JTextField.placeholderText", "Enter your password");
        password.putClientProperty("JPasswordField.showRevealButton", true);

        FadeButton login = FadeButton.primary("Sign in");
        FadeButton register = FadeButton.secondary("New patient? Create an account");
        login.addActionListener(e -> doLogin());
        register.addActionListener(e -> {
            if (AccountDialog.show(this, "PATIENT")) {
                UiUtil.info(this, "Account created. You can now sign in.");
            }
        });
        password.addActionListener(e -> doLogin());
        getRootPane().setDefaultButton(login);

        fixHeight(username, 38);
        fixHeight(password, 38);
        fixHeight(login, 42);
        fixHeight(register, 42);

        for (JComponent c : new JComponent[]{heading, sub, userLbl, username, passLbl, password, login, register}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
        }
        right.add(heading);
        right.add(sub);
        right.add(Box.createVerticalStrut(26));
        right.add(userLbl);
        right.add(Box.createVerticalStrut(4));
        right.add(username);
        right.add(Box.createVerticalStrut(14));
        right.add(passLbl);
        right.add(Box.createVerticalStrut(4));
        right.add(password);
        right.add(Box.createVerticalStrut(24));
        right.add(login);
        right.add(Box.createVerticalStrut(10));
        right.add(register);
        right.add(Box.createVerticalGlue());

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.CENTER);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private static void fixHeight(JComponent c, int h) {
        c.setPreferredSize(new Dimension(c.getPreferredSize().width, h));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
    }

    private void doLogin() {
        try {
            User user = UserDao.authenticate(username.getText(), new String(password.getPassword()));
            if (user == null) {
                UiUtil.error(this, "Invalid username or password.");
                return;
            }
            AppFrame next;
            switch (user.role()) {
                case "ADMIN" -> next = new AdminFrame(user);
                case "DOCTOR" -> {
                    Doctor d = DoctorDao.findByUserId(user.id());
                    if (d == null) {
                        UiUtil.error(this, "No doctor profile found for this account.");
                        return;
                    }
                    next = new DoctorFrame(user, d);
                }
                default -> next = new PatientFrame(user);
            }
            dispose();
            next.setVisible(true);
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }
}
