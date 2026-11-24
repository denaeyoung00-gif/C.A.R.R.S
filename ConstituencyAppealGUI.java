import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class ConstituencyAppealGUI extends JFrame {

    // --- Official Government Palette ---
    private static final Color GOV_NAVY       = new Color(11, 37, 69);
    private static final Color GOV_SLATE_BLUE  = new Color(19, 64, 116);
    private static final Color GOV_LIGHT_BLUE  = new Color(224, 236, 248);
    private static final Color GOV_BG          = new Color(245, 247, 250);
    private static final Color GOV_TEXT_DARK   = new Color(33, 37, 41);
    private static final Color GOV_BORDER      = new Color(203, 213, 225);
    private static final Color GOV_RED         = new Color(166, 28, 28);
    private static final Color GOV_GREEN       = new Color(34, 120, 68);

    // Business Layer References
    private final AppealService appealService = new AppealService();
    private final Map<String, ApplicationRecord> mockDatabase = new LinkedHashMap<>();

    // UI Input Controls
    private JComboBox<String> cmbApplications;
    private JTextField txtCitizenId;
    private JTextField txtProgram;
    private JLabel lblStatusBadge;
    private JTextField txtDecisionDate;
    private JCheckBox chkVerifiedResident;
    private JTextField txtConstituency;
    private JTextField txtAgentId;

    // Grounds Selection
    private JCheckBox chkIncome;
    private JCheckBox chkDoc;
    private JCheckBox chkDependency;
    private JCheckBox chkAdminError;

    // Justification & Files
    private JTextArea txtJustification;
    private JLabel lblCharCounter;
    private DefaultListModel<SupportingDocument> docListModel;
    private JList<SupportingDocument> lstDocuments;

    // Docket Ledger Table
    private DefaultTableModel docketTableModel;

    public ConstituencyAppealGUI() {
        super("C.A.R.R.S. | Constituency Appeals & Disputes Portal");
        seedApplicationData();
        initUI();
    }

    private void seedApplicationData() {
        mockDatabase.put("APP-001", new ApplicationRecord("APP-001", "CIT-1001", "PROG-WELFARE", "Rejected", LocalDateTime.now().minusDays(14)));
        mockDatabase.put("APP-002", new ApplicationRecord("APP-002", "CIT-1002", "PROG-GRANT", "Rejected", LocalDateTime.now().minusDays(35)));
        mockDatabase.put("APP-003", new ApplicationRecord("APP-003", "CIT-1003", "PROG-GRANT", "Approved", LocalDateTime.now().minusDays(5)));
        mockDatabase.put("APP-004", new ApplicationRecord("APP-004", "CIT-1004", "PROG-DISASTER", "Disqualified", LocalDateTime.now().minusDays(4)));
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 840);
        setMinimumSize(new Dimension(1024, 720));
        setLocationRelativeTo(null);
        getContentPane().setBackground(GOV_BG);
        setLayout(new BorderLayout());

        // 1. Top Government Banner Header
        add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Central Split Pane (Intake Form Left, Ledger Right)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createIntakeFormPanel(), createDocketLedgerPanel());
        splitPane.setDividerLocation(620);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        add(splitPane, BorderLayout.CENTER);

        // 3. System Status Bar Footer
        add(createStatusBar(), BorderLayout.SOUTH);

        if (cmbApplications.getItemCount() > 0) {
            cmbApplications.setSelectedIndex(0);
        }
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(GOV_NAVY);
        header.setBorder(new EmptyBorder(16, 24, 16, 24));

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 4));
        titleBlock.setOpaque(false);

        JLabel lblEcosystem = new JLabel("C.A.R.R.S. FILE TRACKING SYSTEM  ::  STATUTORY SERVICES DIVISION");
        lblEcosystem.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblEcosystem.setForeground(new Color(160, 195, 240));

        JLabel lblTitle = new JLabel("Constituency Appeals & Disputes Resolution");
        lblTitle.setFont(new Font("Georgia", Font.BOLD, 20));
        lblTitle.setForeground(Color.WHITE);

        titleBlock.add(lblEcosystem);
        titleBlock.add(lblTitle);

        JPanel badgeBlock = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        badgeBlock.setOpaque(false);

        JLabel lblEnv = new JLabel(" SECURE AUDIT MODE ");
        lblEnv.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblEnv.setOpaque(true);
        lblEnv.setBackground(GOV_SLATE_BLUE);
        lblEnv.setForeground(Color.WHITE);
        lblEnv.setBorder(new EmptyBorder(6, 12, 6, 12));

        badgeBlock.add(lblEnv);

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

        // Section 1: Record Verification
        scrollContent.add(createSectionHeader("1. Underlying Application Verification"));
        scrollContent.add(Box.createVerticalStrut(8));

        JPanel pnlRecord = new JPanel(new GridBagLayout());
        pnlRecord.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        pnlRecord.add(new JLabel("Application ID:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        cmbApplications = new JComboBox<>(mockDatabase.keySet().toArray(new String[0]));
        cmbApplications.setBackground(Color.WHITE);
        cmbApplications.addActionListener(e -> populateApplicationDetails());
        pnlRecord.add(cmbApplications, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        pnlRecord.add(new JLabel("Citizen ID:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        txtCitizenId = createReadOnlyField();
        pnlRecord.add(txtCitizenId, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        pnlRecord.add(new JLabel("Program Type:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        txtProgram = createReadOnlyField();
        pnlRecord.add(txtProgram, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        pnlRecord.add(new JLabel("Current Status:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        lblStatusBadge = new JLabel("UNKNOWN");
        lblStatusBadge.setFont(new Font("SansSerif", Font.BOLD, 12));
        pnlRecord.add(lblStatusBadge, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.3;
        pnlRecord.add(new JLabel("Decision Date:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        txtDecisionDate = createReadOnlyField();
        pnlRecord.add(txtDecisionDate, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.3;
        pnlRecord.add(new JLabel("Constituency Code:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        txtConstituency = new JTextField("CENTRAL");
        txtConstituency.setBorder(new LineBorder(GOV_BORDER));
        pnlRecord.add(txtConstituency, gbc);

        gbc.gridx = 1; gbc.gridy = 6;
        chkVerifiedResident = new JCheckBox("Applicant is a verified constituency resident");
        chkVerifiedResident.setSelected(true);
        chkVerifiedResident.setOpaque(false);
        chkVerifiedResident.setFont(new Font("SansSerif", Font.BOLD, 12));
        pnlRecord.add(chkVerifiedResident, gbc);

        scrollContent.add(pnlRecord);
        scrollContent.add(Box.createVerticalStrut(14));

        // Section 2: Grounds for Appeal
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

        // Section 3: Justification Statement
        scrollContent.add(createSectionHeader("3. Applicant Justification"));
        scrollContent.add(Box.createVerticalStrut(6));

        txtJustification = new JTextArea(4, 30);
        txtJustification.setLineWrap(true);
        txtJustification.setWrapStyleWord(true);
        txtJustification.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(GOV_BORDER),
                new EmptyBorder(6, 6, 6, 6)
        ));
        txtJustification.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                int length = txtJustification.getText().trim().length();
                lblCharCounter.setText("Character Count: " + length + " / 50 min");
                lblCharCounter.setForeground(length >= 50 ? GOV_GREEN : GOV_RED);
            }
        });
        scrollContent.add(new JScrollPane(txtJustification));

        lblCharCounter = new JLabel("Character Count: 0 / 50 min");
        lblCharCounter.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblCharCounter.setForeground(GOV_RED);
        scrollContent.add(lblCharCounter);
        scrollContent.add(Box.createVerticalStrut(14));

        // Section 4: Document Uploads
        scrollContent.add(createSectionHeader("4. Supporting Evidence Attachments (PDF, PNG, JPEG)"));
        scrollContent.add(Box.createVerticalStrut(6));

        docListModel = new DefaultListModel<>();
        lstDocuments = new JList<>(docListModel);
        lstDocuments.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel lbl = new JLabel(" [Attached] " + value.getFileName() + " (" + (value.getFileSizeBytes() / 1024) + " KB)");
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
            lbl.setOpaque(isSelected);
            if (isSelected) {
                lbl.setBackground(GOV_LIGHT_BLUE);
                lbl.setForeground(GOV_NAVY);
            }
            return lbl;
        });

        JScrollPane docScroll = new JScrollPane(lstDocuments);
        docScroll.setPreferredSize(new Dimension(0, 80));
        scrollContent.add(docScroll);

        JPanel pnlDocButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        pnlDocButtons.setOpaque(false);
        JButton btnAddDoc = createStandardButton("Attach Document...");
        btnAddDoc.addActionListener(e -> selectAndAddFile());
        JButton btnRemoveDoc = createStandardButton("Remove Selected");
        btnRemoveDoc.addActionListener(e -> {
            int selected = lstDocuments.getSelectedIndex();
            if (selected != -1) docListModel.remove(selected);
        });
        pnlDocButtons.add(btnAddDoc);
        pnlDocButtons.add(btnRemoveDoc);
        scrollContent.add(pnlDocButtons);

        JScrollPane centerScrollPane = new JScrollPane(scrollContent);
        centerScrollPane.setBorder(null);
        outerPanel.add(centerScrollPane, BorderLayout.CENTER);

        // --- SECTION 5: PINNED SUBMISSION BAR (Always Visible at Bottom) ---
        JPanel pnlSubmitBar = new JPanel(new BorderLayout(8, 0));
        pnlSubmitBar.setBackground(new Color(248, 250, 252));
        pnlSubmitBar.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, GOV_BORDER),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JPanel pnlAgent = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        pnlAgent.setOpaque(false);
        JLabel lblAgent = new JLabel("Lodging Officer ID:");
        lblAgent.setFont(new Font("SansSerif", Font.BOLD, 12));
        pnlAgent.add(lblAgent);
        txtAgentId = new JTextField("GOV-OFFICER-449", 11);
        txtAgentId.setBorder(new LineBorder(GOV_BORDER));
        pnlAgent.add(txtAgentId);

        JButton btnLodgeAppeal = new JButton("LODGE STATUTORY APPEAL");
        btnLodgeAppeal.setOpaque(true);
        btnLodgeAppeal.setContentAreaFilled(true);
        btnLodgeAppeal.setBorderPainted(false);
        btnLodgeAppeal.setBackground(GOV_NAVY);
        btnLodgeAppeal.setForeground(Color.WHITE);
        btnLodgeAppeal.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnLodgeAppeal.setFocusPainted(false);
        btnLodgeAppeal.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLodgeAppeal.setBorder(new EmptyBorder(10, 20, 10, 20));
        btnLodgeAppeal.addActionListener(e -> executeAppealSubmission());

        pnlSubmitBar.add(pnlAgent, BorderLayout.WEST);
        pnlSubmitBar.add(btnLodgeAppeal, BorderLayout.EAST);

        outerPanel.add(pnlSubmitBar, BorderLayout.SOUTH);
        outerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, GOV_BORDER));

        return outerPanel;
    }

    private JPanel createDocketLedgerPanel() {
        JPanel ledgerPanel = new JPanel(new BorderLayout());
        ledgerPanel.setBackground(GOV_BG);
        ledgerPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel ledgerHeader = new JPanel(new BorderLayout());
        ledgerHeader.setOpaque(false);
        JLabel lblLedger = new JLabel("Constituency Appeal Ledger");
        lblLedger.setFont(new Font("Georgia", Font.BOLD, 16));
        lblLedger.setForeground(GOV_NAVY);

        JButton btnPrintReceipt = createStandardButton("View Tracking Receipt");
        btnPrintReceipt.addActionListener(e -> showSelectedReceiptModal());

        ledgerHeader.add(lblLedger, BorderLayout.WEST);
        ledgerHeader.add(btnPrintReceipt, BorderLayout.EAST);
        ledgerHeader.setBorder(new EmptyBorder(0, 0, 10, 0));
        ledgerPanel.add(ledgerHeader, BorderLayout.NORTH);

        String[] columns = {"CADN Docket", "App ID", "Citizen ID", "Constituency", "Status", "Timestamp"};
        docketTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable table = new JTable(docketTableModel);
        table.setRowHeight(28);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.getTableHeader().setBackground(GOV_SLATE_BLUE);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.setSelectionBackground(GOV_LIGHT_BLUE);
        table.setSelectionForeground(GOV_NAVY);
        table.setGridColor(GOV_BORDER);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(new LineBorder(GOV_BORDER));
        ledgerPanel.add(tableScroll, BorderLayout.CENTER);

        return ledgerPanel;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(230, 235, 242));
        statusBar.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, GOV_BORDER),
                new EmptyBorder(6, 16, 6, 16)
        ));

        JLabel lblSystemStatus = new JLabel("C.A.R.R.S. Core Database: CONNECTED | Constituency Gateway: ONLINE");
        lblSystemStatus.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lblSystemStatus.setForeground(GOV_TEXT_DARK);

        JLabel lblTime = new JLabel(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        lblTime.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lblTime.setForeground(GOV_TEXT_DARK);

        statusBar.add(lblSystemStatus, BorderLayout.WEST);
        statusBar.add(lblTime, BorderLayout.EAST);
        return statusBar;
    }

    private JLabel createSectionHeader(String title) {
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(GOV_NAVY);
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, GOV_BORDER),
                new EmptyBorder(2, 0, 4, 0)
        ));
        return lbl;
    }

    private JTextField createReadOnlyField() {
        JTextField tf = new JTextField();
        tf.setEditable(false);
        tf.setBackground(new Color(240, 243, 246));
        tf.setBorder(new LineBorder(GOV_BORDER));
        return tf;
    }

    private JButton createStandardButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(Color.WHITE);
        btn.setForeground(GOV_NAVY);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBorder(new CompoundBorder(
                new LineBorder(GOV_BORDER),
                new EmptyBorder(6, 12, 6, 12)
        ));
        return btn;
    }

    private void populateApplicationDetails() {
        String selectedId = (String) cmbApplications.getSelectedItem();
        if (selectedId == null) return;

        ApplicationRecord record = mockDatabase.get(selectedId);
        if (record != null) {
            txtCitizenId.setText(record.getApplicantCitizenId());
            txtProgram.setText(record.getProgramId());
            txtDecisionDate.setText(record.getDecisionDate() != null
                    ? record.getDecisionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    : "N/A");

            lblStatusBadge.setText(record.getStatus().toUpperCase() + (record.isLocked() ? " [LOCKED]" : ""));
            if ("Rejected".equalsIgnoreCase(record.getStatus()) || "Disqualified".equalsIgnoreCase(record.getStatus())) {
                lblStatusBadge.setForeground(GOV_RED);
            } else {
                lblStatusBadge.setForeground(GOV_GREEN);
            }
        }
    }

    private void selectAndAddFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Supporting Document");
        chooser.setFileFilter(new FileNameExtensionFilter("Allowed Documents (*.pdf, *.png, *.jpeg, *.jpg)", "pdf", "png", "jpeg", "jpg"));

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String name = file.getName();
            String extension = "";
            int i = name.lastIndexOf('.');
            if (i > 0) extension = name.substring(i + 1);

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
        if (chkIncome.isSelected()) grounds.add(AppealGround.INCORRECT_INCOME_ASSESSMENT);
        if (chkDoc.isSelected()) grounds.add(AppealGround.OVERLOOKED_DOCUMENTATION);
        if (chkDependency.isSelected()) grounds.add(AppealGround.UPDATED_HOUSEHOLD_DEPENDENCY);
        if (chkAdminError.isSelected()) grounds.add(AppealGround.ADMINISTRATIVE_ERROR);

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
                    agentId
            );

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            docketTableModel.addRow(new Object[]{
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
            lblCharCounter.setForeground(GOV_RED);
            chkIncome.setSelected(false);
            chkDoc.setSelected(false);
            chkDependency.setSelected(false);
            chkAdminError.setSelected(false);
            docListModel.clear();

            int choice = JOptionPane.showOptionDialog(
                    this,
                    "Statutory Appeal Lodged Successfully!\nDocket CADN: " + appeal.getCadn() + "\nSource application record is now locked.",
                    "Submission Confirmed",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    new Object[]{"View Official Receipt", "Dismiss"},
                    "View Official Receipt"
            );

            if (choice == JOptionPane.YES_OPTION) {
                displayReceiptDialog(appeal.generateReceipt());
            }

        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Appeal Filing Disallowed", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "An unexpected error occurred: " + ex.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showSelectedReceiptModal() {
        DefaultTableModel model = docketTableModel;
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No appeals registered in the active docket ledger yet.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String cadn = (String) model.getValueAt(model.getRowCount() - 1, 0);
        Appeal appeal = appealService.findAppealByCadn(cadn);
        if (appeal != null) {
            displayReceiptDialog(appeal.generateReceipt());
        }
    }

    private void displayReceiptDialog(String receiptText) {
        // Strip internal code tag from receipt output display
        String cleanedReceipt = receiptText.replace("(AC-4)", "").trim();

        JTextArea area = new JTextArea(cleanedReceipt);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setEditable(false);
        area.setBackground(new Color(250, 252, 255));
        area.setBorder(new EmptyBorder(12, 12, 12, 12));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(560, 320));

        JOptionPane.showMessageDialog(this, scroll, "Official Docket Receipt", JOptionPane.PLAIN_MESSAGE);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            ConstituencyAppealGUI gui = new ConstituencyAppealGUI();
            gui.setVisible(true);
        });
    }
}
