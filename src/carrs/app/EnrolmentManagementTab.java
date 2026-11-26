package carrs.app;

import carrs.model.*;
import carrs.repository.CarrsRepository;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.awt.Transparency;

import static javax.swing.JOptionPane.*;


public class EnrolmentManagementTab extends EnrolmentManagement implements FeatureTab {
    private static final Color CREAM = new Color (255, 253, 208);
    private static final Color TEXT_DARK = new Color(33, 37, 41);
    private static final Color BUTTON_LIGHT_ORG = new Color(255, 153, 0);
    private static final Color ORANGE_BORDER = new Color(225, 180, 145);
    private static final Color ORANGE_SOFT = new Color(255, 214, 165);
    private static final Color CARD_WHITE = new Color(255, 255, 255);

    private DefaultTableModel viewTableModel;

   // private EnrolmentManagement enrolmentManagement = new EnrolmentManagement();
   // private final Map<String, ProgramEnrolment> enrolment = new LinkedHashMap<>();
  //  private String databaseError;

    private JTextField txtFirstName;
    private JTextField txtLastName;
    private JTextField txtAddress;
    private JTextField txtTrn;
    private JTextField txtProgramName;
    private JTextField txtNum;
    private JTextField txtEmail;
    private JTextField txtDob;
    private JTextField txtEnrollmentUBI;
    private JTextField txtTrack;
    private JTextField txtEditEmail;
    private JTextField txtEditPhone;
    private JTextField txtEditAddress;

    private JButton btnSubmit;
    private JButton btnSearch;
    private JButton btnLoad;
    private JButton btnSave;
    private JButton btnTrack;


    private static final String CARD_VIEW = "VIEW ENROLLMENT";
    private static final String CARD_CREATE = "CREATE ENROLLMENT";
    private static final String CARD_EDIT = "EDIT ENROLMENT";
    private static final String CARD_TRACK = "TRACK";
    private static final String CARD_SEARCH = "SEARCH";

    private final CarrsRepository repo;
    private final JPanel rootPanel = new JPanel(new BorderLayout());
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardsPanel = new JPanel(cardLayout);

    public EnrolmentManagementTab (CarrsRepository repo){
        this.repo = repo;
        rootPanel.setBackground(Color.WHITE);

        rootPanel.add(NavBar(), BorderLayout.NORTH);

        cardsPanel.add(buildViewPanel(), CARD_VIEW);
        cardsPanel.add(buildCreatePanel(), CARD_CREATE);
        cardsPanel.add(buildEditPanel(), CARD_EDIT);
        cardsPanel.add(buildTrackPanel(), CARD_TRACK);
        cardsPanel.add(buildSearchPanel(), CARD_SEARCH);

        rootPanel.add(cardsPanel, BorderLayout.CENTER);

        cardLayout.show(cardsPanel, CARD_VIEW);
    }

    private JPanel NavBar(){
        JPanel nav = new JPanel (new FlowLayout(FlowLayout.CENTER, 50, 10));
        nav.setBackground(ORANGE_SOFT);
        nav.setBorder(BorderFactory.createMatteBorder(0,0,1,0, ORANGE_BORDER));

        JButton btnView = makeNavButton ("View Enrollments");
        JButton btnCreate = makeNavButton ("Create Enrollment");
        JButton btnEdit = makeNavButton ("Edit Enrollment");
        JButton btnTrack = makeNavButton ("Track Enrollment");
        JButton btnSearch = makeNavButton ("Search For Enrollment");

        btnView.addActionListener(e -> {
            refreshViewTable();
            cardLayout.show(cardsPanel, CARD_VIEW);
        });

        btnCreate.addActionListener(e -> cardLayout.show(cardsPanel, CARD_CREATE));
        btnEdit.addActionListener(e -> cardLayout.show(cardsPanel, CARD_EDIT));
        btnTrack.addActionListener(e -> cardLayout.show(cardsPanel, CARD_TRACK));
        btnSearch.addActionListener(e -> cardLayout.show(cardsPanel, CARD_SEARCH));

        nav.add(btnView);
        nav.add(btnCreate);
        nav.add(btnEdit);
        nav.add(btnTrack);
        nav.add(btnSearch);
        return nav;
    }

