package carrs.app;

import carrs.model.*;
import carrs.repository.CarrsRepository;
import carrs.repository.MySqlCarrsRepository;
import carrs.service.PaymentException;
import carrs.service.PaymentService;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** The Payment and Benefit Management screen. */
public class PaymentApp extends JFrame {
    private final CarrsRepository repo;
    private final PaymentService service;
    private final JComboBox<StaffMember> userBox = new JComboBox<>();
    private final JComboBox<Applicant> applicantBox = new JComboBox<>();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Programme", "Benefit", "Amount", "Scheduled", "Paid on", "Status", "Verified", "Payee"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private List<Payment> shown = List.of();

    public PaymentApp(CarrsRepository repo) {
        super("C.A.R.R.S - Payment and Benefit Management");
        this.repo = repo;
        this.service = new PaymentService(repo);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1150, 520);
        setLocationRelativeTo(null);

        userBox.addItem(new StaffMember(1, "Alexia James", Role.OFFICE_MANAGER));
        userBox.addItem(new StaffMember(3, "Office Staff", Role.OFFICE_STAFF));
        userBox.addItem(new StaffMember(2, "Sam Intern", Role.OFFICE_ATTENDANT));
        userBox.addItem(new StaffMember(4, "Dr Angela Brown-Burke", Role.MEMBER_OF_PARLIAMENT));
        userBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof StaffMember m) setText(m.getName() + " (" + m.getRole() + ")");
                return this;
            }
        });
        applicantBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof Applicant a) setText(a.getName());
                return this;
            }
        });
        for (Applicant a : repo.findAllApplicants()) applicantBox.addItem(a);
        applicantBox.addActionListener(e -> refresh());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        top.add(new JLabel("Logged in as:")); top.add(userBox);
        top.add(new JLabel("Constituent:")); top.add(applicantBox);
        add(top, BorderLayout.NORTH);

        table.setRowHeight(24);
        int[] widths = {40, 290, 150, 100, 90, 90, 100, 70, 230};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        buttons.add(button("Record payment", e -> onRecord()));
        buttons.add(button("Update status", e -> onStatus()));
        buttons.add(button("Correct payment", e -> onCorrect()));
        buttons.add(button("Verify payment", e -> onVerify()));
        buttons.add(button("View change history", e -> onHistory()));
        buttons.add(button("Payment summary", e -> onSummary()));
        add(buttons, BorderLayout.SOUTH);
        refresh();
    }

    private JButton button(String text, java.awt.event.ActionListener l) {
        JButton b = new JButton(text); b.addActionListener(l); return b;
    }
    private StaffMember user() { return (StaffMember) userBox.getSelectedItem(); }
    private Applicant applicant() { return (Applicant) applicantBox.getSelectedItem(); }
    private void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Blocked", JOptionPane.WARNING_MESSAGE); }
    private void info(String msg) { JOptionPane.showMessageDialog(this, msg, "Done", JOptionPane.INFORMATION_MESSAGE); }

    private Payment selected() {
        int row = table.getSelectedRow();
        if (row < 0) { error("Select a payment in the table first."); return null; }
        return shown.get(row);
    }

    private void refresh() {
        tableModel.setRowCount(0);
        if (applicant() == null) return;
        shown = service.getPaymentHistory(applicant().getId());
        for (Payment p : shown) {
            Programme pr = repo.findProgramme(p.getProgrammeId());
            tableModel.addRow(new Object[]{p.getId(), pr == null ? "?" : pr.getName(), p.getBenefitType(),
                    "$" + p.getAmount().toPlainString(), p.getScheduledDate(),
                    p.getDistributedDate() == null ? "" : p.getDistributedDate(), p.getStatus(),
                    p.isVerified() ? "Yes" : "No", p.getPayeeName()});
        }
    }

    private void onRecord() {
        JComboBox<Programme> prog = new JComboBox<>();
        for (Programme p : repo.findActiveProgrammes()) prog.addItem(p);
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
        form.add(new JLabel("Amount:")); form.add(amount);
        form.add(new JLabel("Scheduled date (yyyy-mm-dd):")); form.add(date);
        form.add(new JLabel("Payee (blank = constituent):")); form.add(payee);
        if (JOptionPane.showConfirmDialog(this, form, "Record payment for " + applicant().getName(),
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            Payment p = service.recordPayment(user(), applicant().getId(), ((Programme) prog.getSelectedItem()).getId(),
                    (BenefitType) type.getSelectedItem(), parseMoney(amount.getText()), parseDate(date.getText()), payee.getText());
            refresh();
            info("Payment recorded (ID " + p.getId() + ", status Pending).");
        } catch (PaymentException e) { error(e.getMessage()); }
    }

    private void onStatus() {
        Payment p = selected(); if (p == null) return;
        PaymentStatus[] options = PaymentStatus.values();
        JComboBox<PaymentStatus> next = new JComboBox<>(options);
        JTextField note = new JTextField(25);
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Current status:")); form.add(new JLabel(p.getStatus().toString()));
        form.add(new JLabel("New status:")); form.add(next);
        form.add(new JLabel("Note (required if Failed):")); form.add(note);
        if (JOptionPane.showConfirmDialog(this, form, "Update status", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            service.updateStatus(user(), p.getId(), (PaymentStatus) next.getSelectedItem(), note.getText());
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
        form.add(new JLabel("Amount:")); form.add(amount);
        form.add(new JLabel("Scheduled date:")); form.add(date);
        form.add(new JLabel("Payee:")); form.add(payee);
        form.add(new JLabel("Reason (min 10 characters):")); form.add(reason);
        if (JOptionPane.showConfirmDialog(this, form, "Correct payment " + p.getId(), JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            service.correctPayment(user(), p.getId(), (BenefitType) type.getSelectedItem(), parseMoney(amount.getText()),
                    parseDate(date.getText()), payee.getText(), reason.getText());
            refresh();
            info("Payment corrected. The old values are kept in the change history.");
        } catch (PaymentException e) { error(e.getMessage()); }
    }

    private void onVerify() {
        Payment p = selected(); if (p == null) return;
        try {
            service.verifyPayment(user(), p.getId());
            refresh();
            info("Payment verified.");
        } catch (PaymentException e) { error(e.getMessage()); }
    }

    private void onHistory() {
        Payment p = selected(); if (p == null) return;
        StringBuilder sb = new StringBuilder();
        for (PaymentChange c : service.getChanges(p.getId()))
            sb.append(c.getChangedAt().toLocalDate()).append("  ").append(c.getFieldName()).append(": ")
              .append(c.getOldValue()).append(" -> ").append(c.getNewValue())
              .append("  (").append(c.getReason()).append(")\n");
        if (sb.length() == 0) sb.append("No changes recorded for this payment.");
        JTextArea area = new JTextArea(sb.toString(), 12, 60);
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Change history for payment " + p.getId(), JOptionPane.PLAIN_MESSAGE);
    }

    private void onSummary() {
        JTextField from = new JTextField(LocalDate.now().withDayOfMonth(1).toString(), 12);
        JTextField to = new JTextField(LocalDate.now().toString(), 12);
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("From (yyyy-mm-dd):")); form.add(from);
        form.add(new JLabel("To (yyyy-mm-dd):")); form.add(to);
        if (JOptionPane.showConfirmDialog(this, form, "Payment summary", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
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
        catch (DateTimeParseException e) { throw new PaymentException("Enter dates as yyyy-mm-dd, for example 2026-11-25"); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PaymentApp(new MySqlCarrsRepository()).setVisible(true));
    }
}
