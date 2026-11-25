package carrs.app;

import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.*;

public class EditProgrammeDialog extends JDialog {
    private final ProgramManagementService service;
    private final Program program;
    private boolean updated = false;

    private final JTextField nameField;
    private final JTextField startField;
    private final JTextField finishField;
    private final JTextField capacityField;

    public EditProgrammeDialog(JFrame parent, Program program,
                               ProgramManagementService service) {
        super(parent, "Edit programme", true);
        this.service = service;
        this.program = program;

        nameField = new JTextField(program.getProgramName(), 20);
        startField = new JTextField(program.getStartDate().toString(), 20);
        finishField = new JTextField(program.getFinishDate().toString(), 20);
        capacityField = new JTextField(String.valueOf(program.getCapacity()), 20);

        setLayout(new GridBagLayout());
        ((JComponent) getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;

        addRow(c, 0, "Name:", nameField);
        addRow(c, 1, "Start date (YYYY-MM-DD):", startField);
        addRow(c, 2, "Finish date (YYYY-MM-DD):", finishField);
        addRow(c, 3, "Capacity:", capacityField);

        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");
        save.addActionListener(e -> onSave());
        cancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(save);
        c.gridx = 0; c.gridy = 4; c.gridwidth = 2;
        add(buttons, c);

        pack();
        setLocationRelativeTo(parent);
    }

    private void addRow(GridBagConstraints c, int row, String label, JTextField field) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        add(new JLabel(label), c);
        c.gridx = 1;
        add(field, c);
    }

    private void onSave() {
        try {
            LocalDate start = LocalDate.parse(startField.getText().trim());
            LocalDate finish = LocalDate.parse(finishField.getText().trim());
            int capacity = Integer.parseInt(capacityField.getText().trim());
            service.updateProgram(program.getId(), nameField.getText().trim(),
                    start, finish, capacity);
            updated = true;
            dispose();
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Dates must be YYYY-MM-DD",
                    "Error", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Capacity must be a number",
                    "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean wasUpdated() { return updated; }
}