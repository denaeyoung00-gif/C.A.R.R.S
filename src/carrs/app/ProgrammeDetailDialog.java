package carrs.app;

import java.awt.*;
import java.time.LocalDate;
import javax.swing.*;

public class ProgrammeDetailDialog extends JDialog {
    private final Program program;
    private final ProgramManagementService service;
    private boolean deleted = false;
    private boolean editRequested = false;

    public ProgrammeDetailDialog(JFrame parent, Program program,
                                 ProgramManagementService service) {
        super(parent, "Programme details", true);
        this.program = program;
        this.service = service;

        int enrolled = service.getEnrolledCount(program.getId());
        int remaining = program.getCapacity() - enrolled;

        setLayout(new GridBagLayout());
        ((JComponent) getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.WEST;

        int row = 0;
        addField(c, row++, "Name:", program.getProgramName());
        addField(c, row++, "Start date:", program.getStartDate().toString());
        addField(c, row++, "Finish date:", program.getFinishDate().toString());
        addField(c, row++, "Capacity:", String.valueOf(program.getCapacity()));
        addField(c, row++, "Status:", program.computeStatus(LocalDate.now()).toString());
        addField(c, row++, "Enrolled:", String.valueOf(enrolled));
        addField(c, row++, "Remaining:", String.valueOf(remaining));

        JButton edit = new JButton("Edit");
        JButton delete = new JButton("Delete");
        JButton close = new JButton("Close");

        edit.addActionListener(e -> {
            editRequested = true;
            dispose();
        });
        delete.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete \"" + program.getProgramName() + "\"? This cannot be undone.",
                    "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    service.deleteProgram(program.getId());
                    deleted = true;
                    dispose();
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        close.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(edit);
        buttons.add(delete);
        buttons.add(close);

        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        add(buttons, c);

        pack();
        setLocationRelativeTo(parent);
    }

    private void addField(GridBagConstraints c, int row, String label, String value) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        c.weightx = 0;
        JLabel l = new JLabel(label);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        add(l, c);
        c.gridx = 1;
        c.weightx = 1;
        add(new JLabel(value), c);
    }

    public boolean wasDeleted() { return deleted; }
    public boolean isEditRequested() { return editRequested; }
}

