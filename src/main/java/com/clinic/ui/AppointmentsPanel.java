package com.clinic.ui;

import com.clinic.dao.AppointmentDao;
import com.clinic.model.Appointment;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

/** Appointment list for admin (all), doctor (own) or patient (own): search, filters, bulk approve/cancel/complete. */
public class AppointmentsPanel extends JPanel implements Refreshable {
    public enum Mode { ADMIN, DOCTOR, PATIENT }

    // 0 = checkbox, 1 = ID, 2 = Date, 3 = Time, 4 = Doctor, 5 = Patient, 6 = Status, 7 = Notes
    private static final int COL_SELECT = 0;
    private static final int COL_ID     = 1;
    private static final int COL_STATUS = 6;

    private final Mode mode;
    private final int ownerId;

    private final DefaultTableModel model =
            new DefaultTableModel(new Object[]{"", "ID", "Date", "Time", "Doctor", "Patient", "Status", "Notes"}, 0) {
                @Override public boolean isCellEditable(int row, int col) { return col == COL_SELECT; }
                @Override public Class<?> getColumnClass(int col) {
                    if (col == COL_SELECT) return Boolean.class;
                    if (col == COL_ID) return Integer.class;
                    return Object.class;
                }
            };
    private final JTable table = Tables.create(model);
    private final TableRowSorter<DefaultTableModel> sorter = Tables.sortable(table, model);
    private final JTextField search = new JTextField(18);
    private final JComboBox<String> statusBox =
            new JComboBox<>(new String[]{"All statuses", "Pending", "Approved", "Completed", "Cancelled"});
    private final JCheckBox dateFilter = new JCheckBox("Only on");
    private final DateField dateField = new DateField();
    private final JLabel count = new JLabel(" ");
    private List<Appointment> rows = new ArrayList<>();
    private final Set<Integer> selectedIds = new HashSet<>();
    private boolean syncing;
    private boolean selectAll;

