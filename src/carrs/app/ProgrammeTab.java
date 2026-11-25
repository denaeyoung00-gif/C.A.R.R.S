package carrs.app;

import carrs.model.StaffMember;
import carrs.repository.CarrsRepository;

import java.awt.*;
import javax.swing.*;

/**
 * Programme Management tab. Shows the list of programmes as cards,
 * with a "+ New programme" button that opens the create form.
 *
 * Follows the same pattern as AppealsDisputesTab: takes the shared
 * CarrsRepository, owns its own feature repository/service internally.
 */
public class ProgrammeTab implements FeatureTab {

    private final JPanel rootPanel = new JPanel(new BorderLayout());

    // your existing feature classes (in the default package, same as AppealService etc.)
    private final ProgramRepository programmeRepo = new InMemoryProgramRepository();
    private final ProgramManagementService service = new ProgramManagementService(programmeRepo);
    private final ProgrammeListPanel listPanel = new ProgrammeListPanel(programmeRepo, service);

    public ProgrammeTab(CarrsRepository repo) {
        rootPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // header: title + "+ New programme"
        JLabel title = new JLabel("Programme Management");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        JButton addButton = new JButton("+ New programme");
        addButton.addActionListener(e -> openCreateDialog());

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        header.add(title, BorderLayout.WEST);
        header.add(addButton, BorderLayout.EAST);

        rootPanel.add(header, BorderLayout.NORTH);
        rootPanel.add(new JScrollPane(listPanel), BorderLayout.CENTER);

        listPanel.reload();
    }

    private void openCreateDialog() {
        Window owner = SwingUtilities.getWindowAncestor(rootPanel);
        JFrame parent = (owner instanceof JFrame f) ? f : null;
        CreateProgrammeDialog dlg = new CreateProgrammeDialog(parent, service);
        dlg.setVisible(true);
        if (dlg.wasCreated()) {
            listPanel.reload();
        }
    }

    @Override public String getTitle() { return "Programmes"; }
    @Override public JComponent getView() { return rootPanel; }
    @Override public void onUserChanged(StaffMember user) { /* no-op for now */ }
}