package carrs.app;

import java.util.List;
import javax.swing.*;

public class ProgrammeListPanel extends JPanel {
    private final ProgramRepository repo;
    private final ProgramManagementService service;

    public ProgrammeListPanel(ProgramRepository repo, ProgramManagementService service) {
        this.repo = repo;
        this.service = service;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    public void reload() {
        removeAll();

        List<Program> all = repo.findAll();
        if (all.isEmpty()) {
            JLabel empty = new JLabel("No programmes yet. Click '+ New programme' to start.");
            empty.setAlignmentX(LEFT_ALIGNMENT);
            add(empty);
        } else {
            for (Program p : all) {
                ProgrammeCard card = new ProgrammeCard(p, service, this);
                card.setAlignmentX(LEFT_ALIGNMENT);
                add(card);
                add(Box.createVerticalStrut(8));
            }
        }

        revalidate();
        repaint();
    }
}