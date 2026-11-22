package carrs.app;

import carrs.model.*;
import carrs.repository.CarrsRepository;
import carrs.service.PaymentException;
import carrs.service.PaymentService;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Payment and Benefit Management tab (FTR-GOV-003). It follows the use case:
 * 1 search the constituent  2 enter the amount  3 pick the scheduled date  4 print the cheque on the day,
 * scan it and attach the scan  5 update the status  6 see every payment and its status  7 audit trail.
 * The system never pays anyone itself: every payment is a cheque the office prints, hands over and scans.
 */
public class PaymentTab implements FeatureTab {
    private static final Color DUE = new Color(255, 236, 179);
    private static final Color RED = new Color(170, 0, 0), GREEN = new Color(0, 110, 40);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CarrsRepository repo;
    private final PaymentService service;
    private final JPanel view = new JPanel(new BorderLayout());
    private final JTextField searchField = new JTextField(22);
    private final JButton searchButton = new JButton("Search");
    private final JLabel constituentLabel = new JLabel("No constituent selected");
    private final JLabel eligibility = new JLabel(" ");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Programme", "Benefit", "Amount", "Scheduled", "Paid on",
                         "Status", "Verified", "Payee", "Cheque scan"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private final List<JButton> needsConstituent = new ArrayList<>();
    private final JButton summaryButton = new JButton("Payment summary");
    private StaffMember user;
    private Applicant current;
    private List<Payment> shown = List.of();

