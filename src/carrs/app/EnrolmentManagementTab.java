package carrs.app;

import carrs.model.*;
import carrs.repository.CarrsRepository;

import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Enrolment Management (Latania Johnson). Enrolments are created automatically when an application is
 * approved in the Register Applicant tab; this tab lets staff view, search, track and edit them.
 * Everything is read from and saved to the database through the repository.
 */
public class EnrolmentManagementTab implements FeatureTab {
    private static final Color CREAM = new Color(255, 253, 208);
    private static final Color TEXT_DARK = new Color(33, 37, 41);
    private static final Color BUTTON_LIGHT_ORG = new Color(255, 153, 0);
    private static final Color ORANGE_BORDER = new Color(225, 180, 145);
    private static final Color ORANGE_SOFT = new Color(255, 214, 165);
    private static final Color CARD_WHITE = new Color(255, 255, 255);

    private static final String CARD_VIEW = "VIEW ENROLLMENT";
    private static final String CARD_EDIT = "EDIT ENROLMENT";
    private static final String CARD_TRACK = "TRACK";
    private static final String CARD_SEARCH = "SEARCH";

    private final CarrsRepository repo;
    private final JPanel rootPanel = new JPanel(new BorderLayout());
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardsPanel = new JPanel(cardLayout);

    private DefaultTableModel viewTableModel;
    private DefaultTableModel searchModel;
    private JTextField txtSearchQuery;
    private JLabel lblCount;

    private JTextField txtEnrollmentUBI, txtEditPhone, txtEditAddress, txtTrack;
    private JComboBox<EnrolmentStatus> selStatus;
    private JLabel lblEditWho;

