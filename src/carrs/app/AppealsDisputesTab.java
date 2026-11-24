package carrs.app;

import carrs.model.StaffMember;
import carrs.repository.CarrsRepository;

import java.awt.*;
import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class AppealsDisputesTab implements FeatureTab {

    private static final Color ORANGE_PRIMARY = new Color(200, 80, 0);
    private static final Color ORANGE_ACCENT = new Color(230, 110, 20);
    private static final Color ORANGE_SUBTLE = new Color(250, 225, 195);
    private static final Color ORANGE_CREAM_BG = new Color(255, 245, 232);
    private static final Color ORANGE_BORDER = new Color(225, 180, 145);
    private static final Color TEXT_DARK = new Color(33, 37, 41);
    private static final Color STATUS_RED = new Color(166, 28, 28);
    private static final Color STATUS_GREEN = new Color(34, 120, 68);

    private final CarrsRepository repo;
    private final JPanel rootPanel = new JPanel(new BorderLayout());

    private final AppealService appealService = new AppealService();
    private final Map<String, ApplicationRecord> mockDatabase = new LinkedHashMap<>();

    private JComboBox<String> cmbApplications;
    private JTextField txtCitizenId;
    private JTextField txtProgram;
    private JLabel lblStatusBadge;
    private JTextField txtDecisionDate;
    private JCheckBox chkVerifiedResident;
    private JTextField txtConstituency;
    private JTextField txtAgentId;

    private JCheckBox chkIncome;
    private JCheckBox chkDoc;
    private JCheckBox chkDependency;
    private JCheckBox chkAdminError;

    private JTextArea txtJustification;
    private JLabel lblCharCounter;
    private DefaultListModel<SupportingDocument> docListModel;
    private JList<SupportingDocument> lstDocuments;

    private JButton btnAddDoc;
    private JButton btnRemoveDoc;
    private JButton btnLodgeAppeal;

    private DefaultTableModel docketTableModel;
    private JTable docketTable;
    private JCheckBox chkReviewerRole;
    private JTextArea txtDecisionNote;
    private JLabel lblDecisionCharCounter;
    private JCheckBox chkSimulateSyncError;

    private JButton btnReviewCase;
    private JButton btnApprove;
    private JButton btnDeny;
    private JLabel lblAccessBanner;

    public AppealsDisputesTab(CarrsRepository repo) {
        this.repo = repo;
        seedApplicationData();
        initUI();
    }

    @Override
    public String getTitle() {
        return "Appeals & Disputes";
    }

    @Override
    public JComponent getView() {
        return rootPanel;
    }

    @Override
    public void onUserChanged(StaffMember user) {
        if (user != null && txtAgentId != null) {
            txtAgentId.setText(user.getName());
        }
        applyRoleAccess(user);
    }

    private void applyRoleAccess(StaffMember user) {
        boolean isAttendant = false;
        if (user != null && user.getRole() != null) {
            String roleName = user.getRole().toString().trim().replace("_", " ");
            isAttendant = "Office attendant".equalsIgnoreCase(roleName)
                    || roleName.toLowerCase().contains("attendant")
                    || roleName.toLowerCase().contains("intern");
        }
        boolean allowed = !isAttendant;
        if (cmbApplications != null)
            cmbApplications.setEnabled(allowed);
        if (txtConstituency != null)
            txtConstituency.setEnabled(allowed);
        if (chkVerifiedResident != null)
            chkVerifiedResident.setEnabled(allowed);
        if (chkIncome != null)
            chkIncome.setEnabled(allowed);
        if (chkDoc != null)
            chkDoc.setEnabled(allowed);
        if (chkDependency != null)
            chkDependency.setEnabled(allowed);
        if (chkAdminError != null)
            chkAdminError.setEnabled(allowed);
        if (txtJustification != null)
            txtJustification.setEnabled(allowed);
        if (lstDocuments != null)
            lstDocuments.setEnabled(allowed);
        if (btnAddDoc != null)
            btnAddDoc.setEnabled(allowed);
        if (btnRemoveDoc != null)
            btnRemoveDoc.setEnabled(allowed);
        if (btnLodgeAppeal != null)
            btnLodgeAppeal.setEnabled(allowed);
        if (chkReviewerRole != null)
            chkReviewerRole.setEnabled(allowed);
        if (txtDecisionNote != null)
            txtDecisionNote.setEnabled(allowed);
        if (chkSimulateSyncError != null)
            chkSimulateSyncError.setEnabled(allowed);
        if (btnReviewCase != null)
            btnReviewCase.setEnabled(allowed);
        if (btnApprove != null)
            btnApprove.setEnabled(allowed);
        if (btnDeny != null)
            btnDeny.setEnabled(allowed);
        if (lblAccessBanner != null) {
            lblAccessBanner.setText(allowed ? " SECURE AUDIT MODE " : " ACCESS RESTRICTED: OFFICE ATTENDANT ");
            lblAccessBanner.setBackground(allowed ? ORANGE_ACCENT : STATUS_RED);
        }
    }

    private void seedApplicationData() {
        mockDatabase.put("APP-001", new ApplicationRecord("APP-001", "CIT-1001", "PROG-WELFARE", "Rejected",
                LocalDateTime.now().minusDays(14)));
        mockDatabase.put("APP-002", new ApplicationRecord("APP-002", "CIT-1002", "PROG-GRANT", "Rejected",
                LocalDateTime.now().minusDays(35)));
        mockDatabase.put("APP-003", new ApplicationRecord("APP-003", "CIT-1003", "PROG-GRANT", "Approved",
                LocalDateTime.now().minusDays(5)));
        mockDatabase.put("APP-004", new ApplicationRecord("APP-004", "CIT-1004", "PROG-DISASTER", "Disqualified",
                LocalDateTime.now().minusDays(4)));
    }

    private void initUI() {
        rootPanel.setBackground(ORANGE_CREAM_BG);
        rootPanel.add(createHeaderPanel(), BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                createIntakeFormPanel(),
                createDocketLedgerPanel());
        splitPane.setDividerLocation(620);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        rootPanel.add(splitPane, BorderLayout.CENTER);

        rootPanel.add(createStatusBar(), BorderLayout.SOUTH);

        if (cmbApplications.getItemCount() > 0) {
            cmbApplications.setSelectedIndex(0);
        }
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ORANGE_PRIMARY);
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 3));
        titleBlock.setOpaque(false);

        JLabel lblEcosystem = new JLabel("C.A.R.R.S. FILE TRACKING SYSTEM  ::  STATUTORY SERVICES DIVISION");
        lblEcosystem.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblEcosystem.setForeground(new Color(255, 230, 200));

        JLabel lblTitle = new JLabel("Constituency Appeals & Disputes Resolution");
        lblTitle.setFont(new Font("Georgia", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);

        titleBlock.add(lblEcosystem);
        titleBlock.add(lblTitle);

        JPanel badgeBlock = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        badgeBlock.setOpaque(false);

        lblAccessBanner = new JLabel(" SECURE AUDIT MODE ");

        lblAccessBanner.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblAccessBanner.setOpaque(true);
        lblAccessBanner.setBackground(ORANGE_ACCENT);
        lblAccessBanner.setForeground(Color.WHITE);
        lblAccessBanner.setBorder(new EmptyBorder(6, 12, 6, 12));

        badgeBlock.add(lblAccessBanner);

        header.add(titleBlock, BorderLayout.WEST);
        header.add(badgeBlock, BorderLayout.EAST);
        return header;
    }

    private JPanel createIntakeFormPanel() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBackground(Color.WHITE);

        JPanel scrollContent = new JPanel();
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setBackground(Color.WHITE);
        scrollContent.setBorder(new EmptyBorder(16, 20, 20, 20));

        scrollContent.add(createSectionHeader("1. Underlying Application Verification"));
        scrollContent.add(Box.createVerticalStrut(8));

        JPanel pnlRecord = new JPanel(new GridBagLayout());
        pnlRecord.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        pnlRecord.add(new JLabel("Application ID:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        cmbApplications = new JComboBox<>(mockDatabase.keySet().toArray(new String[0]));
        cmbApplications.setBackground(Color.WHITE);
        cmbApplications.addActionListener(e -> populateApplicationDetails());
        pnlRecord.add(cmbApplications, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        pnlRecord.add(new JLabel("Citizen ID:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        txtCitizenId = createReadOnlyField();
        pnlRecord.add(txtCitizenId, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.3;
        pnlRecord.add(new JLabel("Program Type:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        txtProgram = createReadOnlyField();
        pnlRecord.add(txtProgram, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.3;
        pnlRecord.add(new JLabel("Current Status:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        lblStatusBadge = new JLabel("UNKNOWN");
        lblStatusBadge.setFont(new Font("SansSerif", Font.BOLD, 12));
        pnlRecord.add(lblStatusBadge, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.3;
        pnlRecord.add(new JLabel("Decision Date:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        txtDecisionDate = createReadOnlyField();
        pnlRecord.add(txtDecisionDate, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0.3;
        pnlRecord.add(new JLabel("Constituency Code:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        txtConstituency = new JTextField("CENTRAL");
        txtConstituency.setBorder(new LineBorder(ORANGE_BORDER));
        pnlRecord.add(txtConstituency, gbc);

        gbc.gridx = 1;
        gbc.gridy = 6;
        chkVerifiedResident = new JCheckBox("Applicant is a verified constituency resident");
        chkVerifiedResident.setSelected(true);
        chkVerifiedResident.setOpaque(false);
        chkVerifiedResident.setFont(new Font("SansSerif", Font.BOLD, 12));
        pnlRecord.add(chkVerifiedResident, gbc);

        scrollContent.add(pnlRecord);
        scrollContent.add(Box.createVerticalStrut(14));

        scrollContent.add(createSectionHeader("2. Grounds for Appeal"));
        scrollContent.add(Box.createVerticalStrut(8));

        JPanel pnlGrounds = new JPanel(new GridLayout(2, 2, 8, 6));
        pnlGrounds.setOpaque(false);
        chkIncome = new JCheckBox("Incorrect Income Assessment");
        chkDoc = new JCheckBox("Overlooked Documentation");
        chkDependency = new JCheckBox("Household Dependency Update");
        chkAdminError = new JCheckBox("Administrative Error");
        chkIncome.setOpaque(false);
        chkDoc.setOpaque(false);
        chkDependency.setOpaque(false);
        chkAdminError.setOpaque(false);

        pnlGrounds.add(chkIncome);
        pnlGrounds.add(chkDoc);
        pnlGrounds.add(chkDependency);
        pnlGrounds.add(chkAdminError);
        scrollContent.add(pnlGrounds);
        scrollContent.add(Box.createVerticalStrut(14));

        scrollContent.add(createSectionHeader("3. Applicant Justification"));
        scrollContent.add(Box.createVerticalStrut(6));

        txtJustification = new JTextArea(4, 30);
        txtJustification.setLineWrap(true);
        txtJustification.setWrapStyleWord(true);
        txtJustification.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(ORANGE_BORDER),
                new EmptyBorder(6, 6, 6, 6)));
        txtJustification.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                int length = txtJustification.getText().trim().length();
                lblCharCounter.setText("Character Count: " + length + " / 50 min");
                lblCharCounter.setForeground(length >= 50 ? STATUS_GREEN : STATUS_RED);
            }
        });
        scrollContent.add(new JScrollPane(txtJustification));

        lblCharCounter = new JLabel("Character Count: 0 / 50 min");
        lblCharCounter.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblCharCounter.setForeground(STATUS_RED);
        scrollContent.add(lblCharCounter);
        scrollContent.add(Box.createVerticalStrut(14));

        scrollContent.add(createSectionHeader("4. Supporting Evidence Attachments (PDF, PNG, JPEG)"));
        scrollContent.add(Box.createVerticalStrut(6));

        docListModel = new DefaultListModel<>();
        lstDocuments = new JList<>(docListModel);
        lstDocuments.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel lbl = new JLabel(
                    " [Attached] " + value.getFileName() + " (" + (value.getFileSizeBytes() / 1024) + " KB)");
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
            lbl.setOpaque(isSelected);
            if (isSelected) {
                lbl.setBackground(ORANGE_SUBTLE);
                lbl.setForeground(ORANGE_PRIMARY);
            }
            return lbl;
        });

        JScrollPane docScroll = new JScrollPane(lstDocuments);
        docScroll.setPreferredSize(new Dimension(0, 80));
        scrollContent.add(docScroll);

        JPanel pnlDocButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        pnlDocButtons.setOpaque(false);
        btnAddDoc = createStandardButton("Attach Document...");
        btnAddDoc.addActionListener(e -> selectAndAddFile());
        btnRemoveDoc = createStandardButton("Remove Selected");
        btnRemoveDoc.addActionListener(e -> {
            int selected = lstDocuments.getSelectedIndex();
            if (selected != -1)
                docListModel.remove(selected);
        });
        pnlDocButtons.add(btnAddDoc);
        pnlDocButtons.add(btnRemoveDoc);
        scrollContent.add(pnlDocButtons);

        JScrollPane centerScrollPane = new JScrollPane(scrollContent);
        centerScrollPane.setBorder(null);
        outerPanel.add(centerScrollPane, BorderLayout.CENTER);

        JPanel pnlSubmitBar = new JPanel(new BorderLayout(8, 0));
        pnlSubmitBar.setBackground(ORANGE_CREAM_BG);
        pnlSubmitBar.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, ORANGE_BORDER),
                new EmptyBorder(12, 16, 12, 16)));

        JPanel pnlAgent = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        pnlAgent.setOpaque(false);
        JLabel lblAgent = new JLabel("Lodging Officer:");
        lblAgent.setFont(new Font("SansSerif", Font.BOLD, 12));
        pnlAgent.add(lblAgent);
        txtAgentId = new JTextField("GOV-OFFICER-449", 14);
        txtAgentId.setBorder(new LineBorder(ORANGE_BORDER));
        pnlAgent.add(txtAgentId);

        btnLodgeAppeal = new JButton("LODGE STATUTORY APPEAL");
        btnLodgeAppeal.setOpaque(true);
        btnLodgeAppeal.setContentAreaFilled(true);
        btnLodgeAppeal.setBorderPainted(false);
        btnLodgeAppeal.setBackground(ORANGE_PRIMARY);
        btnLodgeAppeal.setForeground(Color.WHITE);
        btnLodgeAppeal.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnLodgeAppeal.setFocusPainted(false);
        btnLodgeAppeal.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLodgeAppeal.setBorder(new EmptyBorder(10, 20, 10, 20));
        btnLodgeAppeal.addActionListener(e -> executeAppealSubmission());

        pnlSubmitBar.add(pnlAgent, BorderLayout.WEST);
        pnlSubmitBar.add(btnLodgeAppeal, BorderLayout.EAST);

        outerPanel.add(pnlSubmitBar, BorderLayout.SOUTH);
        outerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, ORANGE_BORDER));

        return outerPanel;
    }

    private JPanel createDocketLedgerPanel() {
        JPanel ledgerPanel = new JPanel(new BorderLayout());
        ledgerPanel.setBackground(ORANGE_CREAM_BG);
        ledgerPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel ledgerHeader = new JPanel(new BorderLayout());
        ledgerHeader.setOpaque(false);
        JLabel lblLedger = new JLabel("Constituency Appeal Ledger");
        lblLedger.setFont(new Font("Georgia", Font.BOLD, 16));
        lblLedger.setForeground(ORANGE_PRIMARY);

        JButton btnPrintReceipt = createStandardButton("View Tracking Receipt");
        btnPrintReceipt.addActionListener(e -> showSelectedReceiptModal());

        ledgerHeader.add(lblLedger, BorderLayout.WEST);
        ledgerHeader.add(btnPrintReceipt, BorderLayout.EAST);
        ledgerHeader.setBorder(new EmptyBorder(0, 0, 10, 0));
        ledgerPanel.add(ledgerHeader, BorderLayout.NORTH);

        String[] columns = { "CADN Docket", "App ID", "Citizen ID", "Constituency", "Status", "Timestamp" };
        docketTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        docketTable = new JTable(docketTableModel);
        JTable table = docketTable;
        table.setRowHeight(28);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.getTableHeader().setBackground(ORANGE_ACCENT);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.setSelectionBackground(ORANGE_SUBTLE);
        table.setSelectionForeground(ORANGE_PRIMARY);
        table.setGridColor(ORANGE_BORDER);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(new LineBorder(ORANGE_BORDER));
        ledgerPanel.add(tableScroll, BorderLayout.CENTER);

        ledgerPanel.add(createAdjudicationPanel(), BorderLayout.SOUTH);

        return ledgerPanel;
    }

    private JPanel createAdjudicationPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(
                new EmptyBorder(10, 0, 0, 0),
                new CompoundBorder(new LineBorder(ORANGE_BORDER), new EmptyBorder(10, 12, 10, 12))));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel lblTitle = new JLabel("5. Appeal Review & Adjudication");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblTitle.setForeground(ORANGE_PRIMARY);

        chkReviewerRole = new JCheckBox("Authorized Reviewer Role");
        chkReviewerRole.setSelected(true);
        chkReviewerRole.setOpaque(false);
        chkReviewerRole.setFont(new Font("SansSerif", Font.BOLD, 11));

        topRow.add(lblTitle, BorderLayout.WEST);
        topRow.add(chkReviewerRole, BorderLayout.EAST);
        panel.add(topRow);
        panel.add(Box.createVerticalStrut(6));

        txtDecisionNote = new JTextArea(2, 30);
        txtDecisionNote.setLineWrap(true);
        txtDecisionNote.setWrapStyleWord(true);
        txtDecisionNote.setBorder(new LineBorder(ORANGE_BORDER));
        txtDecisionNote.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                int len = txtDecisionNote.getText().trim().length();
                lblDecisionCharCounter.setText("Decision Note: " + len + " / 20 min");
                lblDecisionCharCounter.setForeground(len >= 20 ? STATUS_GREEN : STATUS_RED);
            }
        });
        panel.add(new JScrollPane(txtDecisionNote));

        lblDecisionCharCounter = new JLabel("Decision Note: 0 / 20 min");
        lblDecisionCharCounter.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblDecisionCharCounter.setForeground(STATUS_RED);
        panel.add(lblDecisionCharCounter);
        panel.add(Box.createVerticalStrut(6));

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionRow.setOpaque(false);

        chkSimulateSyncError = new JCheckBox("Simulate Enrollment Sync Error");
        chkSimulateSyncError.setOpaque(false);
        chkSimulateSyncError.setFont(new Font("SansSerif", Font.PLAIN, 11));

        btnReviewCase = createStandardButton("Open Case");
        btnReviewCase.addActionListener(e -> handleOpenCase());

        btnApprove = createStandardButton("Approve Appeal");
        btnApprove.addActionListener(e -> handleAdjudicate(true));

        btnDeny = createStandardButton("Deny Appeal");
        btnDeny.addActionListener(e -> handleAdjudicate(false));

        actionRow.add(chkSimulateSyncError);
        actionRow.add(btnReviewCase);
        actionRow.add(btnApprove);
        actionRow.add(btnDeny);
        panel.add(actionRow);

        return panel;
    }

    private String getSelectedCadn() {
        int row = docketTable.getSelectedRow();
        if (row == -1 && docketTableModel.getRowCount() > 0) {
            row = docketTableModel.getRowCount() - 1;
        }
        if (row == -1) {
            JOptionPane.showMessageDialog(rootPanel, "Select an appeal docket from the ledger first.", "Notice",
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return (String) docketTableModel.getValueAt(row, 0);
    }

    private void refreshDocketRowStatus(Appeal appeal) {
        for (int i = 0; i < docketTableModel.getRowCount(); i++) {
            if (appeal.getCadn().equals(docketTableModel.getValueAt(i, 0))) {
                docketTableModel.setValueAt(appeal.getStatus(), i, 4);
                break;
            }
        }
    }

    private void handleOpenCase() {
        String cadn = getSelectedCadn();
        if (cadn == null)
            return;
        try {
            Appeal appeal = appealService.openAppealForReview(cadn, txtAgentId.getText().trim(),
                    chkReviewerRole.isSelected());
            StringBuilder docsSummary = new StringBuilder();
            for (SupportingDocument d : appeal.getSupportingDocs()) {
                docsSummary.append(" - ").append(d.getFileName()).append(" (").append(d.getFileSizeBytes() / 1024)
                        .append(" KB)\n");
            }
            JOptionPane.showMessageDialog(rootPanel,
                    "Docket: " + appeal.getCadn()
                            + "\nStatus: " + appeal.getStatus()
                            + "\nGrounds: " + appeal.getGrounds()
                            + "\nJustification: " + appeal.getJustification()
                            + "\nDocuments:\n" + docsSummary,
                    "Appeal Case Review", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(rootPanel, ex.getMessage(), "Review Blocked", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleAdjudicate(boolean approve) {
        String cadn = getSelectedCadn();
        if (cadn == null)
            return;
        Appeal appeal = appealService.findAppealByCadn(cadn);
        if (appeal == null)
            return;
        ApplicationRecord app = mockDatabase.get(appeal.getOriginalApplicationId());
        String note = txtDecisionNote.getText().trim();
        String reviewerId = txtAgentId.getText().trim();
        boolean isReviewer = chkReviewerRole.isSelected();

        try {
            if (approve) {
                appealService.setSimulateEnrollmentSyncFailure(chkSimulateSyncError.isSelected());
                appealService.approveAppeal(cadn, app, reviewerId, isReviewer, note);
                refreshDocketRowStatus(appeal);
                populateApplicationDetails();
                JOptionPane.showMessageDialog(rootPanel,
                        "Appeal " + cadn + " approved and routed to Enrollment Management.",
                        "Approved", JOptionPane.INFORMATION_MESSAGE);
            } else {
                appealService.denyAppeal(cadn, app, reviewerId, isReviewer, note);
                refreshDocketRowStatus(appeal);
                populateApplicationDetails();
                JOptionPane.showMessageDialog(rootPanel,
                        "Appeal " + cadn + " denied. Application archived until " + app.getArchivedUntil(),
                        "Denied", JOptionPane.INFORMATION_MESSAGE);
            }
            txtDecisionNote.setText("");
            lblDecisionCharCounter.setText("Decision Note: 0 / 20 min");
            lblDecisionCharCounter.setForeground(STATUS_RED);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(rootPanel, ex.getMessage(), "Adjudication Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(ORANGE_SUBTLE);
        statusBar.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ORANGE_BORDER),
                new EmptyBorder(6, 16, 6, 16)));

        String databaseStatus = repo != null ? "CONNECTED" : "DISCONNECTED";
        JLabel lblSystemStatus = new JLabel("C.A.R.R.S. Core Database: " + databaseStatus
                + " | Constituency Gateway: ONLINE");
        lblSystemStatus.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lblSystemStatus.setForeground(TEXT_DARK);

        JLabel lblTime = new JLabel(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        lblTime.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lblTime.setForeground(TEXT_DARK);

        statusBar.add(lblSystemStatus, BorderLayout.WEST);
        statusBar.add(lblTime, BorderLayout.EAST);
        return statusBar;
    }

    private JLabel createSectionHeader(String title) {
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(ORANGE_PRIMARY);
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ORANGE_BORDER),
                new EmptyBorder(2, 0, 4, 0)));
        return lbl;
    }

    private JTextField createReadOnlyField() {
        JTextField tf = new JTextField();
        tf.setEditable(false);
        tf.setBackground(ORANGE_CREAM_BG);
        tf.setBorder(new LineBorder(ORANGE_BORDER));
        return tf;
    }

    private JButton createStandardButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(Color.WHITE);
        btn.setForeground(ORANGE_PRIMARY);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBorder(new CompoundBorder(
                new LineBorder(ORANGE_BORDER),
                new EmptyBorder(6, 12, 6, 12)));
        return btn;
    }

    private void populateApplicationDetails() {
        String selectedId = (String) cmbApplications.getSelectedItem();
        if (selectedId == null)
            return;

        ApplicationRecord record = mockDatabase.get(selectedId);
        if (record != null) {
            txtCitizenId.setText(record.getApplicantCitizenId());
            txtProgram.setText(record.getProgramId());
            txtDecisionDate.setText(record.getDecisionDate() != null
                    ? record.getDecisionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    : "N/A");

            lblStatusBadge.setText(record.getStatus().toUpperCase() + (record.isLocked() ? " [LOCKED]" : ""));
            if ("Rejected".equalsIgnoreCase(record.getStatus())
                    || "Disqualified".equalsIgnoreCase(record.getStatus())) {
                lblStatusBadge.setForeground(STATUS_RED);
            } else {
                lblStatusBadge.setForeground(STATUS_GREEN);
            }
        }
    }

    private void selectAndAddFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Supporting Document");
        chooser.setFileFilter(new FileNameExtensionFilter("Allowed Documents (*.pdf, *.png, *.jpeg, *.jpg)", "pdf",
                "png", "jpeg", "jpg"));

        Window owner = SwingUtilities.getWindowAncestor(rootPanel);
        int result = chooser.showOpenDialog(owner);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String name = file.getName();
            String extension = "";
            int i = name.lastIndexOf('.');
            if (i > 0)
                extension = name.substring(i + 1);

            SupportingDocument doc = new SupportingDocument(name, extension, file.length());
            docListModel.addElement(doc);
        }
    }

    private void executeAppealSubmission() {
        String appId = (String) cmbApplications.getSelectedItem();
        ApplicationRecord appRecord = mockDatabase.get(appId);
        String constituency = txtConstituency.getText().trim();
        boolean isResident = chkVerifiedResident.isSelected();
        String agentId = txtAgentId.getText().trim();
        String justification = txtJustification.getText().trim();

        List<AppealGround> grounds = new ArrayList<>();
        if (chkIncome.isSelected())
            grounds.add(AppealGround.INCORRECT_INCOME_ASSESSMENT);
        if (chkDoc.isSelected())
            grounds.add(AppealGround.OVERLOOKED_DOCUMENTATION);
        if (chkDependency.isSelected())
            grounds.add(AppealGround.UPDATED_HOUSEHOLD_DEPENDENCY);
        if (chkAdminError.isSelected())
            grounds.add(AppealGround.ADMINISTRATIVE_ERROR);

        List<SupportingDocument> docs = new ArrayList<>();
        for (int i = 0; i < docListModel.size(); i++) {
            docs.add(docListModel.get(i));
        }

        try {
            Appeal appeal = appealService.lodgeAppeal(
                    appRecord,
                    constituency,
                    isResident,
                    grounds,
                    justification,
                    docs,
                    agentId);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            docketTableModel.addRow(new Object[] {
                    appeal.getCadn(),
                    appeal.getOriginalApplicationId(),
                    appeal.getApplicantCitizenId(),
                    appeal.getConstituencyCode(),
                    appeal.getStatus().toString(),
                    appeal.getSubmissionTimestamp().format(dtf)
            });

            populateApplicationDetails();

            txtJustification.setText("");
            lblCharCounter.setText("Character Count: 0 / 50 min");
            lblCharCounter.setForeground(STATUS_RED);
            chkIncome.setSelected(false);
            chkDoc.setSelected(false);
            chkDependency.setSelected(false);
            chkAdminError.setSelected(false);
            docListModel.clear();

            int choice = JOptionPane.showOptionDialog(
                    rootPanel,
                    "Statutory Appeal Lodged Successfully!\nDocket CADN: " + appeal.getCadn()
                            + "\nSource application record is now locked.",
                    "Submission Confirmed",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    new Object[] { "View Official Receipt", "Dismiss" },
                    "View Official Receipt");

            if (choice == JOptionPane.YES_OPTION) {
                displayReceiptDialog(appeal.generateReceipt());
            }

        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            JOptionPane.showMessageDialog(rootPanel, ex.getMessage(), "Appeal Filing Disallowed",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(rootPanel, "An unexpected error occurred: " + ex.getMessage(), "System Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showSelectedReceiptModal() {
        if (docketTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(rootPanel, "No appeals registered in the active docket ledger yet.", "Notice",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String cadn = (String) docketTableModel.getValueAt(docketTableModel.getRowCount() - 1, 0);
        Appeal appeal = appealService.findAppealByCadn(cadn);
        if (appeal != null) {
            displayReceiptDialog(appeal.generateReceipt());
        }
    }

    private void displayReceiptDialog(String receiptText) {
        String cleanedReceipt = receiptText.replace("(AC-4)", "").trim();

        JTextArea area = new JTextArea(cleanedReceipt);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setEditable(false);
        area.setBackground(ORANGE_CREAM_BG);
        area.setBorder(new EmptyBorder(12, 12, 12, 12));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(560, 320));

        JOptionPane.showMessageDialog(rootPanel, scroll, "Official Docket Receipt", JOptionPane.PLAIN_MESSAGE);
    }
}

enum AppealGround {
    INCORRECT_INCOME_ASSESSMENT("Incorrect Income Assessment"),
    OVERLOOKED_DOCUMENTATION("Overlooked Documentation"),
    UPDATED_HOUSEHOLD_DEPENDENCY("Household Dependency Update"),
    ADMINISTRATIVE_ERROR("Administrative Error");

    private final String label;

    AppealGround(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}

class ApplicationRecord {
    private final String applicationId;
    private final String applicantCitizenId;
    private final String programId;
    private String status;
    private LocalDateTime decisionDate;
    private LocalDateTime archivedUntil;
    private boolean enrollmentTaskCreated;
    private boolean locked;

    ApplicationRecord(String applicationId, String applicantCitizenId, String programId, String status,
            LocalDateTime decisionDate) {
        this.applicationId = applicationId;
        this.applicantCitizenId = applicantCitizenId;
        this.programId = programId;
        this.status = status;
        this.decisionDate = decisionDate;
        this.enrollmentTaskCreated = false;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getApplicantCitizenId() {
        return applicantCitizenId;
    }

    public String getProgramId() {
        return programId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getDecisionDate() {
        return decisionDate;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDecisionDate(LocalDateTime decisionDate) {
        this.decisionDate = decisionDate;
    }

    public LocalDateTime getArchivedUntil() {
        return archivedUntil;
    }

    public void setArchivedUntil(LocalDateTime archivedUntil) {
        this.archivedUntil = archivedUntil;
    }

    public boolean isEnrollmentTaskCreated() {
        return enrollmentTaskCreated;
    }

    public void setEnrollmentTaskCreated(boolean enrollmentTaskCreated) {
        this.enrollmentTaskCreated = enrollmentTaskCreated;
    }
}

class SupportingDocument {
    private final String fileName;
    private final String extension;
    private final long fileSizeBytes;

    SupportingDocument(String fileName, String extension, long fileSizeBytes) {
        this.fileName = fileName;
        this.extension = extension.toLowerCase().replace(".", "");
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getFileName() {
        return fileName;
    }

    public String getExtension() {
        return extension;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }
}

class Appeal {
    private final String cadn;
    private final String originalApplicationId;
    private final String applicantCitizenId;
    private final String constituencyCode;
    private String status = "APPEAL_PENDING_REVIEW";
    private String decidedBy;
    private String decisionNote;
    private LocalDateTime decisionTimestamp;
    private LocalDateTime archivedUntil;
    private boolean finalized = false;
    private final List<String> reviewAuditTrail = new ArrayList<>();
    private final LocalDateTime submissionTimestamp = LocalDateTime.now();
    private final String justification;
    private final String lodgedBy;
    private final List<AppealGround> grounds;
    private final List<SupportingDocument> documents;

    Appeal(String cadn, ApplicationRecord record, String constituencyCode, List<AppealGround> grounds,
            String justification, List<SupportingDocument> documents, String lodgedBy) {
        this.cadn = cadn;
        this.originalApplicationId = record.getApplicationId();
        this.applicantCitizenId = record.getApplicantCitizenId();
        this.constituencyCode = constituencyCode;
        this.grounds = grounds;
        this.justification = justification;
        this.documents = documents;
        this.lodgedBy = lodgedBy;
    }

    public String getCadn() {
        return cadn;
    }

    public String getOriginalApplicationId() {
        return originalApplicationId;
    }

    public String getApplicantCitizenId() {
        return applicantCitizenId;
    }

    public String getConstituencyCode() {
        return constituencyCode;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getSubmissionTimestamp() {
        return submissionTimestamp;
    }

    public String getJustification() {
        return justification;
    }

    public List<SupportingDocument> getSupportingDocs() {
        return new ArrayList<>(documents);
    }

    public List<AppealGround> getGrounds() {
        return new ArrayList<>(grounds);
    }

    public boolean isFinalized() {
        return finalized;
    }

    public void recordReviewSession(String reviewerId) {
        reviewAuditTrail.add(LocalDateTime.now() + " | Reviewer " + reviewerId + " opened docket " + cadn);
    }

    public void finalizeDecision(String outcomeStatus, String reviewerId, String decisionNote,
            LocalDateTime archivedUntil) {
        if (this.finalized) {
            throw new IllegalStateException(
                    "Finalized Error: Appeal " + cadn + " has already been finalized and cannot be re-adjudicated.");
        }
        this.status = outcomeStatus;
        this.decidedBy = reviewerId;
        this.decisionNote = decisionNote;
        this.decisionTimestamp = LocalDateTime.now();
        this.archivedUntil = archivedUntil;
        this.finalized = true;
    }

    public String generateReceipt() {
        return "C.A.R.R.S. STATUTORY APPEAL RECEIPT\n"
                + "----------------------------------------\n"
                + "CADN Docket   : " + cadn + "\n"
                + "Application ID: " + originalApplicationId + "\n"
                + "Citizen ID    : " + applicantCitizenId + "\n"
                + "Constituency  : " + constituencyCode + "\n"
                + "Status        : " + status + "\n"
                + "Lodged By     : " + lodgedBy + "\n"
                + "Timestamp     : " + submissionTimestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                + "\n"
                + "Grounds       : " + grounds + "\n"
                + "Attachments   : " + documents.size() + "\n"
                + "Justification : " + justification + "\n";
    }
}

class AppealService {
    private final AtomicInteger seq = new AtomicInteger(100);
    private final Map<String, Appeal> byCadn = new LinkedHashMap<>();
    private static final int STATUTORY_APPEAL_WINDOW_DAYS = 30;
    private static final int MIN_JUSTIFICATION_LENGTH = 50;
    private static final int MIN_DECISION_NOTE_LENGTH = 20;
    private static final int DENIAL_ARCHIVE_DAYS = 30;
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpeg", "jpg");
    private boolean simulateEnrollmentSyncFailure = false;

    public void setSimulateEnrollmentSyncFailure(boolean simulateEnrollmentSyncFailure) {
        this.simulateEnrollmentSyncFailure = simulateEnrollmentSyncFailure;
    }

    public Appeal lodgeAppeal(ApplicationRecord record, String constituency, boolean isResident,
            List<AppealGround> grounds, String justification,
            List<SupportingDocument> docs, String agentId) {
        if (record == null)
            throw new IllegalArgumentException("Select a valid application record.");
        if (record.isLocked())
            throw new IllegalStateException("This application is already locked under an appeal.");
        String status = record.getStatus();
        if (!"Rejected".equalsIgnoreCase(status) && !"Disqualified".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Appeals are only allowed for Rejected or Disqualified applications.");
        }
        if (record.getDecisionDate() != null) {
            long days = Duration.between(record.getDecisionDate(), LocalDateTime.now()).toDays();
            if (days > STATUTORY_APPEAL_WINDOW_DAYS) {
                throw new IllegalStateException(
                        "Deadline Expiry Error: Appeal window (30 days) has passed (" + days + " days).");
            }
        }
        if (!isResident)
            throw new SecurityException("Applicant must be a verified constituency resident.");
        if (grounds == null || grounds.isEmpty())
            throw new IllegalArgumentException("Select at least one ground for appeal.");
        if (justification == null || justification.trim().length() < MIN_JUSTIFICATION_LENGTH) {
            throw new IllegalArgumentException("Justification must be at least 50 characters.");
        }
        if (docs == null || docs.isEmpty()) {
            throw new IllegalArgumentException("At least one supporting document is required.");
        }
        for (SupportingDocument doc : docs) {
            if (!ALLOWED_EXTENSIONS.contains(doc.getExtension())) {
                throw new IllegalArgumentException("Unsupported file format: " + doc.getFileName());
            }
            if (doc.getFileSizeBytes() > MAX_FILE_SIZE_BYTES) {
                throw new IllegalArgumentException("File exceeds 10 MB limit: " + doc.getFileName());
            }
        }

        record.setLocked(true);
        String cadn = "CADN-" + seq.incrementAndGet();
        Appeal appeal = new Appeal(cadn, record, constituency, grounds, justification, docs, agentId);
        byCadn.put(cadn, appeal);
        return appeal;
    }

    public Appeal openAppealForReview(String cadn, String reviewerId, boolean hasReviewerPermission) {
        if (!hasReviewerPermission) {
            throw new SecurityException("Access Denied: User '" + reviewerId + "' does not have reviewer permissions.");
        }
        Appeal appeal = byCadn.get(cadn);
        if (appeal == null) {
            throw new IllegalArgumentException("Appeal docket not found: " + cadn);
        }
        appeal.recordReviewSession(reviewerId);
        return appeal;
    }

    public Appeal approveAppeal(String cadn, ApplicationRecord app, String reviewerId, boolean hasReviewerPermission,
            String note) {
        validateAdjudication(cadn, app, reviewerId, hasReviewerPermission, note);
        Appeal appeal = byCadn.get(cadn);
        app.setLocked(false);
        app.setStatus("Approved");
        app.setDecisionDate(LocalDateTime.now());
        if (simulateEnrollmentSyncFailure) {
            throw new IllegalStateException("Enrollment Sync Error: Timeout routing appeal " + cadn
                    + " to Enrollment Management. Retry available.");
        }
        app.setEnrollmentTaskCreated(true);
        appeal.finalizeDecision("APPEAL_APPROVED", reviewerId, note.trim(), null);
        return appeal;
    }

    public Appeal denyAppeal(String cadn, ApplicationRecord app, String reviewerId, boolean hasReviewerPermission,
            String note) {
        validateAdjudication(cadn, app, reviewerId, hasReviewerPermission, note);
        Appeal appeal = byCadn.get(cadn);
        LocalDateTime archiveUntil = LocalDateTime.now().plusDays(DENIAL_ARCHIVE_DAYS);
        app.setArchivedUntil(archiveUntil);
        app.setLocked(true);
        appeal.finalizeDecision("APPEAL_DENIED", reviewerId, note.trim(), archiveUntil);
        return appeal;
    }

    private void validateAdjudication(String cadn, ApplicationRecord app, String reviewerId,
            boolean hasReviewerPermission, String note) {
        if (!hasReviewerPermission) {
            throw new SecurityException("Access Denied: User '" + reviewerId + "' cannot adjudicate appeals.");
        }
        if (app == null) {
            throw new IllegalArgumentException("Application record not found.");
        }
        if (note == null || note.trim().length() < MIN_DECISION_NOTE_LENGTH) {
            throw new IllegalArgumentException(
                    "Decision justification must be at least " + MIN_DECISION_NOTE_LENGTH + " characters.");
        }
        Appeal appeal = byCadn.get(cadn);
        if (appeal == null) {
            throw new IllegalArgumentException("Appeal docket not found: " + cadn);
        }
        if (appeal.isFinalized()) {
            throw new IllegalStateException(
                    "Finalized Error: Appeal " + cadn + " is already finalized and cannot be re-adjudicated.");
        }
    }

    public Appeal findAppealByCadn(String cadn) {
        return byCadn.get(cadn);
    }

}