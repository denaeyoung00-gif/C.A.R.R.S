package carrs.app;

import carrs.model.*;
import carrs.repository.CarrsRepository;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.print.PrinterException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.*;

public class ReportTab implements FeatureTab {
    private static final Color ORANGE = new Color(204, 85, 0);

    private final CarrsRepository repo;
    private final JPanel view = new JPanel(new BorderLayout(10, 10));
    private final JTextArea output = new JTextArea();
    private StaffMember user;

    @FunctionalInterface
    private interface ReportAction {
        Report generate() throws Exception;
    }

    public ReportTab(CarrsRepository repo) {
        this.repo = repo;
        view.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel heading = new JLabel("Reports");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 20f));
        heading.setForeground(ORANGE);
        JLabel hint = new JLabel("Click Generate Report to choose the type of report. A generated report can then be printed.");
        JPanel north = new JPanel(new GridLayout(0, 1, 0, 2));
        north.add(heading);
        north.add(hint);

        JLabel section = new JLabel("Generated report");
        section.setFont(section.getFont().deriveFont(Font.BOLD, 14f));
        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.add(section, BorderLayout.NORTH);
        center.add(new JScrollPane(output), BorderLayout.CENTER);

        JButton generate = new JButton("Generate Report");
        JButton print = new JButton("Print");
        generate.addActionListener(e -> onGenerate());
        print.addActionListener(e -> onPrint());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.add(generate);
        buttons.add(print);

        view.add(north, BorderLayout.NORTH);
        view.add(center, BorderLayout.CENTER);
        view.add(buttons, BorderLayout.SOUTH);
    }

    @Override public String getTitle() { return "Reports"; }
    @Override public JComponent getView() { return view; }

    @Override public void onUserChanged(StaffMember u) {
        this.user = u;
        output.setText("");
    }

    private void onGenerate() {
        JPanel choices = new JPanel(new GridLayout(0, 1, 6, 6));
        choices.add(new JLabel("Please select the type of report you would like to generate."));

        JOptionPane pane = new JOptionPane(choices, JOptionPane.WARNING_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, new Object[]{"Cancel"});
        JDialog dialog = pane.createDialog(view, "Generate Report");
        Runnable[] chosen = new Runnable[1];

        addChoice(choices, dialog, chosen, "Quarterly report", this::onQuarterly);
        addChoice(choices, dialog, chosen, "Programme report", this::onProgramme);
        addChoice(choices, dialog, chosen, "Benefits report", this::onBenefits);
        addChoice(choices, dialog, chosen, "Application timeline report", this::onTimeline);
        addChoice(choices, dialog, chosen, "Transfer report", this::onTransfer);
        dialog.pack();
        dialog.setLocationRelativeTo(view);

        dialog.setVisible(true);
        if (chosen[0] != null) chosen[0].run();
    }

    private void addChoice(JPanel choices, JDialog dialog, Runnable[] chosen, String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> {
            chosen[0] = action;
            dialog.dispose();
        });
        choices.add(button);
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(view, msg, "Report error", JOptionPane.WARNING_MESSAGE);
    }

    private String ask(String prompt, String title) {
        String s = JOptionPane.showInputDialog(view, prompt, title, JOptionPane.QUESTION_MESSAGE);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) { error("Nothing was entered. " + prompt); return null; }
        return s;
    }

    private void run(ReportAction action) {
        output.setText("");
        try {
            Report report = action.generate();
            output.setText(String.join("\n", report.getContent()));
            output.setCaretPosition(0);
        } catch (NumberFormatException e) {
            error("Please check the value you entered. IDs and the year must be numbers.");
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    private void onQuarterly() {
        JComboBox<String> quarter = new JComboBox<>(
                new String[]{"Please select a quarter", "Q1", "Q2", "Q3", "Q4"});
        JTextField year = new JTextField(String.valueOf(LocalDate.now().getYear()), 6);
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Quarter:"));
        form.add(quarter);
        form.add(new JLabel("Year:"));
        form.add(year);
        if (JOptionPane.showConfirmDialog(view, form, "Quarterly report",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        if (quarter.getSelectedIndex() == 0) { error("Please select a quarter."); return; }

        int q = quarter.getSelectedIndex();
        String y = year.getText().trim();
        run(() -> {
            QuarterlyReport r = new QuarterlyReport();
            r.generateQuarterly(q, Integer.parseInt(y));
            return r;
        });
    }

    private void onProgramme() {
        String id = ask("Please enter the programme ID:", "Programme report");
        if (id == null) return;
        run(() -> { ProgramReport r = new ProgramReport(); r.generateBy(id); return r; });
    }

    private void onBenefits() {
        run(() -> { BenefitsReport r = new BenefitsReport(); r.generateBenefitsReport(); return r; });
    }

    private void onTimeline() {
        String id = ask("Please enter the application ID:", "Application timeline report");
        if (id == null) return;
        run(() -> { ApplicationTimelineReport r = new ApplicationTimelineReport(); r.generateTimeline(id); return r; });
    }

    private void onTransfer() {
        run(() -> { TransferReport r = new TransferReport(); r.generateTransferReport(); return r; });
    }

    private void onPrint() {
        if (output.getText().isEmpty()) { error("Generate a report first."); return; }
        try { output.print(); }
        catch (PrinterException e) { error("The report could not be printed: " + e.getMessage()); }
    }
}

abstract class Report {
    protected String reportID, title, generatedBy;
    protected LocalDate dateGenerated;
    protected List<String> content = new ArrayList<>();

    public String getReportId() { return reportID; }
    public String getTitle() { return title; }
    public String getGeneratedBy() { return generatedBy; }
    public LocalDate getDateGenerated() { return dateGenerated; }
    public List<String> getContent() { return content; }
}

class Transfer {
    private final String applicantID, originalProgram, transferredProgram, reason;

    public Transfer(String applicantID, String originalProgram, String transferredProgram, String reason) {
        this.applicantID = applicantID;
        this.originalProgram = originalProgram;
        this.transferredProgram = transferredProgram;
        this.reason = reason;
    }

    public String getApplicantID() { return applicantID; }
    public String getOriginalProgram() { return originalProgram; }
    public String getTransferredProgram() { return transferredProgram; }
    public String getReason() { return reason; }
}

class QuarterlyReport extends Report {

    public static class QuarterlyException extends Exception {
        public QuarterlyException(String message) { super(message); }
    }

    private int year;
    private List<String> summaryData;
    List<String> Quarters = List.of("Q1", "Q2", "Q3", "Q4");

    public int getYear() { return year; }
    public List<String> getSummaryData() { return summaryData; }

    public void generateQuarterly(int quarter, int year) throws QuarterlyException {
        if (quarter < 1 || quarter > 4)
            throw new QuarterlyException("Please select a quarter.");

        LocalDate start = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
        LocalDate end = start.plusMonths(3);

        String url = "jdbc:mysql://localhost:3306/carrs";
        String user = "root";
        String password = "";
        String applicationSql = "SELECT p.name AS programme, ap.status FROM application ap "
                + "JOIN programme p ON p.programme_id = ap.programme_id "
                + "WHERE ap.decision_date >= ? AND ap.decision_date < ?";
        String paymentSql = "SELECT COUNT(*) AS payments, COALESCE(SUM(amount), 0) AS total FROM payment "
                + "WHERE status = 'COMPLETED' AND distributed_date >= ? AND distributed_date < ?";

        int total = 0, approved = 0, rejected = 0, payments = 0;
        BigDecimal distributed = BigDecimal.ZERO;
        Map<String, Integer> perProgramme = new TreeMap<>();

        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            try (PreparedStatement stmt = conn.prepareStatement(applicationSql)) {
                stmt.setDate(1, java.sql.Date.valueOf(start));
                stmt.setDate(2, java.sql.Date.valueOf(end));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        total++;
                        perProgramme.merge(rs.getString("programme"), 1, Integer::sum);
                        if ("Approved".equalsIgnoreCase(rs.getString("status"))) approved++;
                        else rejected++;
                    }
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(paymentSql)) {
                stmt.setDate(1, java.sql.Date.valueOf(start));
                stmt.setDate(2, java.sql.Date.valueOf(end));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        payments = rs.getInt("payments");
                        distributed = rs.getBigDecimal("total");
                    }
                }
            }
        } catch (SQLException e) {
            throw new QuarterlyException("Database error: " + e.getMessage());
        }

        if (total == 0 && payments == 0)
            throw new QuarterlyException("No data available for Q" + quarter + " " + year + ".");

        List<String> out = new ArrayList<>();
        out.add("=== QUARTERLY REPORT: Q" + quarter + " " + year + " ===");
        out.add("Applications decided: " + total);
        out.add("Approved: " + approved + " | Rejected or disqualified: " + rejected);
        out.add("Benefit payments completed: " + payments);
        out.add("Total amount distributed: $" + distributed.toPlainString());
        if (!perProgramme.isEmpty()) {
            out.add("");
            out.add("--- Applications by programme ---");
            for (Map.Entry<String, Integer> e : perProgramme.entrySet())
                out.add("  " + e.getKey() + ": " + e.getValue());
        }

        this.year = year;
        this.summaryData = out;
        this.content = out;
    }
}

