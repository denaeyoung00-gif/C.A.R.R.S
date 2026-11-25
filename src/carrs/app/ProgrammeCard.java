package carrs.app;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import javax.swing.*;

public class ProgrammeCard extends JPanel {
    private final Program program;
    private final ProgramManagementService service;
    private final ProgrammeListPanel parent;

    public ProgrammeCard(Program program, ProgramManagementService service,
                         ProgrammeListPanel parent) {
        this.program = program;
        this.service = service;
        this.parent = parent;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        setBackground(Color.WHITE);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel name = new JLabel(program.getProgramName());
        name.setFont(name.getFont().deriveFont(Font.BOLD, 16f));

        JLabel dates = new JLabel(program.getStartDate() + "  →  " + program.getFinishDate());
        dates.setForeground(Color.GRAY);

        JLabel status = new JLabel(program.computeStatus(LocalDate.now()).toString());
        status.setForeground(new Color(0, 120, 0));
        status.setFont(status.getFont().deriveFont(Font.BOLD));

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.add(name);
        center.add(Box.createVerticalStrut(4));
        center.add(dates);

        JButton view = new JButton("View");
        view.addActionListener(e -> openDetail());

        JPanel east = new JPanel();
        east.setLayout(new BoxLayout(east, BoxLayout.Y_AXIS));
        east.setOpaque(false);
        east.add(status);
        east.add(Box.createVerticalStrut(6));
        east.add(view);

        add(center, BorderLayout.CENTER);
        add(east, BorderLayout.EAST);

        // clicking anywhere on the card body opens the detail dialog
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                openDetail();
            }
        });
        center.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                openDetail();
            }
        });
    }

    private void openDetail() {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        ProgrammeDetailDialog dlg = new ProgrammeDetailDialog(frame, program, service);
        dlg.setVisible(true);

        if (dlg.wasDeleted()) {
            parent.reload();
            return;
        }
        if (dlg.isEditRequested()) {
            EditProgrammeDialog edit = new EditProgrammeDialog(frame, program, service);
            edit.setVisible(true);
            if (edit.wasUpdated()) {
                parent.reload();
            }
        }
    }
}
