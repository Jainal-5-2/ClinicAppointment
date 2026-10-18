package com.clinic.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;

/** Rounded button with a smooth hover color fade. */
public class FadeButton extends JButton {
    public enum Kind { PRIMARY, SECONDARY, DANGER, GHOST_LIGHT }

    private final Kind kind;
    private final HoverAnim hover;

    public FadeButton(String text, Icon icon, Kind kind) {
        super(text, icon);
        this.kind = kind;
        this.hover = new HoverAnim(this);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        setIconTextGap(8);
        setFont(getFont().deriveFont(Font.BOLD));
    }

    public static FadeButton primary(String text) { return new FadeButton(text, null, Kind.PRIMARY); }
    public static FadeButton primary(String text, Icon icon) { return new FadeButton(text, icon, Kind.PRIMARY); }
    public static FadeButton secondary(String text) { return new FadeButton(text, null, Kind.SECONDARY); }
    public static FadeButton danger(String text) { return new FadeButton(text, null, Kind.DANGER); }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float t = hover.value();
        Color bg;
        Color fg;
        if (kind == Kind.PRIMARY) {
            bg = Theme.lerp(Theme.ACCENT, Theme.ACCENT_DARK, t);
            fg = Color.WHITE;
        } else if (kind == Kind.DANGER) {
            bg = Theme.lerp(Theme.DANGER, Theme.DANGER_DARK, t);
            fg = Color.WHITE;
        } else if (kind == Kind.GHOST_LIGHT) {
            bg = new Color(255, 255, 255, Math.round(30 + 60 * t));
            fg = Color.WHITE;
        } else {
            bg = Theme.lerp(Color.WHITE, Theme.ACCENT_LIGHT, t);
            fg = Theme.ACCENT_DARK;
        }
        if (!isEnabled()) {
            bg = new Color(0xE5E7EB);
            fg = new Color(0x9CA3AF);
        } else if (getModel().isPressed()) {
            bg = bg.darker();
        }
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        if (kind == Kind.SECONDARY) {
            g2.setColor(isEnabled() ? Theme.lerp(Theme.BORDER, Theme.ACCENT, t) : new Color(0xD1D5DB));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
        }
        g2.dispose();
        if (!fg.equals(getForeground())) setForeground(fg);
        super.paintComponent(g);
    }
}
