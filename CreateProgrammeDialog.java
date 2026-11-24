import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.*;

public class CreateProgrammeDialog extends JDialog {
    private final ProgramManagementService service;
    private boolean created = false;

    private final JTextField nameField = new JTextField(20);
    private final JTextField startField = new JTextField(20);
    private final JTextField finishField = new JTextField(20);
    private final JTextField capacityField = new JTextField(20);

    public CreateProgrammeDialog(JFrame parent, ProgramManagementService service) {
        super(parent, "Create programme", true);
        this.service = service;

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

        JButton create = new JButton("Create");
        JButton cancel = new JButton("Cancel");
        create.addActionListener(e -> onCreate());
        cancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(create);

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

    private void onCreate() {
        try {
            LocalDate start = LocalDate.parse(startField.getText().trim());
            LocalDate finish = LocalDate.parse(finishField.getText().trim());
            int capacity = Integer.parseInt(capacityField.getText().trim());

            service.createProgram(nameField.getText().trim(), start, finish, capacity);
            created = true;
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

    public boolean wasCreated() {
        return created;
    }
}