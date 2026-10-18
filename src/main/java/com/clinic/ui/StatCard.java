package com.clinic.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;

/** Dashboard tile: small title, big value, optional sub-line. */
public class StatCard extends Card {
    private final JLabel value = new JLabel("-");
    private final JLabel sub = new JLabel(" ");

    public StatCard(String title, Icon icon) {
        super(null);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 14));

        JLabel t = new JLabel(title, icon, JLabel.LEFT);
        t.setIconTextGap(8);
        t.setForeground(Theme.MUTED);
        value.setFont(Theme.font(Font.BOLD, 26f));
        value.setForeground(Theme.ACCENT_DARK);
        sub.setForeground(Theme.MUTED);
        sub.setFont(Theme.font(Font.PLAIN, 12.5f));
        t.setAlignmentX(LEFT_ALIGNMENT);
        value.setAlignmentX(LEFT_ALIGNMENT);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        add(t);
        add(javax.swing.Box.createVerticalStrut(6));
        add(value);
        add(sub);
    }

    public void set(String v, String s) {
        value.setText(v);
        sub.setText(s == null || s.isEmpty() ? " " : s);
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.ACCENT);
        g2.fillRoundRect(0, 12, 5, getHeight() - 24, 5, 5);
        g2.dispose();
    }
}