    public AppointmentsPanel(Mode mode, int ownerId) {
        this.mode = mode;
        this.ownerId = ownerId;
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        table.getColumnModel().getColumn(COL_SELECT).setMaxWidth(36);
        table.getColumnModel().getColumn(COL_ID).setMaxWidth(60);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(130);
        table.getColumnModel().getColumn(COL_STATUS).setCellRenderer(new Tables.StatusRenderer());

        // --- select-all header for column 0 ---
        installSelectAllHeader();

        // Model listener: keep selectedIds in sync with checkbox clicks.
        model.addTableModelListener(e -> {
            if (syncing) return;
            if (e.getColumn() == COL_SELECT || e.getColumn() == javax.swing.event.TableModelEvent.ALL_COLUMNS) {
                for (int row = e.getFirstRow(); row <= e.getLastRow() && row < model.getRowCount(); row++) {
                    Object cell = model.getValueAt(row, COL_SELECT);
                    int id = (Integer) model.getValueAt(row, COL_ID);
                    if (Boolean.TRUE.equals(cell)) selectedIds.add(id);
                    else selectedIds.remove(id);
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openDetails();
            }
        });

        search.putClientProperty("JTextField.placeholderText", "Search name, doctor, notes...");
        search.putClientProperty("JTextField.leadingIcon", Icons.search(16, Theme.MUTED));
        search.putClientProperty("JTextField.showClearButton", true);
        Tables.onChange(search, this::applyFilter);
        statusBox.addActionListener(e -> applyFilter());

        JPanel top = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)));
        top.add(search);
        top.add(statusBox);
        if (mode != Mode.PATIENT) {
            dateFilter.setOpaque(false);
            dateFilter.addActionListener(e -> load(false));
            dateField.addChangeListener(() -> {
                dateFilter.setSelected(true);
                load(false);
            });
            top.add(dateFilter);
            top.add(dateField);
        }
        count.setForeground(Theme.MUTED);
        top.add(count);

        FadeButton refresh = FadeButton.secondary("Refresh");
        FadeButton details = FadeButton.secondary("View details");
        FadeButton cancel = FadeButton.danger("Cancel");
        refresh.addActionListener(e -> load(false));
        details.addActionListener(e -> openDetails());
        cancel.addActionListener(e -> bulkChangeStatus("CANCELLED"));

        JPanel actions = Theme.clear(new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)));
        actions.add(refresh);
        actions.add(details);
        actions.add(cancel);
        if (mode != Mode.PATIENT) {
            FadeButton approve = FadeButton.primary("Approve");
            approve.addActionListener(e -> bulkChangeStatus("APPROVED"));
            FadeButton done = FadeButton.primary("Mark completed");
            done.addActionListener(e -> bulkChangeStatus("COMPLETED"));
            actions.add(approve);
            actions.add(done);
        }

        add(top, BorderLayout.NORTH);
        add(Tables.scroll(table), BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
        load(false);

        Timer timer = new Timer(20000, e -> {
            if (!isDisplayable()) ((Timer) e.getSource()).stop();
            else if (isShowing()) load(true);
        });
        timer.start();
    }

    // --- select-all header -------------------------------------------------

    private void installSelectAllHeader() {
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);

        // Renderer that paints a checkbox in column 0 and the normal header text elsewhere.
        TableCellRenderer defaultRenderer = header.getDefaultRenderer();
        header.setDefaultRenderer((tbl, value, selected, focus, row, column) -> {
            if (column == COL_SELECT) {
                JCheckBox box = new JCheckBox();
                box.setOpaque(false);
                box.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                box.setSelected(selectAll);
                return box;
            }
            return defaultRenderer.getTableCellRendererComponent(tbl, value, selected, focus, row, column);
        });

        header.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                if (col < 0) return;
                int modelCol = table.convertColumnIndexToModel(col);
                if (modelCol != COL_SELECT) return;
                setSelectAll(!selectAll);
                table.repaint();
            }
        });
    }

    /** Selects or deselects every visible row and updates the header checkbox. */
    private void setSelectAll(boolean on) {
        selectAll = on;
        syncing = true;
        for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            model.setValueAt(on, modelRow, COL_SELECT);
            int id = (Integer) model.getValueAt(modelRow, COL_ID);
            if (on) selectedIds.add(id); else selectedIds.remove(id);
        }
        syncing = false;
        if (!on) selectedIds.clear();   // also drop any selections that are filtered out
    }

    // --- loading / filtering ----------------------------------------------

    @Override public void reload() {
        load(false);
    }

    private void applyFilter() {
        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        String q = search.getText().trim();
        if (!q.isEmpty()) filters.add(Tables.contains(q));
        if (statusBox.getSelectedIndex() > 0) {
            String st = statusBox.getSelectedItem().toString().toUpperCase();
            filters.add(RowFilter.regexFilter("^" + st + "$", COL_STATUS));
        }
        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
        count.setText(table.getRowCount() + " of " + model.getRowCount() + " shown");
    }

    private int selectedId() {
        int r = table.getSelectedRow();
        if (r < 0) return -1;
        return (Integer) model.getValueAt(table.convertRowIndexToModel(r), COL_ID);
    }

    private Appointment selected() {
        int id = selectedId();
        if (id < 0) return null;
        for (Appointment a : rows) if (a.id() == id) return a;
        return null;
    }

    private List<Appointment> selectedAppointments() {
        List<Appointment> out = new ArrayList<>();
        for (Appointment a : rows) if (selectedIds.contains(a.id())) out.add(a);
        return out;
    }

    private void load(boolean quiet) {
        int keep = selectedId();
        try {
            LocalDate d = (mode != Mode.PATIENT && dateFilter.isSelected()) ? dateField.getDate() : null;
            if (mode == Mode.ADMIN) rows = AppointmentDao.listAll(d);
            else if (mode == Mode.DOCTOR) rows = AppointmentDao.listForDoctor(ownerId, d);
            else rows = AppointmentDao.listForPatient(ownerId);

            syncing = true;
            model.setRowCount(0);
            for (Appointment a : rows) {
                model.addRow(new Object[]{
                        selectedIds.contains(a.id()),
                        a.id(), a.date(), a.start() + " - " + a.end(),
                        "Dr. " + a.doctorName(), a.patientName(), a.status(),
                        a.notes() == null ? "" : a.notes()});
            }
            Set<Integer> current = new HashSet<>();
            for (Appointment a : rows) current.add(a.id());
            selectedIds.retainAll(current);
            syncing = false;

            applyFilter();
            for (int i = 0; i < model.getRowCount(); i++) {
                if ((Integer) model.getValueAt(i, COL_ID) == keep) {
                    int v = table.convertRowIndexToView(i);
                    if (v >= 0) table.setRowSelectionInterval(v, v);
                }
            }
        } catch (SQLException ex) {
            if (!quiet) UiUtil.dbError(this, ex);
        }
    }

    private void openDetails() {
        Appointment a = selected();
        if (a == null) {
            UiUtil.error(this, "Select an appointment first.");
            return;
        }
        AppointmentDetailDialog.Actions allowed = (mode == Mode.PATIENT)
                ? AppointmentDetailDialog.Actions.PATIENT
                : AppointmentDetailDialog.Actions.STAFF;
        AppointmentDetailDialog.show(javax.swing.SwingUtilities.getWindowAncestor(this), a, allowed);
        load(false);
    }

    // --- bulk actions ------------------------------------------------------

    private void bulkChangeStatus(String newStatus) {
        List<Appointment> targets = selectedAppointments();
        if (targets.isEmpty()) {
            Appointment a = selected();
            if (a == null) {
                UiUtil.error(this, "Select at least one appointment.");
                return;
            }
            targets.add(a);
        }

        List<Appointment> eligible = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (Appointment a : targets) {
            String reason = ineligibleReason(a, newStatus);
            if (reason == null) eligible.add(a);
            else skipped.add(a.id() + ": " + reason);
        }

        if (eligible.isEmpty()) {
            UiUtil.error(this, "None of the selected appointments can be " + describe(newStatus) + ".\n"
                    + String.join("\n", skipped));
            return;
        }

        String verb = describe(newStatus);
        String message = "Apply \"" + verb + "\" to " + eligible.size() + " appointment(s)?";
        if (!skipped.isEmpty()) {
            message += "\n\n" + skipped.size() + " will be skipped:\n" + String.join("\n", skipped);
        }
        if (!UiUtil.confirm(this, message)) return;

        int done = 0;
        int failed = 0;
        try {
            for (Appointment a : eligible) {
                boolean ok = switch (newStatus) {
                    case "APPROVED"  -> AppointmentDao.approve(a.id());
                    case "COMPLETED" -> AppointmentDao.complete(a.id());
                    default          -> AppointmentDao.cancel(a.id());
                };
                if (ok) done++; else failed++;
            }
            if (failed > 0) {
                UiUtil.error(this, done + " updated, " + failed
                        + " had already been changed by someone else.");
            }
            selectedIds.clear();
            selectAll = false;
            load(false);
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    private static String ineligibleReason(Appointment a, String target) {
        switch (target) {
            case "APPROVED":
                return a.isPending() ? null : "only pending can be approved";
            case "COMPLETED":
                return a.isApproved() ? null : "only approved can be completed";
            case "CANCELLED":
                if (!(a.isPending() || a.isApproved())) return "only pending/approved can be cancelled";
                if (LocalDateTime.of(a.date(), a.start()).isBefore(LocalDateTime.now())) {
                    return "already started or in the past";
                }
                return null;
            default:
                return "unknown target status";
        }
    }

    private static String describe(String status) {
        return switch (status) {
            case "APPROVED"  -> "approve";
            case "COMPLETED" -> "mark completed";
            default          -> "cancel";
        };
    }
}