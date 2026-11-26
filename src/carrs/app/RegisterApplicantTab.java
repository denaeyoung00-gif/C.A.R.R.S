package carrs.app;

import carrs.model.*;
import carrs.repository.CarrsRepository;

import java.awt.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class RegisterApplicantTab implements FeatureTab {
    private final CarrsRepository repo;
    private final JPanel view = new JPanel(new BorderLayout(10, 10));
    private final JPanel root = new JPanel(new BorderLayout());   // form on top, approvals underneath
    private final ApprovalTab approval;
    private StaffMember user;

    private final JTextField nameField = new JTextField(25);
    private final JComboBox<String> idTypeBox = new JComboBox<>(new String[]{"TRN", "NATIONAL_ID", "DRIVERS_LICENCE"});
    private final JTextField idNumberField = new JTextField(15);
    private final JTextField dobField = new JTextField(12);
    private final JTextField contactField = new JTextField(15);
    private final JTextField addressField = new JTextField(30);
    private final JComboBox<String> employmentBox = new JComboBox<>(new String[]{"EMPLOYED", "UNEMPLOYED", "SELF_EMPLOYED"});
    private final JTextField incomeField = new JTextField(12);
    private final JTextField dependentsField = new JTextField(5);
    private final JTextField householdField = new JTextField(5);

    private final JComboBox<Programme> programmeBox = new JComboBox<>();
    private final JTextField assistanceField = new JTextField(30);
    private final JTextArea reasonArea = new JTextArea(4, 30);
    private final JTextField docField = new JTextField(30);

    public RegisterApplicantTab(CarrsRepository repo) {
        this.repo = repo;
        this.approval = new ApprovalTab(repo);
        view.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JPanel identity = new JPanel(new GridLayout(0, 2, 5, 5));
        identity.setBorder(BorderFactory.createTitledBorder("Personal Information"));
        identity.add(new JLabel("Full Name:")); identity.add(nameField);
        identity.add(new JLabel("ID Type:")); identity.add(idTypeBox);
        identity.add(new JLabel("ID Number:")); identity.add(idNumberField);
        identity.add(new JLabel("DOB (YYYY-MM-DD):")); identity.add(dobField);
        identity.add(new JLabel("Contact Number:")); identity.add(contactField);
        identity.add(new JLabel("Address:")); identity.add(addressField);
        identity.add(new JLabel("Employment Status:")); identity.add(employmentBox);
        identity.add(new JLabel("Monthly Income ($):")); identity.add(incomeField);
        identity.add(new JLabel("Dependents:")); identity.add(dependentsField);
        identity.add(new JLabel("Household Size:")); identity.add(householdField);
        form.add(identity);
        form.add(Box.createVerticalStrut(10));

        JPanel appInfo = new JPanel(new GridLayout(0, 2, 5, 5));
        appInfo.setBorder(BorderFactory.createTitledBorder("Application Info"));
        appInfo.add(new JLabel("Programme:"));
        programmeBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof Programme) setText(((Programme) v).getName());
                return this;
            }
        });
        appInfo.add(programmeBox);
        appInfo.add(new JLabel("Assistance Requested:")); appInfo.add(assistanceField);
        appInfo.add(new JLabel("Reason for Applying:")); appInfo.add(new JScrollPane(reasonArea));
        appInfo.add(new JLabel("Supporting Doc:"));
        JPanel docPanel = new JPanel(new BorderLayout(5, 0));
        docPanel.add(docField, BorderLayout.CENTER);
        JButton pickDoc = new JButton("Browse");
        pickDoc.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Docs", "pdf", "png", "jpg", "jpeg"));
            if (chooser.showOpenDialog(view) == JFileChooser.APPROVE_OPTION)
                docField.setText(chooser.getSelectedFile().getAbsolutePath());
        });
        docPanel.add(pickDoc, BorderLayout.EAST);
        appInfo.add(docPanel);
        form.add(appInfo);

        view.add(new JScrollPane(form), BorderLayout.CENTER);

        JButton saveBtn = new JButton("Save Application");
        saveBtn.addActionListener(e -> onSave());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(saveBtn);
        view.add(south, BorderLayout.SOUTH);

        programmeBox.addActionListener(e -> {
            Programme p = (Programme) programmeBox.getSelectedItem();
            if (p != null) assistanceField.setText(p.getName());
        });

        // put the Approve Applications table underneath the form
        approval.getView().setBorder(BorderFactory.createTitledBorder("Approve Applications"));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, view, approval.getView());
        split.setResizeWeight(0.65);
        split.setContinuousLayout(true);
        root.add(split, BorderLayout.CENTER);

        // every time this tab is opened, reload the programme list so newly created programmes appear
        root.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && root.isShowing())
                reloadProgrammes();
        });
    }

    @Override public String getTitle() { return "Register Applicant"; }
    @Override public JComponent getView() { return root; }

    @Override
    public void onUserChanged(StaffMember u) {
        this.user = u;
        approval.onUserChanged(u);
        reloadProgrammes();
    }

    /** Re-reads the programme list from the database, keeping the chosen programme selected if it is still there. */
    private void reloadProgrammes() {
        Programme current = (Programme) programmeBox.getSelectedItem();
        int keepId = current == null ? -1 : current.getId();
        programmeBox.removeAllItems();
        Programme toSelect = null;
        for (Programme p : repo.findAllProgrammes()) {
            programmeBox.addItem(p);
            if (p.getId() == keepId) toSelect = p;
        }
        if (toSelect != null) programmeBox.setSelectedItem(toSelect);
    }

    private void onSave() {
        if (user == null) return;

        String name = nameField.getText().trim();
        String idNum = idNumberField.getText().trim();
        Programme prog = (Programme) programmeBox.getSelectedItem();

        if (name.isEmpty() || idNum.isEmpty()) {
            JOptionPane.showMessageDialog(view, "Name and ID Number are required.");
            return;
        }
        if (prog == null) {
            JOptionPane.showMessageDialog(view, "You must select a programme.");
            return;
        }

        String idType = (String) idTypeBox.getSelectedItem();

        // Safe parsing
        String dob = dobField.getText().trim();
        if (dob.isEmpty()) dob = null;
        else if (!dob.matches("\\d{4}-\\d{2}-\\d{2}")) {
            JOptionPane.showMessageDialog(view, "Date of Birth must be in YYYY-MM-DD format (e.g. 2000-01-01).");
            return;
        }

        String income = incomeField.getText().trim().isEmpty() ? "0" : incomeField.getText().trim();
        String dependents = dependentsField.getText().trim().isEmpty() ? "0" : dependentsField.getText().trim();
        String household = householdField.getText().trim().isEmpty() ? "0" : householdField.getText().trim();

        try {
            Applicant a = new Applicant(0, name);
            a.with("id_type", idType).with("id_number", idNum)
                    .with("date_of_birth", dob)
                    .with("contact_number", contactField.getText().trim())
                    .with("address", addressField.getText().trim())
                    .with("employment_status", employmentBox.getSelectedItem().toString())
                    .with("monthly_income", income)
                    .with("dependents", dependents)
                    .with("household_size", household);

            String refNo = "APP-" + System.currentTimeMillis();
            ProgrammeApplication app = new ProgrammeApplication(0, refNo, 0, prog.getId(),
                    assistanceField.getText().trim(), reasonArea.getText().trim(),
                    ApplicationStatus.COMPLETED, null, null, null, user.getId(), null, docField.getText().trim());

            repo.createApplicantAndApplication(a, app, user);
            JOptionPane.showMessageDialog(view, "Application saved successfully!");
            clear();
            approval.onUserChanged(user);
        } catch (Exception ex) {
            ex.printStackTrace(); // <--- CRITICAL: Look at your VS Code terminal after clicking save
            JOptionPane.showMessageDialog(view, "Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clear() {
        nameField.setText(""); idNumberField.setText(""); dobField.setText("");
        contactField.setText(""); addressField.setText(""); incomeField.setText("");
        dependentsField.setText(""); householdField.setText(""); reasonArea.setText(""); docField.setText("");
    }
}