class ProgramReport extends Report {

    public static class ProgramException extends Exception {
        public ProgramException(String message) { super(message); }
    }

    private String programID;
    private int totalApplicants, acceptedCount, rejectedCount, inProgressCount;

    public void generateBy(String programID) throws ProgramException {
        int id = Integer.parseInt(programID.trim());

        String url = "jdbc:mysql://localhost:3306/carrs";
        String user = "root";
        String password = "";
        String checkSql = "SELECT name FROM programme WHERE programme_id = ?";
        String sql = "SELECT a.applicant_id, a.name, a.trn, a.contact_number, ap.status, "
                + "(SELECT COUNT(*) FROM appeal p WHERE p.application_id = ap.application_id "
                + "AND p.decided_at IS NULL) AS open_appeals "
                + "FROM application ap JOIN applicant a ON a.applicant_id = ap.applicant_id "
                + "WHERE ap.programme_id = ? ORDER BY a.name";

        String programmeName;
        int accepted = 0, rejected = 0, inProgress = 0;
        List<String> rows = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                check.setInt(1, id);
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next())
                        throw new ProgramException("Programme " + programID + " does not exist.");
                    programmeName = rs.getString("name");
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String status = rs.getString("status");
                        String row = rs.getInt("applicant_id") + " | " + rs.getString("name")
                                + " | TRN " + rs.getString("trn")
                                + " | " + rs.getString("contact_number") + " | " + status;
                        if ("Approved".equalsIgnoreCase(status)) {
                            accepted++;
                        } else {
                            rejected++;
                            row += " | Reason: not recorded";
                        }
                        if (rs.getInt("open_appeals") > 0) {
                            inProgress++;
                            row += " | Appeal in progress";
                        }
                        rows.add(row);
                    }
                }
            }
        } catch (SQLException e) {
            throw new ProgramException("Database error: " + e.getMessage());
        }

        if (rows.isEmpty())
            throw new ProgramException("Programme " + programID + " has no applicants.");

        this.programID = programID;
        this.totalApplicants = rows.size();
        this.acceptedCount = accepted;
        this.rejectedCount = rejected;
        this.inProgressCount = inProgress;

        List<String> out = new ArrayList<>();
        out.add("=== PROGRAMME REPORT: " + programmeName + " (ID " + programID + ") ===");
        out.add("Total applicants: " + totalApplicants);
        out.add("Accepted: " + acceptedCount + " | Rejected or disqualified: " + rejectedCount
                + " | Appeals in progress: " + inProgressCount);
        out.add("");
        out.addAll(rows);
        this.content = out;
    }
}