    private JButton makeActionButton(String label) {
        JButton btn = new JButton(label);
        btn.setBackground(BUTTON_LIGHT_ORG);
        btn.setForeground(TEXT_DARK);
        btn.setFocusPainted(false);
        return btn;

    }

    private JButton makeNavButton(String label){
        JButton btn = new JButton(label);
        btn.setBackground(new Color(255, 255, 204));
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 39, 8, 39));
        return btn;
    }

    private JPanel buildViewPanel(){
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(CARD_WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));

        String[] columns = {"Enrollment ID", "TRN", "Constituent Name", "Program", "Status", "Address", "Phone Number", "Email"};
        viewTableModel = new DefaultTableModel(columns, 0){
            @Override
            public boolean isCellEditable (int row, int column){
                return false;
            }
        };

        JTable table = new JTable(viewTableModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void refreshViewTable() {
        viewTableModel.setRowCount(0);
        for (ProgramEnrolment m : getEnrolments()) {
            Constituent c = m.getConstituent();
            String fullName = c.getFirstName() + " " +c.getLastName();
            viewTableModel.addRow(new Object[]{
                    m.getEnrolmentId(), c.getTRN(), fullName, m.getProgram().getProgramName(),
                    m.status(), c.getAddress(), c.getPhoneNumber(), c.getEmail()
            });;
        }

    }

    private JPanel buildCreatePanel() {
        JPanel panel = new JPanel(new GridLayout(6, 2, 5, 4));
        panel.setBackground(CREAM);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JComboBox<Character> selGender = new JComboBox<>(new Character[]{'F', 'M', 'O'});
        txtFirstName = new JTextField();
        txtLastName = new JTextField();
        txtAddress = new JTextField();
        txtTrn = new JTextField();
        txtProgramName = new JTextField();
        txtNum = new JTextField();
        txtDob = new JTextField();
        txtEmail = new JTextField();

        btnSubmit = makeActionButton("Submit Enrollment");
        panel.add(new JLabel("First Name; "));
        panel.add(txtFirstName);
        panel.add(new JLabel("Last Name; "));
        panel.add(txtLastName);
        panel.add(new JLabel("Address: "));
        panel.add(txtAddress);
        panel.add(new JLabel("TRN:"));
        panel.add(txtTrn);
        panel.add(new JLabel("Program: "));
        panel.add(txtProgramName);
        panel.add(new JLabel("Gender: "));
        panel.add(selGender);
        panel.add(new JLabel("Phone Number"));
        panel.add(txtNum);
        panel.add(new JLabel("Date of Birth; "));
        panel.add(txtDob);
        panel.add(new JLabel("Email; "));
        panel.add(txtEmail);
        panel.add(new JLabel(""));
        panel.add(btnSubmit);


        btnSubmit.addActionListener(e -> {
            String firstName = txtFirstName.getText().trim();
            String lastName = txtLastName.getText().trim();
            String address = txtAddress.getText().trim();
            String trn = txtTrn.getText().trim();
            String programName = txtProgramName.getText().trim();
            char gender = (Character) selGender.getSelectedItem();
            String phoneNumber = txtNum.getText().trim();
            String dOB = txtDob.getText().trim();
            String email = txtEmail.getText().trim();

            if (firstName.isEmpty() || lastName.isEmpty() || address.isEmpty() || trn.isEmpty() || programName.isEmpty()
            || dOB.isEmpty()){
                JOptionPane.showMessageDialog(rootPanel, "Please fill in the missing required fields.");
                return;
            }

            Constituent constituent = enrollConstituent(firstName, lastName, email, trn, phoneNumber, address, gender, dOB);

            EnrolmentProgramData selectedProgram = new EnrolmentProgramData(programName, 1, 50, 0, "","","");
            ProgramEnrolment enrolment = enrollToProgram(constituent, selectedProgram);

            if (enrolment != null){
                JOptionPane.showMessageDialog(rootPanel, "Enrollment Successfully Created!\nEnrollment ID:"
                + enrolment.getEnrolmentId() + "Constituent ID: "+ constituent.getApplicantId());

                txtFirstName.setText("");
                txtLastName.setText("");
                txtAddress.setText("");
                txtTrn.setText("");
                txtProgramName.setText("");
                txtNum.setText("");
                txtDob.setText("");
                txtEmail.setText("");
            } else {
                JOptionPane.showMessageDialog(rootPanel, "Could Not Complete Enrollment");
            }
        });
        return panel;
    }

    private JPanel buildEditPanel (){
        JPanel panel1 = new JPanel(new BorderLayout(5, 4));
        panel1.setBackground(Color.WHITE);
        panel1.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBackground(CREAM);
        btnLoad = makeActionButton("Load Enrollment");

        txtEnrollmentUBI = new JTextField(15);
        btnLoad = makeActionButton("Load Enrollment");
        txtEditEmail = new JTextField();
        txtEditPhone = new JTextField();
        txtEditAddress = new JTextField();
        JComboBox<EnrolmentStatus> selStatus = new JComboBox<>(EnrolmentStatus.values());
        btnSave = makeActionButton("Save Enrollment");

        top.add(new JLabel("Enrollment ID: "));
        top.add(txtEnrollmentUBI);
        top.add(btnLoad);

        JPanel form = new JPanel(new GridLayout(4,2,5,5));
        form.setBackground(CREAM);
        form.add(new JLabel("Email: "));
        form.add(txtEditEmail);
        form.add(new JLabel ("Phone Number: "));
        form.add(txtEditPhone);
        form.add(new JLabel("Address: "));
        form.add(txtEditAddress);
        form.add(new JLabel("Status: "));
        form.add(selStatus);

        btnSave = makeActionButton("Save Enrollment");

        btnLoad.addActionListener(e -> { String id = txtEnrollmentUBI.getText().trim();
            ProgramEnrolment found = searchEnrolmentByID(id);
            if(found == null) {
                JOptionPane.showMessageDialog(rootPanel, "No Enrollment Found For ID: " + id);
                return;
            }

            Constituent c = found.getConstituent();
            txtEditEmail.setText(c.getEmail());
            txtEditPhone.setText(c.getPhoneNumber());
            txtEditAddress.setText(c.getAddress());
            selStatus.setSelectedItem(found.status());
        });

        btnSave.addActionListener(e -> {
            String id = txtEnrollmentUBI.getText().trim();
            ProgramEnrolment found = searchEnrolmentByID(id);
            if (found == null) {
                JOptionPane.showMessageDialog(rootPanel, "Load a valid enrollment. ");
                return;
            }
            String applicantId = found.getConstituent().getApplicantId();
            editConstituentRecord(applicantId, txtEditEmail.getText().trim(), txtEditPhone.getText().trim(),
                    txtEditAddress.getText().trim());

            found.updateStatus((EnrolmentStatus) selStatus.getSelectedItem());
            refreshViewTable();
            JOptionPane.showMessageDialog(rootPanel, "Changes Saved.");

        });

        panel1.add(top, BorderLayout.NORTH);
        panel1.add(form, BorderLayout.CENTER);
        panel1.add(btnSave, BorderLayout.SOUTH);
        return panel1;
    }

    private JPanel buildTrackPanel(){
        JPanel panel0 = new JPanel(new BorderLayout(10,10));
        panel0.setBackground(CREAM);
        panel0.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBackground(CREAM);
        txtTrack = new JTextField(15);
        btnTrack = makeActionButton("Track Status");
        top.add(new JLabel("Enrollment UBI or Constituent ID:"));
        top.add(txtTrack);
        top.add(btnTrack);

        txtTrack = new JTextField(15);
        JTextArea result = new JTextArea(8, 40);
        result.setEditable(false);

        btnTrack.addActionListener(e -> {
            String query = txtTrack.getText().trim();
            if (query.isEmpty()){
                result.setText("Please Enter an Enrollment ID or Constituent ID:  ");
                return;
            }

            ProgramEnrolment single = searchEnrolmentByID(query);
            if (single != null){
                Constituent c = single.getConstituent();
                result.setText(
                        "Enrollment ID: " + single.getEnrolmentId() + "\n" +
                                "Constituent: " + c.getFirstName() + " " + c.getLastName() + " (" + c.getApplicantId() + ")\n" +
                                "Program: " + single.getProgram().getProgramName() + "\n" +
                                "Status: " + single.status() + "\n" +
                                "Enrolment Date: " + single.getEnrolmentDate()
                );
                return;
            }

            List <ProgramEnrolment> byConstituent = getEnrolmentsConstituentId(query);
            if (!byConstituent.isEmpty()){
                StringBuilder sb = new StringBuilder("Enrollments for Constituent " + query + ":\n\n");
                for (ProgramEnrolment pe : byConstituent) {
                    sb.append("- ").append(pe.getEnrolmentId()).append(" | ").append(pe.getProgram().getProgramName())
                            .append(" | Status: ").append(pe.status())
                            .append(" | Date: ").append(pe.getEnrolmentDate()).append("\n");
                }
                result.setText(sb.toString());
            } else {
                result.setText("No enrollment records found for: " + query);
            }
        });

        panel0.add(top, BorderLayout.NORTH);
        panel0.add(new JScrollPane(result), BorderLayout.CENTER);
        return panel0;
    }

    private JPanel buildSearchPanel() {
        JPanel panel3 = new JPanel(new BorderLayout(10, 10));
        panel3.setBackground(CREAM);
        panel3.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel3.add(new JLabel("Search Enrollments"), BorderLayout.NORTH);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBackground(CREAM);

        JTextField txtSearchQuery = new JTextField(18);
        btnSearch = makeActionButton("Search");

        top.add(new JLabel("Search (Name, TRN, or ID: "));
        top.add(txtSearchQuery);
        top.add(btnSearch);

        String[] cols = {"Enrollment ID", "Constituent ID", "Name", "TRN", "Program", "Status"};
        DefaultTableModel searchModel = new DefaultTableModel(cols, 0){
            @Override
            public boolean isCellEditable ( int row, int column){
                return false;
                }
        };
        JTable searchTable = new JTable(searchModel);

        btnSearch.addActionListener(e -> {
        String q = txtSearchQuery.getText().trim().toLowerCase();
        searchModel.setRowCount(0);
        for (ProgramEnrolment pe : getEnrolments()) {
            Constituent c = pe.getConstituent();
            String fullName = (c.getFirstName() + " " + c.getLastName()).toLowerCase();
            if (q.isEmpty()
                    || pe.getEnrolmentId().toLowerCase().contains(q)
                    || c.getApplicantId().toLowerCase().contains(q)
                    || fullName.contains(q)
                    || c.getTRN().toLowerCase().contains(q)) {
                searchModel.addRow(new Object[]{
                        pe.getEnrolmentId(),
                        c.getApplicantId(),
                        c.getFirstName() + " " + c.getLastName(),
                        c.getTRN(),
                        pe.getProgram().getProgramName(),
                        pe.status()
                });
            }
        }
    });

    panel3.add(top, BorderLayout.NORTH);
    panel3.add(new JScrollPane(searchTable), BorderLayout.CENTER);
    return panel3;
}

    @Override
    public String getTitle(){
        return "Enrolment Management";
    }

    @Override
    public JComponent getView(){
        return rootPanel;
    }

    @Override
    public void onUserChanged(StaffMember user){

    }
}
