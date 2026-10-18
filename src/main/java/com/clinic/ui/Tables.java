package com.clinic.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;

/** Shared table styling, search filter and the colored status badge renderer. */
public final class Tables {
    private Tables() { }

    public static JTable create(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(36);
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setGridColor(new Color(0xE8EFED));
        t.setFillsViewportHeight(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setSelectionBackground(Theme.ACCENT_LIGHT);
        t.setSelectionForeground(Theme.TEXT);
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setFont(Theme.font(Font.BOLD, 13f));
        h.setPreferredSize(new Dimension(0, 36));
        return t;
    }

    public static TableRowSorter<DefaultTableModel> sortable(JTable t, DefaultTableModel m) {
        TableRowSorter<DefaultTableModel> s = new TableRowSorter<>(m);
        t.setRowSorter(s);
        return s;
    }

    public static JScrollPane scroll(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    /** Case-insensitive "contains" filter across all columns. */
    public static RowFilter<Object, Object> contains(String text) {
        return RowFilter.regexFilter("(?i)" + Pattern.quote(text));
    }

    public static void onChange(JTextComponent c, Runnable r) {
        c.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { r.run(); }
            @Override public void removeUpdate(DocumentEvent e) { r.run(); }
            @Override public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }

    /** Draws BOOKED / COMPLETED / CANCELLED as a colored pill. */
    public static class StatusRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean selected, boolean focus,
                                                       int row, int col) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
            p.setOpaque(true);
            p.setBackground(selected ? t.getSelectionBackground() : (row % 2 == 1 ? Theme.ROW_ALT : Color.WHITE));
            p.add(new Badge(String.valueOf(v)));
            return p;
        }
    }

    private static class Badge extends JLabel {
        private final Color fill;

        Badge(String status) {
            super(status.isEmpty() ? "" : status.charAt(0) + status.substring(1).toLowerCase());
            Color fg;
            if ("BOOKED".equals(status)) {
                fg = Theme.ACCENT_DARK;
                fill = new Color(0xCCFBF1);
            } else if ("COMPLETED".equals(status)) {
                fg = new Color(0x1D4ED8);
                fill = new Color(0xDBEAFE);
            } else {
                fg = Theme.DANGER_DARK;
                fill = new Color(0xFEE2E2);
            }
            setForeground(fg);
            setFont(Theme.font(Font.BOLD, 12f));
            setBorder(BorderFactory.createEmptyBorder(3, 12, 3, 12));
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
