package com.clinic.ui;

import com.clinic.dao.AppointmentDao;
import com.clinic.model.Appointment;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Read-only view of a single appointment, with role-appropriate actions. */
public final class AppointmentDetailDialog extends JDialog {
    public enum Actions { NONE, PATIENT, STAFF }

    private final Appointment appointment;
    private boolean acted;

    private AppointmentDetailDialog(java.awt.Window parent, Appointment a, Actions actions) {
        super(parent, "Appointment details", ModalityType.APPLICATION_MODAL);
        this.appointment = a;

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel heading = new JLabel("Appointment #" + a.id());
        heading.setFont(Theme.font(Font.BOLD, 18f));
        root.add(heading, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridBagLayout());
        int row = 0;
        addRow(fields, row++, "Status", a.status());
        addRow(fields, row++, "Date", a.date().toString());
        addRow(fields, row++, "Time", a.start() + " - " + a.end());
        addRow(fields, row++, "Doctor", "Dr. " + a.doctorName());
        addRow(fields, row++, "Patient", a.patientName());

        JTextArea notes = new JTextArea(a.notes() == null ? "" : a.notes());
        notes.setEditable(false);
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        notes.setOpaque(false);
        notes.setRows(3);
        notes.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        JScrollPane notesScroll = new JScrollPane(notes);
        notesScroll.setBorder(BorderFactory.createTitledBorder("Reason / notes"));
        notesScroll.setPreferredSize(new java.awt.Dimension(360, 100));

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.add(fields, BorderLayout.NORTH);
        center.add(notesScroll, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        JPanel buttons = Theme.clear(new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)));
        FadeButton close = FadeButton.secondary("Close");
        close.addActionListener(e -> dispose());
        buttons.add(close);

        if (actions != Actions.NONE) {
            boolean canCancel = a.isPending() || a.isApproved();
            boolean canApprove = a.isPending() && actions == Actions.STAFF;
            boolean canComplete = a.isApproved() && actions == Actions.STAFF;

            if (canCancel) {
                FadeButton cancel = FadeButton.danger("Cancel appointment");
                cancel.addActionListener(e -> doAction("CANCEL"));
                buttons.add(cancel);
            }
            if (canApprove) {
                FadeButton approve = FadeButton.primary("Approve");
                approve.addActionListener(e -> doAction("APPROVE"));
                buttons.add(approve);
            }
            if (canComplete) {
                FadeButton done = FadeButton.primary("Mark completed");
                done.addActionListener(e -> doAction("COMPLETE"));
                buttons.add(done);
            }
        }

        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        setSize(440, 400);
        setLocationRelativeTo(parent);
    }

    private void addRow(JPanel form, int row, String label, String value) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = row;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(4, 0, 4, 14);
        JLabel l = new JLabel(label);
        l.setForeground(Theme.MUTED);
        form.add(l, g);

        g = new GridBagConstraints();
        g.gridx = 1;
        g.gridy = row;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        form.add(new JLabel(value), g);
    }

    private void doAction(String action) {
        String verb = switch (action) {
            case "APPROVE"  -> "Approve";
            case "COMPLETE" -> "Mark as completed:";
            default         -> "Cancel";
        };
        String tail = switch (action) {
            case "APPROVE", "COMPLETE" -> " this appointment?";
            default -> " " + appointment.date() + " " + appointment.start()
                    + " (" + appointment.patientName() + ")?";
        };
        if (!UiUtil.confirm(this, verb + tail)) return;

        try {
            boolean ok = switch (action) {
                case "APPROVE"  -> AppointmentDao.approve(appointment.id());
                case "COMPLETE" -> AppointmentDao.complete(appointment.id());
                default         -> AppointmentDao.cancel(appointment.id());
            };
            if (!ok) {
                UiUtil.error(this, "The appointment was already updated by someone else.");
            } else {
                acted = true;
            }
            dispose();
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    public static boolean show(java.awt.Window parent, Appointment a, Actions actions) {
        AppointmentDetailDialog dlg = new AppointmentDetailDialog(parent, a, actions);
        dlg.setVisible(true);
        return dlg.acted;
    }
}