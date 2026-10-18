package com.clinic;

import com.clinic.dao.UserDao;
import com.clinic.ui.LoginFrame;
import com.clinic.ui.Theme;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Font;
import java.sql.SQLException;
import java.util.Collections;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        try {
            boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
            UIManager.put("defaultFont", new Font(windows ? "Segoe UI" : Font.SANS_SERIF, Font.PLAIN, 14));
            FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#0D9488"));
            FlatLightLaf.setup();
            UIManager.put("Component.arc", 10);
            UIManager.put("Button.arc", 12);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.trackArc", 999);
            UIManager.put("Table.alternateRowColor", Theme.ROW_ALT);
        } catch (Exception ignored) { }

        SwingUtilities.invokeLater(() -> {
            try {
                // First run: create the default administrator account.
                if (UserDao.countByRole("ADMIN") == 0) {
                    UserDao.create("admin", "admin123", "ADMIN", "Administrator", null, null);
                }
                new LoginFrame().setVisible(true);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null,
                        "Cannot connect to the database.\n\n" + ex.getMessage() +
                        "\n\nCheck that MySQL is running, database/schema.sql was executed,\n" +
                        "and src/main/resources/db.properties has the right user/password.",
                        "Database error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