    public PaymentTab(CarrsRepository repo) {
        this.repo = repo;
        this.service = new PaymentService(repo);

        // ---- search bar (step 1)
        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        search.add(new JLabel("Search constituent (name or ID):"));
        search.add(searchField);
        search.add(searchButton);
        searchButton.addActionListener(e -> onSearch());
        searchField.addActionListener(e -> onSearch());

        constituentLabel.setFont(constituentLabel.getFont().deriveFont(Font.BOLD, 16f));
        JPanel info = new JPanel(new GridLayout(0, 1));
        info.setBorder(BorderFactory.createEmptyBorder(0, 14, 6, 14));
        info.add(constituentLabel);
        info.add(eligibility);

        JPanel north = new JPanel(new BorderLayout());
        north.add(search, BorderLayout.NORTH);
        north.add(info, BorderLayout.CENTER);
        view.add(north, BorderLayout.NORTH);

        // ---- payment history (step 6)
        table.setRowHeight(24);
        int[] widths = {40, 250, 120, 100, 90, 90, 110, 65, 210, 160};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                boolean due = r < shown.size() && shown.get(r).isDue(LocalDate.now());
                setBackground(sel ? t.getSelectionBackground() : due ? DUE : t.getBackground());
                return this;
            }
        });
        view.add(new JScrollPane(table), BorderLayout.CENTER);

        // ---- actions (steps 2-5, 7)
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        buttons.add(button("1  Record payment", e -> onRecord()));
        buttons.add(button("2  Print cheque", e -> onPrintCheque()));
        buttons.add(button("3  Attach cheque scan", e -> onAttachScan()));
        buttons.add(button("4  Update status", e -> onStatus()));
        buttons.add(button("Correct payment", e -> onCorrect()));
        buttons.add(button("Verify payment", e -> onVerify()));
        buttons.add(button("Change history", e -> onHistory()));
        buttons.add(button("Audit trail", e -> onAudit()));
        summaryButton.addActionListener(e -> onSummary());
        buttons.add(summaryButton);
        JLabel hint = new JLabel("Amber rows are due today or overdue. Cheques are printed by the office, scanned and attached here; the system does not pay anyone.");
        hint.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
        JPanel south = new JPanel(new BorderLayout());
        south.add(buttons, BorderLayout.CENTER);
        south.add(hint, BorderLayout.SOUTH);
        view.add(south, BorderLayout.SOUTH);
    }

    @Override public String getTitle() { return "Payments & Benefits"; }
    @Override public JComponent getView() { return view; }

    @Override public void onUserChanged(StaffMember u) {
        this.user = u;
        current = null;
        searchField.setText("");
        showNothing();
    }

    // ------------------------------------------------------------ helpers
    private JButton button(String text, java.awt.event.ActionListener l) {
        JButton b = new JButton(text); b.addActionListener(l); needsConstituent.add(b); return b;
    }
    private void error(String msg) { JOptionPane.showMessageDialog(view, msg, "Blocked", JOptionPane.WARNING_MESSAGE); }
    private void info(String msg) { JOptionPane.showMessageDialog(view, msg, "Done", JOptionPane.INFORMATION_MESSAGE); }
    private boolean allowed() { return user != null && user.canViewPayments(); }

    private Payment selected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= shown.size()) { error("Select a payment in the table first."); return null; }
        return shown.get(row);
    }

    /** Clears the screen and switches things on or off for the logged-in user. */
    private void showNothing() {
        tableModel.setRowCount(0);
        shown = List.of();
        constituentLabel.setText("No constituent selected");
        boolean ok = allowed();
        searchField.setEnabled(ok); searchButton.setEnabled(ok); summaryButton.setEnabled(ok);
        for (JButton b : needsConstituent) b.setEnabled(false);
        if (!ok) {
            eligibility.setText("ACCESS DENIED: " + (user == null ? "nobody" : user.getName())
                    + " is not authorised to view or manage payments.");
            eligibility.setForeground(RED);
        } else {
            eligibility.setText("Search for a constituent by name or ID number to view and manage their payments.");
            eligibility.setForeground(Color.DARK_GRAY);
        }
    }

    // ------------------------------------------------------------ step 1: search
    private void onSearch() {
        if (!allowed()) return;
        String text = searchField.getText().trim();
        if (text.isEmpty()) { error("Type a constituent's name or ID number first."); return; }
        try {
            List<Applicant> found = service.searchApplicants(user, text);
            if (found.isEmpty()) {
                current = null; showNothing();
                eligibility.setText("No constituent found for \"" + text + "\".");
                eligibility.setForeground(RED);
                return;
            }
            Applicant pick = found.size() == 1 ? found.get(0) : choose(found);
            if (pick == null) return;
            current = pick;
            refresh();
        } catch (PaymentException e) { error(e.getMessage()); }
    }

    /** Several constituents matched: let the user pick one from a short list. */
    private Applicant choose(List<Applicant> found) {
        DefaultListModel<String> model = new DefaultListModel<>();
        for (Applicant a : found) model.addElement(a.getId() + "  -  " + a.getName());
        JList<String> list = new JList<>(model);
        list.setSelectedIndex(0);
        list.setVisibleRowCount(Math.min(found.size(), 10));
        int r = JOptionPane.showConfirmDialog(view, new JScrollPane(list), found.size() + " constituents match - choose one",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return (r == JOptionPane.OK_OPTION && list.getSelectedIndex() >= 0) ? found.get(list.getSelectedIndex()) : null;
    }

    /** Reloads the eligibility banner and the payment table for the chosen constituent. */
    private void refresh() {
        tableModel.setRowCount(0);
        shown = List.of();
        if (!allowed() || current == null) { showNothing(); return; }
        for (JButton b : needsConstituent) b.setEnabled(true);
        constituentLabel.setText(current.getName() + "   (ID " + current.getId() + ")");

        List<Programme> eligible = service.getEligibleProgrammes(current.getId());
        if (eligible.isEmpty()) {
            eligibility.setText("NOT eligible for payment: not approved and actively enrolled in a programme that provides benefits.");
            eligibility.setForeground(RED);
        } else {
            StringBuilder names = new StringBuilder();
            for (Programme p : eligible) names.append(names.length() == 0 ? "" : "; ").append(p.getName());
            eligibility.setText("Eligible for payment. Enrolled in: " + names);
            eligibility.setForeground(GREEN);
        }
        try { shown = service.getPaymentHistory(user, current.getId()); }
        catch (PaymentException e) { error(e.getMessage()); return; }
        for (Payment p : shown) {
            Programme pr = repo.findProgramme(p.getProgrammeId());
            String status = p.getStatus() + (p.isDue(LocalDate.now()) ? " - DUE" : "");
            tableModel.addRow(new Object[]{p.getId(), pr == null ? "?" : pr.getName(), p.getBenefitType(),
                    "$" + p.getAmount().toPlainString(), p.getScheduledDate(),
                    p.getDistributedDate() == null ? "" : p.getDistributedDate(), status,
                    p.isVerified() ? "Yes" : "No", p.getPayeeName(),
                    p.hasChequeScan() ? new File(p.getChequeScanPath()).getName() : "Not attached"});
        }
    }

    // ------------------------------------------------------------ steps 2 and 3: amount and scheduled date
    private void onRecord() {
        List<Programme> eligible = service.getEligibleProgrammes(current.getId());
        if (eligible.isEmpty()) {
            error(current.getName() + " is not approved and actively enrolled in a programme that provides benefits, "
                    + "so no payment can be recorded.");
            return;
        }
        JComboBox<Programme> prog = new JComboBox<>(eligible.toArray(new Programme[0]));
        prog.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof Programme p) setText(p.getName());
                return this;
            }
        });
        JComboBox<BenefitType> type = new JComboBox<>(BenefitType.values());
        JTextField amount = new JTextField(12), date = new JTextField(LocalDate.now().toString(), 12), payee = new JTextField(20);
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Programme:")); form.add(prog);
        form.add(new JLabel("Benefit type:")); form.add(type);
        form.add(new JLabel("Payment method:")); form.add(new JLabel(PaymentMethod.CHEQUE.toString()));
        form.add(new JLabel("Amount ($):")); form.add(amount);
        form.add(new JLabel("Scheduled distribution date (yyyy-mm-dd):")); form.add(date);
        form.add(new JLabel("Payee (blank = constituent):")); form.add(payee);
        if (JOptionPane.showConfirmDialog(view, form, "Record payment for " + current.getName(),
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            Payment p = service.recordPayment(user, current.getId(), ((Programme) prog.getSelectedItem()).getId(),
                    (BenefitType) type.getSelectedItem(), parseMoney(amount.getText()),
                    parseDate(date.getText()), payee.getText());
            refresh();
            info("Payment recorded (ID " + p.getId() + ", status Pending, scheduled " + p.getScheduledDate() + ").");
        } catch (PaymentException e) { error(e.getMessage()); }
    }

    // ------------------------------------------------------------ step 4: print the cheque, attach the scan
    private void onPrintCheque() {
        Payment p = selected(); if (p == null) return;
        try {
            String text = service.printCheque(user, p.getId());
            JTextArea area = new JTextArea(text, 14, 58);
            area.setEditable(false);
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            int choice = JOptionPane.showOptionDialog(view, new JScrollPane(area), "Cheque for payment " + p.getId(),
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[]{"Print", "Close"}, "Print");
            if (choice == 0) {
                try { area.print(); }
                catch (PrinterException ex) { error("The cheque could not be printed: " + ex.getMessage()); }
            }
            info("When the cheque has been handed over, scan it and use \"Attach cheque scan\".");
        } catch (PaymentException e) { error(e.getMessage()); }
    }

    private void onAttachScan() {
        Payment p = selected(); if (p == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose the scanned cheque (PDF, PNG or JPEG, up to 10 MB)");
        chooser.setFileFilter(new FileNameExtensionFilter("Scanned cheque (pdf, png, jpg)", "pdf", "png", "jpg", "jpeg"));
        if (chooser.showOpenDialog(view) != JFileChooser.APPROVE_OPTION) return;
        File f = chooser.getSelectedFile();
        try {
            service.attachChequeScan(user, p.getId(), f.getAbsolutePath(), f.length());
            refresh();
            info("Scanned cheque attached to payment " + p.getId() + ".");
        } catch (PaymentException e) { error(e.getMessage()); }
    }

    // ------------------------------------------------------------ step 5: update the status
    private void onStatus() {
        Payment p = selected(); if (p == null) return;
        JComboBox<PaymentStatus> next = new JComboBox<>();
        for (PaymentStatus s : PaymentStatus.values()) if (p.getStatus().canTransitionTo(s)) next.addItem(s);
        if (next.getItemCount() == 0) { error("A " + p.getStatus() + " payment cannot be changed any further."); return; }
        JTextField note = new JTextField(25);
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Current status:")); form.add(new JLabel(p.getStatus().toString()));
        form.add(new JLabel("New status:")); form.add(next);
        form.add(new JLabel("Note (required if Failed):")); form.add(note);
        if (JOptionPane.showConfirmDialog(view, form, "Update status of payment " + p.getId(), JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            service.updateStatus(user, p.getId(), (PaymentStatus) next.getSelectedItem(), note.getText());
            refresh();
        } catch (PaymentException e) { error(e.getMessage()); }
    }

    private void onCorrect() {
        Payment p = selected(); if (p == null) return;
        JComboBox<BenefitType> type = new JComboBox<>(BenefitType.values());
        type.setSelectedItem(p.getBenefitType());
        JTextField amount = new JTextField(p.getAmount().toPlainString(), 12);
        JTextField date = new JTextField(p.getScheduledDate().toString(), 12);
        JTextField payee = new JTextField(p.getPayeeName() == null ? "" : p.getPayeeName(), 20);
        JTextField reason = new JTextField(25);
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Benefit type:")); form.add(type);
        form.add(new JLabel("Amount ($):")); form.add(amount);
        form.add(new JLabel("Scheduled date:")); form.add(date);
        form.add(new JLabel("Payee:")); form.add(payee);
        form.add(new JLabel("Reason (min 10 characters):")); form.add(reason);
        if (JOptionPane.showConfirmDialog(view, form, "Correct payment " + p.getId(), JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            service.correctPayment(user, p.getId(), (BenefitType) type.getSelectedItem(), parseMoney(amount.getText()),
                    parseDate(date.getText()), payee.getText(), reason.getText());
            refresh();
            info("Payment corrected. The old values are kept in the change history.");
        } catch (PaymentException e) { error(e.getMessage()); }
    }

    private void onVerify() {
        Payment p = selected(); if (p == null) return;
        try {
            service.verifyPayment(user, p.getId());
            refresh();
            info("Payment verified.");
        } catch (PaymentException e) { error(e.getMessage()); }
    }

    // ------------------------------------------------------------ steps 6 and 7: history and audit trail
    private void onHistory() {
        Payment p = selected(); if (p == null) return;
        StringBuilder sb = new StringBuilder();
        for (PaymentChange c : service.getChanges(p.getId()))
            sb.append(c.getChangedAt().format(STAMP)).append("  ")
              .append(c.getFieldName()).append(": ").append(c.getOldValue()).append(" -> ").append(c.getNewValue())
              .append("  (").append(c.getReason()).append(")\n");
        if (sb.length() == 0) sb.append("No changes recorded for this payment.");
        showText("Change history for payment " + p.getId(), sb.toString());
    }

    private void onAudit() {
        try {
            StringBuilder sb = new StringBuilder();
            for (PaymentAudit a : service.getAuditTrail(user, current.getId()))
                sb.append(a.getPerformedAt().format(STAMP)).append("  ").append(staffName(a.getPerformedBy()))
                  .append("  |  payment ").append(a.getPaymentId()).append("  |  ").append(a.getAction())
                  .append("  |  ").append(a.getDetails()).append("\n");
            if (sb.length() == 0) sb.append("No payment activity recorded for this constituent yet.");
            showText("Audit trail for " + current.getName(), sb.toString());
        } catch (PaymentException e) { error(e.getMessage()); }
    }

    private String staffName(int id) {
        for (StaffMember s : repo.findAllStaff()) if (s.getId() == id) return s.getName();
        return "Staff #" + id;
    }

    private void showText(String title, String text) {
        JTextArea area = new JTextArea(text, 16, 100);
        area.setEditable(false);
        area.setCaretPosition(0);
        JOptionPane.showMessageDialog(view, new JScrollPane(area), title, JOptionPane.PLAIN_MESSAGE);
    }

    private void onSummary() {
        JTextField from = new JTextField(LocalDate.now().withDayOfMonth(1).toString(), 12);
        JTextField to = new JTextField(LocalDate.now().toString(), 12);
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("From (yyyy-mm-dd):")); form.add(from);
        form.add(new JLabel("To (yyyy-mm-dd):")); form.add(to);
        if (JOptionPane.showConfirmDialog(view, form, "Payment summary", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            PaymentSummary s = service.getSummary(parseDate(from.getText()), parseDate(to.getText()));
            StringBuilder sb = new StringBuilder("Completed payments " + s.getFromDate() + " to " + s.getToDate() + "\n\n");
            sb.append("Total distributed: $").append(s.getTotalDistributed().toPlainString()).append("\n");
            sb.append("Number of payments: ").append(s.getPaymentCount()).append("\n\n");
            s.getTotalByBenefitType().forEach((k, v) -> sb.append(k).append(": $").append(v.toPlainString()).append("\n"));
            info(sb.toString());
        } catch (PaymentException e) { error(e.getMessage()); }
    }

    private BigDecimal parseMoney(String text) throws PaymentException {
        try { return new BigDecimal(text.trim().replace(",", "").replace("$", "")); }
        catch (NumberFormatException e) { throw new PaymentException("Enter the amount as a number, for example 5000.00"); }
    }
    private LocalDate parseDate(String text) throws PaymentException {
        try { return LocalDate.parse(text.trim()); }
        catch (DateTimeParseException e) { throw new PaymentException("Enter a valid date as yyyy-mm-dd, for example 2026-11-25"); }
    }
}
