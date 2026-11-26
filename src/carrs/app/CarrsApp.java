package carrs.app;

import carrs.model.StaffMember;
import carrs.repository.CarrsRepository;
import carrs.repository.MySqlCarrsRepository;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * The main C.A.R.R.S window: a row of tabs (like browser tabs) along the top,
 * one per feature,
 * and a "logged in as" selector. Teammates plug their screens in through
 * registerTabs().
 */
public class CarrsApp extends JFrame {
    private final CarrsRepository repo;
    private final JComboBox<StaffMember> userBox = new JComboBox<>();
    private final JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
    private final List<FeatureTab> features = new ArrayList<>();

    public CarrsApp(CarrsRepository repo) {
        super("C.A.R.R.S - Constituency Assistance Registration & Record System");
        this.repo = repo;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1350, 700);
        setLocationRelativeTo(null);

        // ---- header: title + who is logged in
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        JLabel title = new JLabel("C.A.R.R.S  |  Office of the Member of Parliament, Dr Angela Brown-Burke");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        title.setForeground(new Color(200, 80, 0));
        header.add(title, BorderLayout.WEST);
        JPanel who = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        who.add(new JLabel("Logged in as:"));
        for (StaffMember s : repo.findAllStaff())
            userBox.addItem(s);
        userBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof StaffMember m)
                    setText(m.getName() + "  (" + pretty(m.getRole().name()) + ")");
                return this;
            }
        });
        userBox.addActionListener(e -> notifyTabs());
        who.add(userBox);
        header.add(who, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ---- the browser-style tab strip
        tabs.setFont(tabs.getFont().deriveFont(Font.PLAIN, 14f));
        registerTabs();
        for (FeatureTab f : features) {
            tabs.addTab(null, f.getView());
            int i = tabs.getTabCount() - 1;
            JLabel label = new JLabel(f.getTitle());
            label.setFont(tabs.getFont());
            label.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            tabs.setTabComponentAt(i, label);
        }
        add(tabs, BorderLayout.CENTER);
        notifyTabs();
    }

    /**
     * ===== TEAMMATES: PLUG YOUR SCREEN IN HERE =====
     * Replace your PlaceholderTab with your own class that implements FeatureTab.
     * Example: features.add(new RegisterApplicantTab(repo));
     */
    private void registerTabs() {
        features.add(new PlaceholderTab("Register Applicant", "Justin Bent",
                "FTR-GOV-004 Program Administration and Approval"));
        features.add(new ProgrammeTab(repo));
        features.add(
                new PlaceholderTab("Enrolment Management", "Latania Johnson", "FTR-GOV-008 Enrollment Management"));
        features.add(new AppealsDisputesTab(repo));
        features.add(new ReportTab(repo));
        features.add(new PaymentTab(repo)); // Payment and Benefit Management: Nashella Fairweather
    }

    private void notifyTabs() {
        StaffMember u = (StaffMember) userBox.getSelectedItem();
        for (FeatureTab f : features)
            f.onUserChanged(u);
    }

    private static String pretty(String role) {
        String s = role.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static void main(String[] args) {
        UIManager.put("nimbusBase", new Color(230, 110, 20)); // main orange
        UIManager.put("nimbusBlueGrey", new Color(250, 225, 195)); // soft orange panels
        UIManager.put("control", new Color(255, 245, 232)); // cream background
        UIManager.put("nimbusSelectionBackground", new Color(240, 140, 40)); // selected row/tab
        UIManager.put("nimbusFocus", new Color(200, 80, 0)); // click outline
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new CarrsApp(new MySqlCarrsRepository()).setVisible(true));
    }
}