class BenefitsReport extends Report {

    public static class BenefitsException extends Exception {
        public BenefitsException(String message) { super(message); }
    }

    private int receivedCount, pendingCount;

    public void generateBenefitsReport() throws BenefitsException {
        String url = "jdbc:mysql://localhost:3306/carrs";
        String user = "root";
        String password = "";
        String sql = "SELECT p.name AS programme, pay.amount, pay.status FROM payment pay "
                + "JOIN programme p ON p.programme_id = pay.programme_id";

        int received = 0, pending = 0, failed = 0, total = 0;
        BigDecimal distributedTotal = BigDecimal.ZERO;
        Map<String, Integer> byStatus = new TreeMap<>();
        Map<String, int[]> byProgramme = new TreeMap<>();

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                total++;
                String status = rs.getString("status");
                BigDecimal amount = rs.getBigDecimal("amount");
                int[] counts = byProgramme.computeIfAbsent(rs.getString("programme"), k -> new int[3]);

                byStatus.merge(status, 1, Integer::sum);
                counts[0]++;
                if ("COMPLETED".equals(status)) {
                    received++;
                    counts[1]++;
                    if (amount != null) distributedTotal = distributedTotal.add(amount);
                } else if ("FAILED".equals(status)) {
                    failed++;
                    counts[2]++;
                } else {
                    pending++;
                }
            }
        } catch (SQLException e) {
            throw new BenefitsException("Database error: " + e.getMessage());
        }

        if (total == 0)
            throw new BenefitsException("There are no benefit or payment records to report on.");

        this.receivedCount = received;
        this.pendingCount = pending;

        List<String> out = new ArrayList<>();
        out.add("=== BENEFITS & PAYMENTS REPORT ===");
        out.add("Total payments recorded: " + total);
        out.add("Received benefits: " + receivedCount);
        out.add("Still to receive (pending or processed): " + pendingCount);
        out.add("Failed: " + failed);
        out.add("Total amount distributed: $" + distributedTotal.toPlainString());
        out.add("");
        out.add("--- By status ---");
        for (Map.Entry<String, Integer> e : byStatus.entrySet())
            out.add("  " + e.getKey() + ": " + e.getValue());
        out.add("");
        out.add("--- By programme ---");
        for (Map.Entry<String, int[]> e : byProgramme.entrySet())
            out.add("  " + e.getKey() + ": " + e.getValue()[0] + " payments, "
                    + e.getValue()[1] + " received, "
                    + (e.getValue()[0] - e.getValue()[1] - e.getValue()[2]) + " still to receive, "
                    + e.getValue()[2] + " failed");
        this.content = out;
    }
}

