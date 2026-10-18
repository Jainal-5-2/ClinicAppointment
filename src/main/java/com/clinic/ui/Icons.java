package com.clinic.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

/** Small vector icons drawn in code (no image files needed). */
public final class Icons {
    private Icons() { }

    private abstract static class Vec implements Icon {
        final int size;
        final Color color;

        Vec(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(size / 10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            draw(g2, size);
            g2.dispose();
        }

        abstract void draw(Graphics2D g, float s);
    }

    public static Icon calendar(int size, Color c) {
        return new Vec(size, c) {
            @Override void draw(Graphics2D g, float s) {
                g.draw(new RoundRectangle2D.Float(s * 0.12f, s * 0.2f, s * 0.76f, s * 0.68f, s * 0.16f, s * 0.16f));
                g.draw(new Line2D.Float(s * 0.12f, s * 0.42f, s * 0.88f, s * 0.42f));
                g.draw(new Line2D.Float(s * 0.32f, s * 0.08f, s * 0.32f, s * 0.28f));
                g.draw(new Line2D.Float(s * 0.68f, s * 0.08f, s * 0.68f, s * 0.28f));
            }
        };
    }

    public static Icon plus(int size, Color c) {
        return new Vec(size, c) {
            @Override void draw(Graphics2D g, float s) {
                g.draw(new Line2D.Float(s * 0.5f, s * 0.18f, s * 0.5f, s * 0.82f));
                g.draw(new Line2D.Float(s * 0.18f, s * 0.5f, s * 0.82f, s * 0.5f));
            }
        };
    }

    public static Icon user(int size, Color c) {
        return new Vec(size, c) {
            @Override void draw(Graphics2D g, float s) {
                g.draw(new Ellipse2D.Float(s * 0.32f, s * 0.12f, s * 0.36f, s * 0.36f));
                g.draw(new Arc2D.Float(s * 0.16f, s * 0.56f, s * 0.68f, s * 0.7f, 0, 180, Arc2D.OPEN));
            }
        };
    }

    /** Medical cross. */
    public static Icon cross(int size, Color c) {
        return new Vec(size, c) {
            @Override void draw(Graphics2D g, float s) {
                g.setStroke(new BasicStroke(s * 0.28f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Float(s * 0.5f, s * 0.2f, s * 0.5f, s * 0.8f));
                g.draw(new Line2D.Float(s * 0.2f, s * 0.5f, s * 0.8f, s * 0.5f));
            }
        };
    }

    public static Icon clock(int size, Color c) {
        return new Vec(size, c) {
            @Override void draw(Graphics2D g, float s) {
                g.draw(new Ellipse2D.Float(s * 0.1f, s * 0.1f, s * 0.8f, s * 0.8f));
                g.draw(new Line2D.Float(s * 0.5f, s * 0.5f, s * 0.5f, s * 0.28f));
                g.draw(new Line2D.Float(s * 0.5f, s * 0.5f, s * 0.68f, s * 0.5f));
            }
        };
    }

    /** Four rounded squares (dashboard). */
    public static Icon grid(int size, Color c) {
        return new Vec(size, c) {
            @Override void draw(Graphics2D g, float s) {
                float a = s * 0.14f, b = s * 0.54f, w = s * 0.32f, r = s * 0.1f;
                g.draw(new RoundRectangle2D.Float(a, a, w, w, r, r));
                g.draw(new RoundRectangle2D.Float(b, a, w, w, r, r));
                g.draw(new RoundRectangle2D.Float(a, b, w, w, r, r));
                g.draw(new RoundRectangle2D.Float(b, b, w, w, r, r));
            }
        };
    }

    public static Icon search(int size, Color c) {
        return new Vec(size, c) {
            @Override void draw(Graphics2D g, float s) {
                g.draw(new Ellipse2D.Float(s * 0.14f, s * 0.14f, s * 0.5f, s * 0.5f));
                g.draw(new Line2D.Float(s * 0.58f, s * 0.58f, s * 0.86f, s * 0.86f));
            }
        };
    }
}
