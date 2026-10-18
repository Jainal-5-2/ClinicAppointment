package com.clinic.ui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.Timer;

/** Tiny hover fade: value() goes smoothly 0 -> 1 on mouse enter and back on exit. */
final class HoverAnim {
    private float t;
    private int dir;
    private Timer timer;

    HoverAnim(JComponent c) {
        timer = new Timer(16, e -> {
            t += dir * 0.2f;
            if (t >= 1f) { t = 1f; timer.stop(); }
            if (t <= 0f) { t = 0f; timer.stop(); }
            c.repaint();
        });
        c.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { dir = 1; timer.start(); }
            @Override public void mouseExited(MouseEvent e) { dir = -1; timer.start(); }
        });
    }

    float value() { return t; }
}
