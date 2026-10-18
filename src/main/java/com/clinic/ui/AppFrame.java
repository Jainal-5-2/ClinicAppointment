package com.clinic.ui;

import com.clinic.model.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/** Base window: teal header with the signed-in user + logout, and a tab area. */
public abstract class AppFrame extends JFrame {
    protected User user;
    protected final JTabbedPane tabs = new JTabbedPane();
    private final JLabel who = new JLabel();

    protected AppFrame(User user, String title) {
        super(title);
        this.user = user;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1060, 680);
        setMinimumSize(new java.awt.Dimension(900, 600));
        setLocationRelativeTo(null);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.ACCENT_DARK);
        top.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));

        JLabel brand = new JLabel("Clinic Appointments", Icons.cross(22, Color.WHITE), SwingConstants.LEFT);
        brand.setIconTextGap(10);
        brand.setForeground(Color.WHITE);
        brand.setFont(Theme.font(Font.BOLD, 18f));

        who.setForeground(new Color(0xCCFBF1));
        refreshHeader(user);

        FadeButton logout = new FadeButton("Logout", null, FadeButton.Kind.GHOST_LIGHT);
        logout.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        JPanel right = Theme.clear(new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0)));
        right.add(who);
        right.add(logout);

        top.add(brand, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        tabs.putClientProperty("JTabbedPane.tabType", "underlined");
        tabs.putClientProperty("JTabbedPane.tabHeight", 44);
        tabs.putClientProperty("JTabbedPane.tabInsets", new Insets(4, 18, 4, 18));
        tabs.setFont(Theme.font(Font.PLAIN, 14f));
        tabs.addChangeListener(e -> {
            Component c = tabs.getSelectedComponent();
            if (c instanceof Refreshable r) r.reload();
        });

        add(top, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    protected void addTab(String title, Icon icon, JComponent content) {
        tabs.addTab(title, icon, content);
    }

    /** Updates the header label and the cached User record. */
    public void refreshHeader(User fresh) {
        if (fresh != null) this.user = fresh;
        who.setText(this.user.fullName() + "  |  " + roleLabel(this.user.role()));
    }

    /** Walks up from a component to the enclosing AppFrame, if any. */
    public static AppFrame enclosing(Component from) {
        return (AppFrame) SwingUtilities.getAncestorOfClass(AppFrame.class, from);
    }

    private static String roleLabel(String role) {
        switch (role) {
            case "ADMIN": return "Admin / Reception";
            case "DOCTOR": return "Doctor";
            default: return "Patient";
        }
    }
}