    public EnrolmentManagementTab(CarrsRepository repo) {
        this.repo = repo;
        rootPanel.setBackground(Color.WHITE);
        rootPanel.add(navBar(), BorderLayout.NORTH);

        cardsPanel.add(buildViewPanel(), CARD_VIEW);
        cardsPanel.add(buildEditPanel(), CARD_EDIT);
        cardsPanel.add(buildTrackPanel(), CARD_TRACK);
        cardsPanel.add(buildSearchPanel(), CARD_SEARCH);
        rootPanel.add(cardsPanel, BorderLayout.CENTER);
        cardLayout.show(cardsPanel, CARD_VIEW);

        // reload from the database every time the tab is opened, so new approvals appear straight away
        rootPanel.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && rootPanel.isShowing()) refreshAll();
        });
        refreshAll();
    }

    // ---------------------------------------------------------------- data helpers
    private List<EnrolmentRow> loadRows() {
        try {
            return repo.findEnrolmentRows();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(rootPanel, "Could not load enrolments: " + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
            return new ArrayList<>();
        }
    }

    /** Accepts "5", "ENR5" or "enr-5". Returns null if it is not an enrolment number. */
    private static Integer parseId(String text) {
        String digits = text.trim().replaceAll("(?i)^enr[-\\s]*", "");
        try { return Integer.valueOf(digits); } catch (NumberFormatException e) { return null; }
    }

    private EnrolmentRow findById(String text) {
        Integer id = parseId(text);
        if (id == null) return null;
        for (EnrolmentRow r : loadRows()) if (r.getEnrolmentId() == id) return r;
        return null;
    }

    private static String enrId(EnrolmentRow r) { return "ENR" + r.getEnrolmentId(); }

    private void refreshAll() {
        refreshViewTable();
        runSearch();
    }

    // ---------------------------------------------------------------- navigation
    private JPanel navBar() {
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 10));
        nav.setBackground(ORANGE_SOFT);
        nav.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ORANGE_BORDER));

        JButton btnView = makeNavButton("View Enrollments");
        JButton btnEdit = makeNavButton("Edit Enrollment");
        JButton btnTrack = makeNavButton("Track Enrollment");
        JButton btnSearch = makeNavButton("Search For Enrollment");

        btnView.addActionListener(e -> { refreshViewTable(); cardLayout.show(cardsPanel, CARD_VIEW); });
        btnEdit.addActionListener(e -> cardLayout.show(cardsPanel, CARD_EDIT));
        btnTrack.addActionListener(e -> cardLayout.show(cardsPanel, CARD_TRACK));
        btnSearch.addActionListener(e -> { runSearch(); cardLayout.show(cardsPanel, CARD_SEARCH); });

        nav.add(btnView); nav.add(btnEdit); nav.add(btnTrack); nav.add(btnSearch);
        return nav;
    }

    private JButton makeActionButton(String label) {
        JButton btn = new JButton(label);
        btn.setBackground(BUTTON_LIGHT_ORG);
        btn.setForeground(TEXT_DARK);
        btn.setFocusPainted(false);
        return btn;
    }

    private JButton makeNavButton(String label) {
        JButton btn = new JButton(label);
        btn.setBackground(new Color(255, 255, 204));
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 39, 8, 39));
        return btn;
    }

    private static DefaultTableModel readOnlyModel(String[] cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    // ---------------------------------------------------------------- view
    private JPanel buildViewPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(CARD_WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        viewTableModel = readOnlyModel(new String[]{"Enrollment ID", "Applicant ID", "TRN", "Name", "Program",
                "Status", "Address", "Phone Number", "Enrolled On"});
        panel.add(new JScrollPane(new JTable(viewTableModel)), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setBackground(CARD_WHITE);
        lblCount = new JLabel(" ");
        JButton refresh = makeActionButton("Refresh");
        refresh.addActionListener(e -> refreshAll());
        bottom.add(refresh);
        bottom.add(lblCount);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshViewTable() {
        viewTableModel.setRowCount(0);
        List<EnrolmentRow> rows = loadRows();
        for (EnrolmentRow r : rows) {
            viewTableModel.addRow(new Object[]{enrId(r), r.getApplicantId(), r.getTrn(), r.getApplicantName(),
                    r.getProgrammeName(), r.getStatus(), r.getAddress(), r.getPhone(), r.getEnrolledOn()});
        }
        lblCount.setText(rows.size() + " enrolment(s)");
    }

    // ---------------------------------------------------------------- edit
    private JPanel buildEditPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 4));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBackground(CREAM);
        txtEnrollmentUBI = new JTextField(15);
        JButton btnLoad = makeActionButton("Load Enrollment");
        top.add(new JLabel("Enrollment ID: "));
        top.add(txtEnrollmentUBI);
        top.add(btnLoad);

        lblEditWho = new JLabel(" ");
        txtEditPhone = new JTextField();
        txtEditAddress = new JTextField();
        selStatus = new JComboBox<>(EnrolmentStatus.values());

        JPanel form = new JPanel(new GridLayout(4, 2, 5, 5));
        form.setBackground(CREAM);
        form.add(new JLabel("Applicant: "));
        form.add(lblEditWho);
        form.add(new JLabel("Phone Number: "));
        form.add(txtEditPhone);
        form.add(new JLabel("Address: "));
        form.add(txtEditAddress);
        form.add(new JLabel("Status: "));
        form.add(selStatus);

        JButton btnSave = makeActionButton("Save Enrollment");

        btnLoad.addActionListener(e -> {
            EnrolmentRow found = findById(txtEnrollmentUBI.getText());
            if (found == null) {
                JOptionPane.showMessageDialog(rootPanel, "No Enrollment Found For ID: " + txtEnrollmentUBI.getText().trim());
                return;
            }
            lblEditWho.setText(found.getApplicantName() + " - " + found.getProgrammeName());
            txtEditPhone.setText(found.getPhone());
            txtEditAddress.setText(found.getAddress());
            selStatus.setSelectedItem(found.getStatus());
        });

        btnSave.addActionListener(e -> {
            EnrolmentRow found = findById(txtEnrollmentUBI.getText());
            if (found == null) {
                JOptionPane.showMessageDialog(rootPanel, "Load a valid enrollment first.");
                return;
            }
            try {
                repo.updateEnrolmentRecord(found.getEnrolmentId(), (EnrolmentStatus) selStatus.getSelectedItem(),
                        txtEditPhone.getText().trim(), txtEditAddress.getText().trim());
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(rootPanel, "Could not save: " + ex.getMessage(),
                        "Database error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            refreshAll();
            JOptionPane.showMessageDialog(rootPanel, "Changes Saved.");
        });

        panel.add(top, BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        panel.add(btnSave, BorderLayout.SOUTH);
        return panel;
    }

    // ---------------------------------------------------------------- track
    private JPanel buildTrackPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(CREAM);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBackground(CREAM);
        txtTrack = new JTextField(15);
        JButton btnTrack = makeActionButton("Track Status");
        top.add(new JLabel("Enrollment ID or Applicant ID:"));
        top.add(txtTrack);
        top.add(btnTrack);

        JTextArea result = new JTextArea(8, 40);
        result.setEditable(false);

        btnTrack.addActionListener(e -> {
            String query = txtTrack.getText().trim();
            if (query.isEmpty()) { result.setText("Please enter an Enrollment ID or Applicant ID."); return; }

            Integer id = parseId(query);
            List<EnrolmentRow> rows = loadRows();

            if (id != null) {
                for (EnrolmentRow r : rows) {
                    if (r.getEnrolmentId() == id) {
                        result.setText("Enrollment ID: " + enrId(r) + "\n"
                                + "Applicant: " + r.getApplicantName() + " (" + r.getApplicantId() + ")\n"
                                + "Program: " + r.getProgrammeName() + "\n"
                                + "Status: " + r.getStatus() + "\n"
                                + "Enrolment Date: " + r.getEnrolledOn());
                        return;
                    }
                }
                StringBuilder sb = new StringBuilder();
                for (EnrolmentRow r : rows) {
                    if (r.getApplicantId() == id) {
                        sb.append("- ").append(enrId(r)).append(" | ").append(r.getProgrammeName())
                          .append(" | Status: ").append(r.getStatus())
                          .append(" | Date: ").append(r.getEnrolledOn()).append("\n");
                    }
                }
                if (sb.length() > 0) {
                    result.setText("Enrollments for Applicant " + id + ":\n\n" + sb);
                    return;
                }
            }
            result.setText("No enrollment records found for: " + query);
        });

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(result), BorderLayout.CENTER);
        return panel;
    }

    // ---------------------------------------------------------------- search
    private JPanel buildSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(CREAM);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBackground(CREAM);
        txtSearchQuery = new JTextField(18);
        JButton btnSearch = makeActionButton("Search");
        top.add(new JLabel("Search (Name, TRN, Program or ID): "));
        top.add(txtSearchQuery);
        top.add(btnSearch);

        searchModel = readOnlyModel(new String[]{"Enrollment ID", "Applicant ID", "Name", "TRN", "Program", "Status"});
        btnSearch.addActionListener(e -> runSearch());
        txtSearchQuery.addActionListener(e -> runSearch());

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(new JTable(searchModel)), BorderLayout.CENTER);
        return panel;
    }

    private void runSearch() {
        if (searchModel == null) return;
        String q = txtSearchQuery.getText().trim().toLowerCase();
        searchModel.setRowCount(0);
        for (EnrolmentRow r : loadRows()) {
            if (q.isEmpty()
                    || enrId(r).toLowerCase().contains(q)
                    || String.valueOf(r.getApplicantId()).equals(q)
                    || r.getApplicantName().toLowerCase().contains(q)
                    || r.getProgrammeName().toLowerCase().contains(q)
                    || r.getTrn().toLowerCase().contains(q)) {
                searchModel.addRow(new Object[]{enrId(r), r.getApplicantId(), r.getApplicantName(), r.getTrn(),
                        r.getProgrammeName(), r.getStatus()});
            }
        }
    }

    // ---------------------------------------------------------------- FeatureTab
    @Override public String getTitle() { return "Enrolment Management"; }
    @Override public JComponent getView() { return rootPanel; }
    @Override public void onUserChanged(StaffMember user) { refreshAll(); }
}
