package com.clinic.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JToggleButton;

/** Selectable time-slot chip with a hover fade. */
public class ChipButton extends JToggleButton {
    private final HoverAnim hover;

    public ChipButton(String text) {
        super(text);
        hover = new HoverAnim(this);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float t = hover.value();
        Color bg;
        Color line;
        Color fg;
        if (isSelected()) {
            bg = Theme.ACCENT;
            line = Theme.ACCENT;
            fg = Color.WHITE;
        } else {
            bg = Theme.lerp(Color.WHITE, Theme.ACCENT_LIGHT, t);
            line = Theme.lerp(Theme.BORDER, Theme.ACCENT, t);
            fg = Theme.TEXT;
        }
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.setColor(line);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
        g2.dispose();
        if (!fg.equals(getForeground())) setForeground(fg);
        super.paintComponent(g);
    }
}
