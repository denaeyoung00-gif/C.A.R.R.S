package carrs.app;

import java.awt.*;
import javax.swing.*;
import javax.swing.JFrame;

public class MainFrame extends JFrame {
    private final ProgramRepository repo;
    private final ProgramManagementService service;
    private final ProgrammeListPanel listPanel;

    public MainFrame(ProgramRepository repo, ProgramManagementService service) {
        this.repo=repo;
        this.service = service;

        setTitle("Programme Management");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // header
        JLabel title = new JLabel("Programmes");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        JButton addButton = new JButton("+ New programme");
        addButton.addActionListener(e -> openCreateDialog());

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        top.add(title, BorderLayout.WEST);
        top.add(addButton, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // list
        listPanel = new ProgrammeListPanel(repo, service);
        JScrollPane scroll = new JScrollPane();
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        add(scroll, BorderLayout.CENTER);

        listPanel.reload();
    }

    private void openCreateDialog() {
        CreateProgrammeDialog dlg = new CreateProgrammeDialog(this, service);
        dlg.setVisible(true);
        if (dlg.wasCreated()) {
            listPanel.reload();
        }
    }
}