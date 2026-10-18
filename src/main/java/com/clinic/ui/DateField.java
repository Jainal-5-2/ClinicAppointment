package com.clinic.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/** Date box that opens a month-calendar popup when clicked. Supports an empty state. */
public class DateField extends JPanel {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH);

    private LocalDate date = LocalDate.now();
    private boolean hasValue = true;
    private LocalDate minDate;
    private final JTextField text = new JTextField(12);
    private final List<Runnable> listeners = new ArrayList<>();

    public DateField() {
        super(new BorderLayout());
        setOpaque(false);
        text.setEditable(false);
        text.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        text.putClientProperty("JTextField.trailingIcon", Icons.calendar(16, Theme.ACCENT));
        text.putClientProperty("JTextField.placeholderText", "Pick a date");
        refreshText();
        text.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { showPopup(); }
        });
        add(text, BorderLayout.CENTER);
    }

    public LocalDate getDate() { return hasValue ? date : null; }

    public void setMinDate(LocalDate min) {
        this.minDate = min;
        if (min != null && hasValue && date.isBefore(min)) setDate(min);
    }

    public void setDate(LocalDate d) {
        if (d == null) { clear(); return; }
        boolean changed = !hasValue || !d.equals(date);
        date = d;
        hasValue = true;
        refreshText();
        if (changed) fireChanged();
    }

    public void clear() {
        boolean changed = hasValue;
        hasValue = false;
        refreshText();
        if (changed) fireChanged();
    }

    public void addChangeListener(Runnable r) { listeners.add(r); }

    private void refreshText() {
        text.setText(hasValue ? FMT.format(date) : "");
    }

    private void fireChanged() {
        for (Runnable r : new ArrayList<>(listeners)) r.run();
    }

    private void showPopup() {
        final YearMonth[] ym = {YearMonth.from(hasValue ? date : LocalDate.now())};
        final JPopupMenu pop = new JPopupMenu();
        JPanel root = new JPanel(new BorderLayout(6, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        final JLabel title = new JLabel("", SwingConstants.CENTER);
        title.setFont(Theme.font(Font.BOLD, 15f));
        final JPanel grid = new JPanel(new GridLayout(0, 7, 2, 2));
        grid.setOpaque(false);

        final Runnable render = () -> {
            grid.removeAll();
            title.setText(ym[0].getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + ym[0].getYear());
            for (int i = 1; i <= 7; i++) {
                String n = DayOfWeek.of(i).getDisplayName(TextStyle.SHORT, Locale.ENGLISH).substring(0, 2);
                JLabel l = new JLabel(n, SwingConstants.CENTER);
                l.setForeground(Theme.MUTED);
                grid.add(l);
            }
            LocalDate first = ym[0].atDay(1);
            LocalDate start = first.minusDays(first.getDayOfWeek().getValue() - 1);
            LocalDate today = LocalDate.now();
            for (int i = 0; i < 42; i++) {
                final LocalDate d = start.plusDays(i);
                boolean muted = !YearMonth.from(d).equals(ym[0]);
                boolean selected = hasValue && d.equals(date);
                DayButton b = new DayButton(String.valueOf(d.getDayOfMonth()), selected, d.equals(today), muted);
                boolean blocked = minDate != null && d.isBefore(minDate);
                b.setEnabled(!blocked);
                b.addActionListener(e -> {
                    setDate(d);
                    pop.setVisible(false);
                });
                grid.add(b);
            }
            grid.revalidate();
            grid.repaint();
        };

        FadeButton prev = FadeButton.secondary("<");
        FadeButton next = FadeButton.secondary(">");
        prev.addActionListener(e -> { ym[0] = ym[0].minusMonths(1); render.run(); });
        next.addActionListener(e -> { ym[0] = ym[0].plusMonths(1); render.run(); });
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(prev, BorderLayout.WEST);
        head.add(title, BorderLayout.CENTER);
        head.add(next, BorderLayout.EAST);

        FadeButton todayBtn = FadeButton.secondary("Today");
        todayBtn.addActionListener(e -> {
            LocalDate t = LocalDate.now();
            if (minDate == null || !t.isBefore(minDate)) {
                setDate(t);
                pop.setVisible(false);
            }
        });
        FadeButton clearBtn = FadeButton.secondary("Clear");
        clearBtn.addActionListener(e -> {
            clear();
            pop.setVisible(false);
        });
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        foot.setOpaque(false);
        foot.add(todayBtn);
        foot.add(clearBtn);

        render.run();
        root.add(head, BorderLayout.NORTH);
        root.add(grid, BorderLayout.CENTER);
        root.add(foot, BorderLayout.SOUTH);
        pop.add(root);
        pop.show(text, 0, text.getHeight() + 2);
    }

    /** One day cell in the calendar popup. */
    private static class DayButton extends JButton {
        private final HoverAnim hover;
        private final boolean selected;
        private final boolean today;

        DayButton(String label, boolean selected, boolean today, boolean muted) {
            super(label);
            this.selected = selected;
            this.today = today;
            this.hover = new HoverAnim(this);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setForeground(selected ? Color.WHITE : (muted ? new Color(0x9CA3AF) : Theme.TEXT));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (selected) {
                g2.setColor(Theme.ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            } else if (isEnabled()) {
                g2.setColor(Theme.lerp(new Color(224, 245, 242, 0), Theme.ACCENT_LIGHT, hover.value()));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
            if (today && !selected) {
                g2.setColor(Theme.ACCENT);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}