class ApplicationTimelineReport extends Report {

    public static class TimelineException extends Exception {
        public TimelineException(String message) { super(message); }
    }

    private String applicationID;
    private LocalDate startDate, benefitReceivedDate;
    private int durationDays;

    public void generateTimeline(String applicationID) throws TimelineException {
        int id = Integer.parseInt(applicationID.trim());

        String url = "jdbc:mysql://localhost:3306/carrs";
        String user = "root";
        String password = "";
        String sql = "SELECT ap.status, ap.decision_date, e.enrolled_at, pay.scheduled_date, pay.distributed_date "
                + "FROM application ap "
                + "LEFT JOIN enrolment e ON e.applicant_id = ap.applicant_id AND e.programme_id = ap.programme_id "
                + "LEFT JOIN payment pay ON pay.applicant_id = ap.applicant_id AND pay.programme_id = ap.programme_id "
                + "WHERE ap.application_id = ?";

        boolean found = false;
        String status = null;
        LocalDate decided = null, enrolled = null, scheduled = null, received = null;

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    found = true;
                    status = rs.getString("status");

                    java.sql.Timestamp d = rs.getTimestamp("decision_date");
                    if (d != null) decided = d.toLocalDateTime().toLocalDate();

                    java.sql.Timestamp en = rs.getTimestamp("enrolled_at");
                    if (en != null) {
                        LocalDate day = en.toLocalDateTime().toLocalDate();
                        if (enrolled == null || day.isBefore(enrolled)) enrolled = day;
                    }

                    java.sql.Date sc = rs.getDate("scheduled_date");
                    if (sc != null) {
                        LocalDate day = sc.toLocalDate();
                        if (scheduled == null || day.isBefore(scheduled)) scheduled = day;
                    }

                    java.sql.Date dist = rs.getDate("distributed_date");
                    if (dist != null) {
                        LocalDate day = dist.toLocalDate();
                        if (received == null || day.isBefore(received)) received = day;
                    }
                }
            }
        } catch (SQLException e) {
            throw new TimelineException("Database error: " + e.getMessage());
        }

        if (!found)
            throw new TimelineException("Application " + applicationID + " does not exist.");
        if (decided == null || received == null)
            throw new TimelineException("Application " + applicationID
                    + " is incomplete: no benefit has been received yet, so there is no full timeline.");

        this.applicationID = applicationID;
        this.startDate = decided;
        this.benefitReceivedDate = received;
        this.durationDays = (int) ChronoUnit.DAYS.between(startDate, benefitReceivedDate);

        List<String> out = new ArrayList<>();
        out.add("=== APPLICATION TIMELINE: APPLICATION " + applicationID + " ===");
        out.add("Decision date (" + status + "): " + startDate);
        if (enrolled != null) out.add("Enrolled in programme:      " + enrolled);
        if (scheduled != null) out.add("Payment scheduled:          " + scheduled);
        out.add("Benefits received:         " + benefitReceivedDate);
        out.add("Total time taken:          " + durationDays + " days");
        this.content = out;
    }
}

