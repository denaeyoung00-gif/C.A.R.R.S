package carrs.app;

import carrs.model.ProgrammeDetails;
import carrs.model.StaffMember;
import carrs.repository.CarrsRepository;

import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Programes tab (Programe Management). Lists every programme from the database and lets Office Staff,
 * Office Manager and MP create, edit and delete them. Attendants can only look.
 * Programmes are saved in the "programme" table, so Register Applicant, Enrolment and Payments see them too.
 */
public class ProgrammeTab implements FeatureTab {
    private final ProgramManagementService service;
    private final JPanel view = new JPanel(new BorderLayout(0, 10));

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Programme", "Start Date", "Finish Date", "Capacity", "Enrolled", "Places Left", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextField searchField = new JTextField(20);
    private final JButton newBtn = new JButton("New Programme");
    private final JButton editBtn = new JButton("Edit");
    private final JButton deleteBtn = new JButton("Delete");

    private List<ProgrammeDetails> all = new ArrayList<>();
    private List<ProgrammeDetails> shown = new ArrayList<>();   // the rows currently in the table, in order

    public ProgrammeTab(CarrsRepository repo) {
        this.service = new ProgramManagementService(repo);
        view.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.add(new JLabel("Search:"));
        north.add(searchField);
        JButton searchBtn = new JButton("Search");
        north.add(searchBtn);
        view.add(north, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] widths = {40, 330, 100, 100, 70, 70, 80, 100};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        view.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(newBtn); south.add(editBtn); south.add(deleteBtn);
        view.add(south, BorderLayout.SOUTH);

        searchBtn.addActionListener(e -> showRows());
        searchField.addActionListener(e -> showRows());
        newBtn.addActionListener(e -> onNew());
        editBtn.addActionListener(e -> onEdit());
        deleteBtn.addActionListener(e -> onDelete());
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && editBtn.isEnabled()) onEdit();
            }
        });

        // reload from the database every time the tab is opened
        view.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && view.isShowing()) reload();
        });
        setButtons(false);
        reload();
    }

    private void setButtons(boolean can) {
        newBtn.setEnabled(can); editBtn.setEnabled(can); deleteBtn.setEnabled(can);
    }

    /** Reads the programmes from the database again. */
    private void reload() {
        try {
            all = service.findAll();
        } catch (RuntimeException ex) {
            all = new ArrayList<>();
            JOptionPane.showMessageDialog(view, "Could not load programmes: " + ex.getMessage());
        }
        showRows();
    }

    private void showRows() {
        String q = searchField.getText().trim().toLowerCase();
        model.setRowCount(0);
        shown = new ArrayList<>();
        for (ProgrammeDetails p : all) {
            if (!q.isEmpty() && !p.getName().toLowerCase().contains(q)) continue;
            shown.add(p);
            model.addRow(new Object[]{p.getId(), p.getName(),
                    p.getStartDate() == null ? "Not set" : p.getStartDate().toString(),
                    p.getFinishDate() == null ? "Not set" : p.getFinishDate().toString(),
                    p.getCapacity(), p.getEnrolled(), p.getRemaining(), p.getStatus()});
        }
    }

    private ProgrammeDetails selected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(view, "Select a programme first."); return null; }
        return shown.get(row);
    }

    private void onNew() {
        ProgrammeFormDialog dlg = new ProgrammeFormDialog(SwingUtilities.getWindowAncestor(view), service, null);
        dlg.setVisible(true);
        if (dlg.wasSaved()) reload();
    }

    private void onEdit() {
        ProgrammeDetails p = selected();
        if (p == null) return;
        ProgrammeFormDialog dlg = new ProgrammeFormDialog(SwingUtilities.getWindowAncestor(view), service, p);
        dlg.setVisible(true);
        if (dlg.wasSaved()) reload();
    }

    private void onDelete() {
        ProgrammeDetails p = selected();
        if (p == null) return;
        int ok = JOptionPane.showConfirmDialog(view, "Delete \"" + p.getName() + "\"? This cannot be undone.",
                "Delete programme", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            service.delete(p.getId());
            reload();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(view, ex.getMessage());
        }
    }

    @Override public String getTitle() { return "Programs"; }
    @Override public JComponent getView() { return view; }

    @Override public void onUserChanged(StaffMember u) {
        setButtons(u != null && u.canApproveApplications());
        reload();
    }
}
