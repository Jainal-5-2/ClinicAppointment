package com.clinic.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.UIManager;

/** Colors and small helpers shared by all screens (medical teal palette). */
public final class Theme {
    private Theme() { }

    public static final Color ACCENT = new Color(0x0D9488);
    public static final Color ACCENT_DARK = new Color(0x0F766E);
    public static final Color ACCENT_LIGHT = new Color(0xE0F5F2);
    public static final Color DANGER = new Color(0xDC2626);
    public static final Color DANGER_DARK = new Color(0xB91C1C);
    public static final Color BORDER = new Color(0xD5DDDB);
    public static final Color MUTED = new Color(0x6B7280);
    public static final Color TEXT = new Color(0x1F2937);
    public static final Color ROW_ALT = new Color(0xF6FAF9);

    public static Color lerp(Color a, Color b, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        return new Color(mix(a.getRed(), b.getRed(), k), mix(a.getGreen(), b.getGreen(), k),
                mix(a.getBlue(), b.getBlue(), k), mix(a.getAlpha(), b.getAlpha(), k));
    }

    private static int mix(int x, int y, float t) {
        return Math.round(x + (y - x) * t);
    }

    public static Font font(int style, float size) {
        Font base = UIManager.getFont("Label.font");
        if (base == null) base = new JLabel().getFont();
        return base.deriveFont(style, size);
    }

    /** Makes a panel/label transparent so it blends into a Card or the page. */
    public static <T extends JComponent> T clear(T c) {
        c.setOpaque(false);
        return c;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        return l;
    }
}