class TransferReport extends Report {

    public static class TransferException extends Exception {
        public TransferException(String message) { super(message); }
    }

    private List<Transfer> transferlist;

    public void generateTransferReport() throws TransferException {
        String url = "jdbc:mysql://localhost:3306/carrs";
        String user = "root";
        String password = "";
        String sql = "SELECT a.applicant_id, a.name, p1.name AS original_program, p2.name AS transferred_program "
                + "FROM enrolment e2 "
                + "JOIN enrolment e1 ON e1.enrolment_id = e2.transferred_from "
                + "JOIN applicant a ON a.applicant_id = e2.applicant_id "
                + "JOIN programme p1 ON p1.programme_id = e1.programme_id "
                + "JOIN programme p2 ON p2.programme_id = e2.programme_id "
                + "ORDER BY e2.enrolled_at";

        List<Transfer> found = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                found.add(new Transfer(rs.getInt("applicant_id") + " - " + rs.getString("name"),
                        rs.getString("original_program"), rs.getString("transferred_program"), "Not recorded"));
            }
        } catch (SQLException e) {
            throw new TransferException("Database error: " + e.getMessage());
        }

        if (found.isEmpty())
            throw new TransferException("There are no transferred applicants.");

        this.transferlist = found;

        Map<String, Integer> byPath = new TreeMap<>();
        for (Transfer t : transferlist)
            byPath.merge(t.getOriginalProgram() + " -> " + t.getTransferredProgram(), 1, Integer::sum);

        List<String> out = new ArrayList<>();
        out.add("=== TRANSFER REPORT ===");
        out.add("Total transferred applicants: " + transferlist.size());
        out.add("");
        out.add("--- Summary by programme transfer ---");
        for (Map.Entry<String, Integer> e : byPath.entrySet())
            out.add("  " + e.getKey() + ": " + e.getValue());
        out.add("");
        out.add("--- All transferred applicants ---");
        for (Transfer t : transferlist)
            out.add(t.getApplicantID() + " | " + t.getOriginalProgram()
                    + " -> " + t.getTransferredProgram() + " | Reason: " + t.getReason());
        this.content = out;
    }
}