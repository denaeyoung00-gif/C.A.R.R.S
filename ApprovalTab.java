package carrs.app;

import carrs.model.*;
import carrs.repository.CarrsRepository;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ApprovalTab implements FeatureTab {
    private final CarrsRepository repo;
    private final JPanel view = new JPanel(new BorderLayout());
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Reference", "Programme ID", "Applicant ID", "Reason"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JButton approveBtn = new JButton("Approve");
    private final JButton rejectBtn = new JButton("Reject");
    private StaffMember user;
    private List<Application> pending;

    public ApprovalTab(CarrsRepository repo) {
        this.repo = repo;
        view.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        view.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(approveBtn); south.add(rejectBtn);
        view.add(south, BorderLayout.SOUTH);

        approveBtn.addActionListener(e -> decide(ApplicationStatus.APPROVED));
        rejectBtn.addActionListener(e -> decide(ApplicationStatus.REJECTED));
    }

    @Override public String getTitle() { return "Approve Applications"; }
    @Override public JComponent getView() { return view; }

    @Override
    public void onUserChanged(StaffMember u) {
        this.user = u;
        boolean can = u != null && u.canApproveApplications();
        approveBtn.setEnabled(can); rejectBtn.setEnabled(can);
        model.setRowCount(0);
        if (!can) return;

        pending = repo.findApplicationsByStatus(ApplicationStatus.COMPLETED);
        for (Application a : pending) {
            model.addRow(new Object[]{a.getId(), a.getReferenceNo(), a.getProgrammeId(), a.getApplicantId(), a.getReasonForApplying()});
        }
    }

    private void decide(ApplicationStatus status) {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(view, "Select an application first."); return; }
        
        String reason = null;
        if (status == ApplicationStatus.REJECTED) {
            reason = JOptionPane.showInputDialog(view, "Why is this application being rejected?");
            if (reason == null || reason.trim().isEmpty()) return;
        }

        try {
            repo.decideApplication(pending.get(row).getId(), status, reason, user);
            JOptionPane.showMessageDialog(view, "Application " + status);
            onUserChanged(user); // Refresh table
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(view, ex.getMessage());
        }
    }
}
